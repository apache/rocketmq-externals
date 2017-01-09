package org.apache.rocketmq.console.service;

import com.alibaba.rocketmq.common.Pair;
import com.alibaba.rocketmq.common.message.MessageExt;
import com.alibaba.rocketmq.common.protocol.body.ConsumeMessageDirectlyResult;
import com.alibaba.rocketmq.tools.admin.api.MessageTrack;
import org.apache.rocketmq.console.model.MessageView;

import java.util.List;

/**
 * Created by tangjie
 * 2016/11/25
 * styletang.me@gmail.com
 */
public interface MessageService {
    /**
     *
     *
     * @param subject
     * @param msgId
     * @return
     */
    Pair<MessageView,List<MessageTrack>> viewMessage(String subject, final String msgId);// 消息查询

    List<MessageView> queryMessageByTopicAndKey(final String topic, final String key);

    /**
     * @see com.alibaba.rocketmq.tools.command.message.PrintMessageSubCommand
     * @param topic
     * @param begin
     * @param end
     * @return
     */
    List<MessageView> queryMessageByTopic(final String topic, final long begin,
                                  final long end);

    List<MessageTrack> messageTrackDetail(MessageExt msg);

    /**
     * 系统选取第一个clientId进行发送
     */
    ConsumeMessageDirectlyResult consumeMessageDirectly(String msgId, String consumerGroup);

    /**
     * 自己指定clientId进行发送
     */
    ConsumeMessageDirectlyResult consumeMessageDirectly(String msgId, String consumerGroup, String clientId);

    Pair<MessageView,List<MessageTrack>> viewMessageByBrokerAndOffset(String brokerHost, int port, long offset);
}
