package org.apache.rocketmq.console.service.impl;

import com.alibaba.rocketmq.client.consumer.DefaultMQPullConsumer;
import com.alibaba.rocketmq.client.consumer.PullResult;
import com.alibaba.rocketmq.common.MixAll;
import com.alibaba.rocketmq.common.Pair;
import com.alibaba.rocketmq.common.message.MessageDecoder;
import com.alibaba.rocketmq.common.message.MessageExt;
import com.alibaba.rocketmq.common.message.MessageQueue;
import com.alibaba.rocketmq.common.protocol.body.Connection;
import com.alibaba.rocketmq.common.protocol.body.ConsumeMessageDirectlyResult;
import com.alibaba.rocketmq.common.protocol.body.ConsumerConnection;
import com.alibaba.rocketmq.tools.admin.MQAdminExt;
import com.alibaba.rocketmq.tools.admin.api.MessageTrack;
import org.apache.rocketmq.console.model.MessageView;
import org.apache.rocketmq.console.service.MessageService;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.*;

import static com.alibaba.rocketmq.common.message.MessageDecoder.MSG_ID_LENGTH;

/**
 * Created by tangjie
 * 2016/11/25
 * styletang.me@gmail.com
 */
@Service
public class MessageServiceImpl implements MessageService {

    private Logger logger = LoggerFactory.getLogger(MessageServiceImpl.class);
    /**
     * @see com.alibaba.rocketmq.store.config.MessageStoreConfig
     * maxMsgsNumBatch = 64;
     * @see com.alibaba.rocketmq.store.index.IndexService
     * maxNum = Math.min(maxNum, this.defaultMessageStore.getMessageStoreConfig().getMaxMsgsNumBatch());
     */
    private final static int QUERY_MESSAGE_MAX_NUM = 64;//
    @Resource
    private MQAdminExt mqAdminExt;

    public Pair<MessageView, List<MessageTrack>> viewMessage(String subject, final String msgId) {
        try {

            MessageExt messageExt = mqAdminExt.viewMessage(msgId);
            List<MessageTrack> messageTrackList = messageTrackDetail(messageExt);
            return new Pair<>(MessageView.fromMessageExt(messageExt), messageTrackList);
        } catch (Exception e) {
            throw Throwables.propagate(e);
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
    public List<MessageView> queryMessageByTopic(String topic,final long begin,final long end) {
        DefaultMQPullConsumer consumer = new DefaultMQPullConsumer(MixAll.TOOLS_CONSUMER_GROUP, null);
        List<MessageView> messageViewList = Lists.newArrayList();
        try {
            String subExpression = "*";
            consumer.start();
            Set<MessageQueue> mqs = consumer.fetchSubscribeMessageQueues(topic);
            for (MessageQueue mq : mqs) {
                long minOffset = consumer.searchOffset(mq, begin);
//                int  beginOffset = consumer.getOffsetInQueueByTime(topic, i, timeStamp);
                long maxOffset = consumer.searchOffset(mq, end);
                READQ:
                for (long offset = minOffset; offset < maxOffset; ) {
                    try {
                        if(messageViewList.size()>2000){
                            break ;
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
                                List<MessageView> filteredList =  Lists.newArrayList(Iterables.filter(messageViewListByQuery, new Predicate<MessageView>() {
                                    @Override
                                    public boolean apply(MessageView messageView) {
                                        if (messageView.getStoreTimestamp() < begin || messageView.getStoreTimestamp() > end) {
                                            logger.info("begin={} end={} time not in range {} {}", begin, end, messageView.getStoreTimestamp(), new Date(messageView.getStoreTimestamp()).toString());
                                        }
                                        return messageView.getStoreTimestamp()>=begin &&messageView.getStoreTimestamp()<=end;
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
                    return (o1.getStoreTimestamp() > o2.getStoreTimestamp()) ? 1 : -1;
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
            logger.error("op=messageTrackDetailError",e);
            return Collections.emptyList();
        }
    }

    @Override
    public ConsumeMessageDirectlyResult consumeMessageDirectly(String msgId, String consumerGroup) {
        return consumeMessageDirectly(msgId, consumerGroup, null);
    }

    @Override
    public ConsumeMessageDirectlyResult consumeMessageDirectly(String msgId, String consumerGroup, String clientId) {
        if (StringUtils.isNotBlank(clientId)) {
            try {
                return mqAdminExt.consumeMessageDirectly(consumerGroup, clientId, msgId);
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
                logger.info("clientId={}",connection.getClientId());
                return mqAdminExt.consumeMessageDirectly(consumerGroup, connection.getClientId(), msgId);
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        throw new IllegalStateException("没有可用的消费组");

    }

    @Override
    public Pair<MessageView, List<MessageTrack>> viewMessageByBrokerAndOffset(String brokerHost, int port, long offset) {
        ByteBuffer byteBufferMsgId = ByteBuffer.allocate(MSG_ID_LENGTH);
        SocketAddress brokerHostAddress = new InetSocketAddress(brokerHost, port);
        //通过broker信息以及offset构造一个offsetMessageId
        String msgId = MessageDecoder.createMessageId(byteBufferMsgId, MessageExt.socketAddress2ByteBuffer(brokerHostAddress), offset);
        return viewMessage(null, msgId);
    }

}
