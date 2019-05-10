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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.Pair;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.body.Connection;
import org.apache.rocketmq.common.protocol.body.ConsumeMessageDirectlyResult;
import org.apache.rocketmq.common.protocol.body.ConsumerConnection;
import org.apache.rocketmq.console.exception.ServiceException;
import org.apache.rocketmq.console.model.MessageView;
import org.apache.rocketmq.console.service.MessageService;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.apache.rocketmq.tools.admin.api.MessageTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MessageServiceImpl implements MessageService {

    private Logger logger = LoggerFactory.getLogger(MessageServiceImpl.class);
    /**
     * @see org.apache.rocketmq.store.config.MessageStoreConfig maxMsgsNumBatch = 64;
     * @see org.apache.rocketmq.store.index.IndexService maxNum = Math.min(maxNum, this.defaultMessageStore.getMessageStoreConfig().getMaxMsgsNumBatch());
     */
    private final static int QUERY_MESSAGE_MAX_NUM = 64;
    @Resource
    private MQAdminExt mqAdminExt;

    public Pair<MessageView, List<MessageTrack>> viewMessage(String subject, final String msgId) {
        try {

            MessageExt messageExt = mqAdminExt.viewMessage(subject, msgId);
            List<MessageTrack> messageTrackList = messageTrackDetail(messageExt);
            return new Pair<>(MessageView.fromMessageExt(messageExt), messageTrackList);
        }
        catch (Exception e) {
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
        }
        catch (Exception err) {
            throw Throwables.propagate(err);
        }
    }

    @Override
    public List<MessageView> queryMessageByTopic(String topic, final long begin, final long end) {
        DefaultMQPullConsumer consumer = new DefaultMQPullConsumer(MixAll.TOOLS_CONSUMER_GROUP, null);
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
                    }
                    catch (Exception e) {
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
        }
        catch (Exception e) {
            throw Throwables.propagate(e);
        }
        finally {
            consumer.shutdown();
        }
    }

    @Override
    public List<MessageTrack> messageTrackDetail(MessageExt msg) {
        try {
            return mqAdminExt.messageTrackDetail(msg);
        }
        catch (Exception e) {
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
            }
            catch (Exception e) {
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
        }
        catch (Exception e) {
            throw Throwables.propagate(e);
        }
        throw new IllegalStateException("NO CONSUMER");

    }

}
