package org.apache.rocketmq.console.controller;

import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.console.model.request.SendTopicMessageRequest;
import org.apache.rocketmq.console.model.request.TopicConfigInfo;
import org.apache.rocketmq.console.service.ConsumerService;
import org.apache.rocketmq.console.service.TopicService;
import org.apache.rocketmq.console.support.annotation.JsonBody;
import org.apache.rocketmq.console.util.JsonUtil;
import com.google.common.base.Preconditions;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;

/**
 * Created by tangjie on 2016/11/17.
 */
@Controller
@RequestMapping("/topic")
public class TopicController  {
    private Logger logger = LoggerFactory.getLogger(TopicController.class);

    @Resource
    private TopicService topicService;

    @Resource
    private ConsumerService consumerService;

    @RequestMapping(value = "/list.query", method = RequestMethod.GET)
    @JsonBody
    public Object list() throws MQClientException, RemotingException, InterruptedException {
        return topicService.fetchAllTopicList();
    }

    @RequestMapping(value = "/stats.query", method = RequestMethod.GET)
    @JsonBody
    public Object stats(@RequestParam String topic) {
        return topicService.stats(topic);
    }


    @RequestMapping(value = "/route.query", method = RequestMethod.GET)
    @JsonBody
    public Object route(@RequestParam String topic) {
        return topicService.route(topic);
    }


    @RequestMapping(value = "/createOrUpdate.do", method = {RequestMethod.GET, RequestMethod.POST})
    @JsonBody
    public Object topicCreateOrUpdateRequest(@RequestBody TopicConfigInfo topicCreateOrUpdateRequest) {
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(topicCreateOrUpdateRequest.getBrokerNameList()), "brokerName can not be all blank");
        logger.info("op=look topicCreateOrUpdateRequest={}", JsonUtil.obj2String(topicCreateOrUpdateRequest));
        topicService.createOrUpdate(topicCreateOrUpdateRequest);
        return true;
    }

    @RequestMapping(value = "/queryConsumerByTopic.query")
    @JsonBody
    public Object queryConsumerByTopic(@RequestParam String topic) {
        return consumerService.queryConsumeStatsListByTopicName(topic);
    }

    @RequestMapping(value = "/queryTopicConsumerInfo.query")
    @JsonBody
    public Object queryTopicConsumerInfo(@RequestParam String topic) {
        return topicService.queryTopicConsumerInfo(topic);
    }


    @RequestMapping(value = "/examineTopicConfig.query")
    @JsonBody
    public Object examineTopicConfig(@RequestParam String topic, @RequestParam(required = false) String brokerName) throws RemotingException, MQClientException, InterruptedException {
        return topicService.examineTopicConfig(topic);
    }

    @RequestMapping(value = "/sendTopicMessage.do", method = {RequestMethod.POST})
    @JsonBody
    public Object sendTopicMessage(@RequestBody SendTopicMessageRequest sendTopicMessageRequest) throws RemotingException, MQClientException, InterruptedException {
        return topicService.sendTopicMessageRequest(sendTopicMessageRequest);
    }

    /**
     * topic信息会存在于namesever和broker2个地方
     * @param clusterName
     * @param topic
     * @return
     */
    @RequestMapping(value = "/deleteTopic.do", method = {RequestMethod.POST})
    @JsonBody
    public Object delete(@RequestParam(required = false) String clusterName, @RequestParam String topic) {
//        clusterName 没传 就删除全部集群的
        return topicService.deleteTopic(topic, clusterName);
    }

    @RequestMapping(value = "/deleteTopicByBroker.do", method = {RequestMethod.POST})
    @JsonBody
    public Object deleteTopicByBroker(@RequestParam String brokerName, @RequestParam String topic) {
        return topicService.deleteTopicInBroker(brokerName, topic);
    }


}
