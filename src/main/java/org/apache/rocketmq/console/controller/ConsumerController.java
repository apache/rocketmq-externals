package org.apache.rocketmq.console.controller;

import org.apache.rocketmq.console.model.request.ConsumerConfigInfo;
import org.apache.rocketmq.console.model.request.DeleteSubGroupRequest;
import org.apache.rocketmq.console.model.request.ResetOffsetRequest;
import org.apache.rocketmq.console.service.ConsumerService;
import org.apache.rocketmq.console.support.annotation.JsonBody;
import org.apache.rocketmq.console.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;

/**
 * Created by tangjie
 * 2016/11/22
 * styletang.me@gmail.com
 */
@Controller
@RequestMapping("/consumer")
public class ConsumerController {
    private Logger logger = LoggerFactory.getLogger(ConsumerController.class);

    @Resource
    private ConsumerService consumerService;

    @RequestMapping(value = "/groupList.query")
    @JsonBody
    public Object list() {
        return consumerService.queryGroupList();
    }

    @RequestMapping(value = "/resetOffset.do", method = {RequestMethod.POST})
    @JsonBody
    public Object resetOffset(@RequestBody ResetOffsetRequest resetOffsetRequest) {
        logger.info("op=look resetOffsetRequest={}", JsonUtil.obj2String(resetOffsetRequest));
        return consumerService.resetOffset(resetOffsetRequest);
    }

    @RequestMapping(value = "/examineSubscriptionGroupConfig.query")
    @JsonBody
    public Object examineSubscriptionGroupConfig(@RequestParam String consumerGroup) {
        return consumerService.examineSubscriptionGroupConfig(consumerGroup);
    }


    @RequestMapping(value = "/deleteSubGroup.do", method = {RequestMethod.POST})
    @JsonBody
    public Object deleteSubGroup(@RequestBody DeleteSubGroupRequest deleteSubGroupRequest) {
        return consumerService.deleteSubGroup(deleteSubGroupRequest);
    }

    @RequestMapping(value = "/createOrUpdate.do", method = {RequestMethod.POST})
    @JsonBody
    public Object consumerCreateOrUpdateRequest(@RequestBody ConsumerConfigInfo consumerConfigInfo) {
        logger.info("{}", JsonUtil.obj2String(consumerConfigInfo));
        return consumerService.createAndUpdateSubscriptionGroupConfig(consumerConfigInfo);
    }

    @RequestMapping(value = "/fetchBrokerNameList.query", method = {RequestMethod.GET})
    @JsonBody
    public Object fetchBrokerNameList(@RequestParam String consumerGroup) {
        return consumerService.fetchBrokerNameSetBySubscriptionGroup(consumerGroup);
    }

    @RequestMapping(value = "/queryTopicByConsumer.query")
    @JsonBody
    public Object queryConsumerByTopic(@RequestParam String consumerGroup) {
        return consumerService.queryConsumeStatsListByGroupName(consumerGroup);
    }

    @RequestMapping(value = "/consumerConnection.query")
    @JsonBody
    public Object consumerConnection(@RequestParam(required = false) String consumerGroup) {
        return consumerService.getConsumerConnection(consumerGroup);
    }

    @RequestMapping(value = "/consumerRunningInfo.query")
    @JsonBody
    public Object getConsumerRunningInfo(@RequestParam String consumerGroup, @RequestParam String clientId, @RequestParam boolean jstack) {
        return consumerService.getConsumerRunningInfo(consumerGroup, clientId, jstack);
    }
}
