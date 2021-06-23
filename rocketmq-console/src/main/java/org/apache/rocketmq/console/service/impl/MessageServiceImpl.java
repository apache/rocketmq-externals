/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.rocketmq.console.service.impl;


import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.consumer.PullStatus;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.Pair;
import org.apache.rocketmq.common.message.MessageClientIDSetter;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.body.Connection;
import org.apache.rocketmq.common.protocol.body.ConsumeMessageDirectlyResult;
import org.apache.rocketmq.common.protocol.body.ConsumerConnection;
import org.apache.rocketmq.console.config.RMQConfigure;
import org.apache.rocketmq.console.exception.ServiceException;
import org.apache.rocketmq.console.model.QueueOffsetInfo;
import org.apache.rocketmq.console.model.MessageView;
import org.apache.rocketmq.console.model.MessagePage;
import org.apache.rocketmq.console.model.MessagePageTask;
import org.apache.rocketmq.console.model.MessageQueryByPage;
import org.apache.rocketmq.console.model.request.MessageQuery;
import org.apache.rocketmq.console.service.MessageService;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.apache.rocketmq.tools.admin.api.MessageTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Set;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {

    private Logger logger = LoggerFactory.getLogger(MessageServiceImpl.class);

    private static final Cache<String, List<QueueOffsetInfo>> CACHE = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .build();

    @Autowired
    private RMQConfigure configure;
    /**
     * @see org.apache.rocketmq.store.config.MessageStoreConfig maxMsgsNumBatch = 64;
     * @see org.apache.rocketmq.store.index.IndexService maxNum = Math.min(maxNum, this.defaultMessageStore.getMessageStoreConfig().getMaxMsgsNumBatch());
     */
    private final static int QUERY_MESSAGE_MAX_NUM = 64;
    @Resource
    private MQAdminExt mqAdminExt;

    @Override
    public Pair<MessageView, List<MessageTrack>> viewMessage(String subject, final String msgId) {
        try {

            MessageExt messageExt = mqAdminExt.viewMessage(subject, msgId);
            List<MessageTrack> messageTrackList = messageTrackDetail(messageExt);
            return new Pair<>(MessageView.fromMessageExt(messageExt), messageTrackList);
        } catch (Exception e) {
            throw new ServiceException(-1, String.format("Failed to query message by Id: %s", msgId));
        }
    }

    @Override
    public List<MessageView> queryMessageByTopicAndKey(String topic, String key) {
        try {
            return Lists.transform(mqAdminExt.queryMessage(topic, key, QUERY_MESSAGE_MAX_NUM, 0, System.currentTimeMillis()).getMessageList(), new Function<MessageExt, MessageView>() {
                @Override
                public MessageView apply(MessageExt messageExt) {
                    return MessageView.fromMessageExt(messageExt);
                }
            });
        } catch (Exception err) {
            throw Throwables.propagate(err);
        }
    }

    @Override
    public List<MessageView> queryMessageByTopic(String topic, final long begin, final long end) {
        boolean isEnableAcl = !StringUtils.isEmpty(configure.getAccessKey()) && !StringUtils.isEmpty(configure.getSecretKey());
        RPCHook rpcHook = null;
        if (isEnableAcl) {
            rpcHook = new AclClientRPCHook(new SessionCredentials(configure.getAccessKey(), configure.getSecretKey()));
        }
        DefaultMQPullConsumer consumer = new DefaultMQPullConsumer(MixAll.TOOLS_CONSUMER_GROUP, rpcHook);
        List<MessageView> messageViewList = Lists.newArrayList();
        try {
            String subExpression = "*";
            consumer.start();
            Set<MessageQueue> mqs = consumer.fetchSubscribeMessageQueues(topic);
            for (MessageQueue mq : mqs) {
                long minOffset = consumer.searchOffset(mq, begin);
                long maxOffset = consumer.searchOffset(mq, end);
                READQ:
                for (long offset = minOffset; offset <= maxOffset; ) {
                    try {
                        if (messageViewList.size() > 2000) {
                            break;
                        }
                        PullResult pullResult = consumer.pull(mq, subExpression, offset, 32);
                        offset = pullResult.getNextBeginOffset();
                        switch (pullResult.getPullStatus()) {
                            case FOUND:

                                List<MessageView> messageViewListByQuery = Lists.transform(pullResult.getMsgFoundList(), new Function<MessageExt, MessageView>() {
                                    @Override
                                    public MessageView apply(MessageExt messageExt) {
                                        messageExt.setBody(null);
                                        return MessageView.fromMessageExt(messageExt);
                                    }
                                });
                                List<MessageView> filteredList = Lists.newArrayList(Iterables.filter(messageViewListByQuery, new Predicate<MessageView>() {
                                    @Override
                                    public boolean apply(MessageView messageView) {
                                        if (messageView.getStoreTimestamp() < begin || messageView.getStoreTimestamp() > end) {
                                            logger.info("begin={} end={} time not in range {} {}", begin, end, messageView.getStoreTimestamp(), new Date(messageView.getStoreTimestamp()).toString());
                                        }
                                        return messageView.getStoreTimestamp() >= begin && messageView.getStoreTimestamp() <= end;
                                    }
                                }));
                                messageViewList.addAll(filteredList);
                                break;
                            case NO_MATCHED_MSG:
                            case NO_NEW_MSG:
                            case OFFSET_ILLEGAL:
                                break READQ;
                        }
                    } catch (Exception e) {
                        break;
                    }
                }
            }
            Collections.sort(messageViewList, new Comparator<MessageView>() {
                @Override
                public int compare(MessageView o1, MessageView o2) {
                    if (o1.getStoreTimestamp() - o2.getStoreTimestamp() == 0) {
                        return 0;
                    }
                    return (o1.getStoreTimestamp() > o2.getStoreTimestamp()) ? -1 : 1;
                }
            });
            return messageViewList;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            consumer.shutdown();
        }
    }

    @Override
    public List<MessageTrack> messageTrackDetail(MessageExt msg) {
        try {
            return mqAdminExt.messageTrackDetail(msg);
        } catch (Exception e) {
            logger.error("op=messageTrackDetailError", e);
            return Collections.emptyList();
        }
    }


    @Override
    public ConsumeMessageDirectlyResult consumeMessageDirectly(String topic, String msgId, String consumerGroup,
                                                               String clientId) {
        if (StringUtils.isNotBlank(clientId)) {
            try {
                return mqAdminExt.consumeMessageDirectly(consumerGroup, clientId, topic, msgId);
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }

        try {
            ConsumerConnection consumerConnection = mqAdminExt.examineConsumerConnectionInfo(consumerGroup);
            for (Connection connection : consumerConnection.getConnectionSet()) {
                if (StringUtils.isBlank(connection.getClientId())) {
                    continue;
                }
                logger.info("clientId={}", connection.getClientId());
                return mqAdminExt.consumeMessageDirectly(consumerGroup, connection.getClientId(), topic, msgId);
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        throw new IllegalStateException("NO CONSUMER");

    }

    @Override
    public MessagePage queryMessageByPage(MessageQuery query) {
        MessageQueryByPage queryByPage = new MessageQueryByPage(
                query.getPageNum(),
                query.getPageSize(),
                query.getTopic(),
                query.getBegin(),
                query.getEnd());

        List<QueueOffsetInfo> queueOffsetInfos = CACHE.getIfPresent(query.getTaskId());

        if (queueOffsetInfos == null) {
            query.setPageNum(1);
            MessagePageTask task = this.queryFirstMessagePage(queryByPage);
            String taskId = MessageClientIDSetter.createUniqID();
            CACHE.put(taskId, task.getQueueOffsetInfos());

            return new MessagePage(task.getPage(), taskId);
        }
        Page<MessageView> messageViews = queryMessageByTaskPage(queryByPage, queueOffsetInfos);
        return new MessagePage(messageViews, query.getTaskId());

    }

    private MessagePageTask queryFirstMessagePage(MessageQueryByPage query) {
        boolean isEnableAcl = !StringUtils.isEmpty(configure.getAccessKey()) && !StringUtils.isEmpty(configure.getSecretKey());
        RPCHook rpcHook = null;
        if (isEnableAcl) {
            rpcHook = new AclClientRPCHook(new SessionCredentials(configure.getAccessKey(), configure.getSecretKey()));
        }
        DefaultMQPullConsumer consumer = new DefaultMQPullConsumer(MixAll.TOOLS_CONSUMER_GROUP, rpcHook);

        long total = 0;
        List<QueueOffsetInfo> queueOffsetInfos = new ArrayList<>();

        List<MessageView> messageViews = new ArrayList<>();

        try {
            consumer.start();
            Collection<MessageQueue> messageQueues = consumer.fetchSubscribeMessageQueues(query.getTopic());
            int idx = 0;
            for (MessageQueue messageQueue : messageQueues) {
                Long minOffset = consumer.searchOffset(messageQueue, query.getBegin());
                Long maxOffset = consumer.searchOffset(messageQueue, query.getEnd()) + 1;
                queueOffsetInfos.add(new QueueOffsetInfo(idx++, minOffset, maxOffset, minOffset, minOffset, messageQueue));
            }

            // check first offset has message
            // filter the begin time
            for (QueueOffsetInfo queueOffset : queueOffsetInfos) {
                Long start = queueOffset.getStart();
                boolean hasData = false;
                boolean hasIllegalOffset = true;
                while (hasIllegalOffset) {
                    PullResult pullResult = consumer.pull(queueOffset.getMessageQueues(), "*", start, 32);
                    if (pullResult.getPullStatus() == PullStatus.FOUND) {
                        hasData = true;
                        List<MessageExt> msgFoundList = pullResult.getMsgFoundList();
                        for (MessageExt messageExt : msgFoundList) {
                            if (messageExt.getStoreTimestamp() < query.getBegin()) {
                                start++;
                            } else {
                                hasIllegalOffset = false;
                                break;
                            }
                        }
                    } else {
                        hasIllegalOffset = false;
                    }
                }
                if (!hasData) {
                    queueOffset.setEnd(queueOffset.getStart());
                }
                queueOffset.setStart(start);
                queueOffset.setStartOffset(start);
                queueOffset.setEndOffset(start);
            }

            // filter the end time
            for (QueueOffsetInfo queueOffset : queueOffsetInfos) {
                if (queueOffset.getStart().equals(queueOffset.getEnd())) {
                    continue;
                }
                long end = queueOffset.getEnd();
                long pullOffset = end;
                int pullSize = 32;
                boolean hasIllegalOffset = true;
                while (hasIllegalOffset) {

                    if (pullOffset - pullSize > queueOffset.getStart()) {
                        pullOffset = pullOffset - pullSize;
                    } else {
                        pullOffset = queueOffset.getStartOffset();
                        pullSize = (int) (end - pullOffset);
                    }
                    PullResult pullResult = consumer.pull(queueOffset.getMessageQueues(), "*", pullOffset, pullSize);
                    if (pullResult.getPullStatus() == PullStatus.FOUND) {
                        List<MessageExt> msgFoundList = pullResult.getMsgFoundList();
                        for (int i = msgFoundList.size() - 1; i >= 0; i--) {
                            MessageExt messageExt = msgFoundList.get(i);
                            if (messageExt.getStoreTimestamp() < query.getBegin()) {
                                end--;
                            } else {
                                hasIllegalOffset = false;
                                break;
                            }
                        }
                    } else {
                        hasIllegalOffset = false;
                    }
                    if (pullOffset == queueOffset.getStartOffset()) {
                        break;
                    }
                }
                queueOffset.setEnd(end);
                total += queueOffset.getEnd() - queueOffset.getStart();
            }

            long pageSize = total > query.getPageSize() ? query.getPageSize() : total;


            // move startOffset
            int next = moveStartOffset(queueOffsetInfos, query);
            moveEndOffset(queueOffsetInfos, query, next);

            // find the first page of message
            for (QueueOffsetInfo queueOffsetInfo : queueOffsetInfos) {
                Long start = queueOffsetInfo.getStartOffset();
                Long end = queueOffsetInfo.getEndOffset();
                long size = Math.min(end - start, pageSize);
                if (size == 0) {
                    continue;
                }

                while (size > 0) {
                    PullResult pullResult = consumer.pull(queueOffsetInfo.getMessageQueues(), "*", start, 32);
                    if (pullResult.getPullStatus() == PullStatus.FOUND) {
                        List<MessageExt> poll = pullResult.getMsgFoundList();
                        if (poll.size() == 0) {
                            break;
                        }
                        List<MessageView> collect = poll.stream()
                                .map(MessageView::fromMessageExt).collect(Collectors.toList());

                        for (MessageView view : collect) {
                            if (size > 0) {
                                messageViews.add(view);
                                size--;
                            }
                        }
                    } else {
                        break;
                    }

                }
            }
            PageImpl<MessageView> page = new PageImpl<>(messageViews, query.page(), total);
            return new MessagePageTask(page, queueOffsetInfos);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            consumer.shutdown();
        }
    }

    private Page<MessageView> queryMessageByTaskPage(MessageQueryByPage query, List<QueueOffsetInfo> queueOffsetInfos) {
        boolean isEnableAcl = !StringUtils.isEmpty(configure.getAccessKey()) && !StringUtils.isEmpty(configure.getSecretKey());
        RPCHook rpcHook = null;
        if (isEnableAcl) {
            rpcHook = new AclClientRPCHook(new SessionCredentials(configure.getAccessKey(), configure.getSecretKey()));
        }
        DefaultMQPullConsumer consumer = new DefaultMQPullConsumer(MixAll.TOOLS_CONSUMER_GROUP, rpcHook);
        List<MessageView> messageViews = new ArrayList<>();

        long offset = query.getPageNum() * query.getPageSize();

        long total = 0;
        try {
            consumer.start();
            for (QueueOffsetInfo queueOffsetInfo : queueOffsetInfos) {
                long start = queueOffsetInfo.getStart();
                long end = queueOffsetInfo.getEnd();
                queueOffsetInfo.setStartOffset(start);
                queueOffsetInfo.setEndOffset(start);
                total += end - start;
            }
            if (total <= offset) {
                return Page.empty();
            }
            long pageSize = total - offset > query.getPageSize() ? query.getPageSize() : total - offset;

            int next = moveStartOffset(queueOffsetInfos, query);
            moveEndOffset(queueOffsetInfos, query, next);

            for (QueueOffsetInfo queueOffsetInfo : queueOffsetInfos) {
                Long start = queueOffsetInfo.getStartOffset();
                Long end = queueOffsetInfo.getEndOffset();
                long size = Math.min(end - start, pageSize);
                if (size == 0) {
                    continue;
                }

                while (size > 0) {
                    PullResult pullResult = consumer.pull(queueOffsetInfo.getMessageQueues(), "*", start, 32);
                    if (pullResult.getPullStatus() == PullStatus.FOUND) {
                        List<MessageExt> poll = pullResult.getMsgFoundList();
                        if (poll.size() == 0) {
                            break;
                        }
                        List<MessageView> collect = poll.stream()
                                .map(MessageView::fromMessageExt).collect(Collectors.toList());

                        for (MessageView view : collect) {
                            if (size > 0) {
                                messageViews.add(view);
                                size--;
                            }
                        }
                    } else {
                        break;
                    }

                }
            }
            return new PageImpl<>(messageViews, query.page(), total);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            consumer.shutdown();
        }
    }

    private int moveStartOffset(List<QueueOffsetInfo> queueOffsets, MessageQueryByPage query) {
        int size = queueOffsets.size();
        int next = 0;
        long offset = query.getPageNum() * query.getPageSize();
        if (offset == 0) {
            return next;
        }
        // sort by queueOffset size
        List<QueueOffsetInfo> orderQueue = queueOffsets
                .stream()
                .sorted((o1, o2) -> {
                    long size1 = o1.getEnd() - o1.getStart();
                    long size2 = o2.getEnd() - o2.getStart();
                    if (size1 < size2) {
                        return -1;
                    } else if (size1 > size2) {
                        return 1;
                    }
                    return 0;
                }).collect(Collectors.toList());

        // Take the smallest one each time
        for (int i = 0; i < size && offset >= (size - i); i++) {
            long minSize = orderQueue.get(i).getEnd() - orderQueue.get(i).getStartOffset();
            if (minSize == 0) {
                continue;
            }
            long reduce = minSize * (size - i);
            if (reduce <= offset) {
                offset -= reduce;
                for (int j = i; j < size; j++) {
                    orderQueue.get(j).incStartOffset(minSize);
                }
            } else {
                long addOffset = offset / (size - i);
                offset -= addOffset * (size - i);
                if (addOffset != 0) {
                    for (int j = i; j < size; j++) {
                        orderQueue.get(j).incStartOffset(addOffset);
                    }
                }
            }
        }
        for (QueueOffsetInfo info : orderQueue) {
            QueueOffsetInfo queueOffsetInfo = queueOffsets.get(info.getIdx());
            queueOffsetInfo.setStartOffset(info.getStartOffset());
            queueOffsetInfo.setEndOffset(info.getEndOffset());
        }

        for (QueueOffsetInfo info : queueOffsets) {
            if (offset == 0) {
                break;
            }
            next = (next + 1) % size;
            if (info.getStartOffset() < info.getEnd()) {
                info.incStartOffset();
                --offset;
            }
        }
        return next;
    }

    private void moveEndOffset(List<QueueOffsetInfo> queueOffsets, MessageQueryByPage query, int next) {
        int size = queueOffsets.size();
        for (int j = 0; j < query.getPageSize(); j++) {
            QueueOffsetInfo nextQueueOffset = queueOffsets.get(next);
            next = (next + 1) % size;
            int start = next;
            while (nextQueueOffset.getEndOffset() >= nextQueueOffset.getEnd()) {
                nextQueueOffset = queueOffsets.get(next);
                next = (next + 1) % size;
                if (start == next) {
                    return;
                }
            }
            nextQueueOffset.incEndOffset();
        }
    }

}
