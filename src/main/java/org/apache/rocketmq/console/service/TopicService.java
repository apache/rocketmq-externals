package org.apache.rocketmq.console.service;

import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.common.TopicConfig;
import com.alibaba.rocketmq.common.admin.TopicStatsTable;
import com.alibaba.rocketmq.common.protocol.body.GroupList;
import com.alibaba.rocketmq.common.protocol.body.TopicList;
import com.alibaba.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.console.model.request.SendTopicMessageRequest;
import org.apache.rocketmq.console.model.request.TopicConfigInfo;

import java.util.List;

/**
 * Created by tangjie on 2016/11/18.
 */
public interface TopicService {
    TopicList fetchAllTopicList();

    TopicStatsTable stats(String topic);


    TopicRouteData route(String topic);

    GroupList queryTopicConsumerInfo(String topic);


    void createOrUpdate(TopicConfigInfo topicCreateOrUpdateRequest);

    TopicConfig examineTopicConfig(String topic, String brokerName);

    /**
     * 查询一个topic的配置
     * 每一个broker都会有不同的配置
     * @param topic
     * @return
     */
    List<TopicConfigInfo> examineTopicConfig(String topic);

    boolean deleteTopic(String topic, String clusterName);

    boolean deleteTopic(String topic);

    boolean deleteTopicInBroker(String brokerName,String topic);

    SendResult sendTopicMessageRequest(SendTopicMessageRequest sendTopicMessageRequest);

}
