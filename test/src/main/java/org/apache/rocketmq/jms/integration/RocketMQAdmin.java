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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.jms.integration;

import com.google.common.collect.Sets;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.apache.rocketmq.jms.integration.Constant.BROKER_ADDRESS;
import static org.apache.rocketmq.jms.integration.Constant.NAME_SERVER_ADDRESS;

@Service
public class RocketMQAdmin {

    private static final Logger log = LoggerFactory.getLogger(RocketMQAdmin.class);

    @Autowired
    // make sure RocketMQServer start ahead
    private RocketMQServer rocketMQServer;

    //MQAdmin client
    private DefaultMQAdminExt defaultMQAdminExt = new DefaultMQAdminExt();

    @PostConstruct
    public void start() {
        // reduce rebalance waiting time
        System.setProperty("rocketmq.client.rebalance.waitInterval", "1000");

        defaultMQAdminExt.setNamesrvAddr(NAME_SERVER_ADDRESS);
        try {
            defaultMQAdminExt.start();
            log.info("Start RocketMQAdmin Successfully");
        }
        catch (MQClientException e) {
            log.error("Failed to start MQAdmin", e);
            System.exit(1);
        }
    }

    @PreDestroy
    public void shutdown() {
        defaultMQAdminExt.shutdown();
    }

    public void createTopic(String topic) {
        createTopic(topic, 1);
    }

    public void createTopic(String topic, int queueNum) {
        TopicConfig topicConfig = new TopicConfig();
        topicConfig.setTopicName(topic);
        topicConfig.setReadQueueNums(queueNum);
        topicConfig.setWriteQueueNums(queueNum);
        try {
            defaultMQAdminExt.createAndUpdateTopicConfig(BROKER_ADDRESS, topicConfig);
        }
        catch (Exception e) {
            log.error("Create topic:{}, addr:{} failed:{}", topic, BROKER_ADDRESS, ExceptionUtils.getStackTrace(e));
        }
    }

    public void deleteTopic(String topic) {
        try {
            defaultMQAdminExt.deleteTopicInBroker(Sets.newHashSet(BROKER_ADDRESS), topic);
        }
        catch (Exception e) {
            log.error("Delete topic:{}, addr:{} failed:{}", topic, BROKER_ADDRESS, ExceptionUtils.getStackTrace(e));
        }
    }
}
