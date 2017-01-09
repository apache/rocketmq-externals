package org.apache.rocketmq.console.service;

import com.alibaba.rocketmq.common.protocol.body.ConsumerConnection;
import com.alibaba.rocketmq.common.protocol.body.ConsumerRunningInfo;
import org.apache.rocketmq.console.model.ConsumerGroupRollBackStat;
import org.apache.rocketmq.console.model.GroupConsumeInfo;
import org.apache.rocketmq.console.model.TopicConsumerInfo;
import org.apache.rocketmq.console.model.request.ConsumerConfigInfo;
import org.apache.rocketmq.console.model.request.DeleteSubGroupRequest;
import org.apache.rocketmq.console.model.request.ResetOffsetRequest;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by tangjie
 * 2016/11/22
 * styletang.me@gmail.com
 */
public interface ConsumerService {
    /**
     * 查询所有的消费组信息
     * @return
     */
    List<GroupConsumeInfo> queryGroupList();

    List<TopicConsumerInfo> queryConsumeStatsListByGroupName(String groupName);

    List<TopicConsumerInfo> queryConsumeStatsList(String topic, String groupName);

    Map<String,TopicConsumerInfo> queryConsumeStatsListByTopicName(String topic);


    Map<String /*consumerGroup*/ ,ConsumerGroupRollBackStat> resetOffset(ResetOffsetRequest resetOffsetRequest);


    List<ConsumerConfigInfo> examineSubscriptionGroupConfig(String group);

    boolean deleteSubGroup(DeleteSubGroupRequest deleteSubGroupRequest);

    boolean createAndUpdateSubscriptionGroupConfig(ConsumerConfigInfo consumerConfigInfo);

    Set<String> fetchBrokerNameSetBySubscriptionGroup(String group);

    ConsumerConnection getConsumerConnection(String consumerGroup);

    ConsumerRunningInfo getConsumerRunningInfo(String consumerGroup, String clientId, boolean jstack);
}
