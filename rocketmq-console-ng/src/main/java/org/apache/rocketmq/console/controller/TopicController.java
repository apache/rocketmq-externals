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
package org.apache.rocketmq.console.controller;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.console.model.request.SendTopicMessageRequest;
import org.apache.rocketmq.console.model.request.TopicConfigInfo;
import org.apache.rocketmq.console.service.ConsumerService;
import org.apache.rocketmq.console.service.TopicService;
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
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/topic")
public class TopicController {
    private Logger logger = LoggerFactory.getLogger(TopicController.class);

    @Resource
    private TopicService topicService;

    @Resource
    private ConsumerService consumerService;

    @RequestMapping(value = "/list.query", method = RequestMethod.GET)
    @ResponseBody
    public Object list() throws MQClientException, RemotingException, InterruptedException {
        return topicService.fetchAllTopicList();
    }

    @RequestMapping(value = "/stats.query", method = RequestMethod.GET)
    @ResponseBody
    public Object stats(@RequestParam String topic) {
        return topicService.stats(topic);
    }

    @RequestMapping(value = "/route.query", method = RequestMethod.GET)
    @ResponseBody
    public Object route(@RequestParam String topic) {
        return topicService.route(topic);
    }


    @RequestMapping(value = "/createOrUpdate.do", method = { RequestMethod.POST})
    @ResponseBody
    public Object topicCreateOrUpdateRequest(@RequestBody TopicConfigInfo topicCreateOrUpdateRequest) {
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(topicCreateOrUpdateRequest.getBrokerNameList()) || CollectionUtils.isNotEmpty(topicCreateOrUpdateRequest.getClusterNameList()),
            "clusterName or brokerName can not be all blank");
        logger.info("op=look topicCreateOrUpdateRequest={}", JsonUtil.obj2String(topicCreateOrUpdateRequest));
        topicService.createOrUpdate(topicCreateOrUpdateRequest);
        return true;
    }

    @RequestMapping(value = "/queryConsumerByTopic.query")
    @ResponseBody
    public Object queryConsumerByTopic(@RequestParam String topic) {
        return consumerService.queryConsumeStatsListByTopicName(topic);
    }

    @RequestMapping(value = "/queryTopicConsumerInfo.query")
    @ResponseBody
    public Object queryTopicConsumerInfo(@RequestParam String topic) {
        return topicService.queryTopicConsumerInfo(topic);
    }

    @RequestMapping(value = "/examineTopicConfig.query")
    @ResponseBody
    public Object examineTopicConfig(@RequestParam String topic,
        @RequestParam(required = false) String brokerName) throws RemotingException, MQClientException, InterruptedException {
        return topicService.examineTopicConfig(topic);
    }

    @RequestMapping(value = "/sendTopicMessage.do", method = {RequestMethod.POST})
    @ResponseBody
    public Object sendTopicMessage(
        @RequestBody SendTopicMessageRequest sendTopicMessageRequest) throws RemotingException, MQClientException, InterruptedException {
        return topicService.sendTopicMessageRequest(sendTopicMessageRequest);
    }

    @RequestMapping(value = "/deleteTopic.do", method = {RequestMethod.POST})
    @ResponseBody
    public Object delete(@RequestParam(required = false) String clusterName, @RequestParam String topic) {
        return topicService.deleteTopic(topic, clusterName);
    }

    @RequestMapping(value = "/deleteTopicByBroker.do", method = {RequestMethod.POST})
    @ResponseBody
    public Object deleteTopicByBroker(@RequestParam String brokerName, @RequestParam String topic) {
        return topicService.deleteTopicInBroker(brokerName, topic);
    }

}
