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

package org.apache.rocketmq.iot.common.util;

import java.util.Map;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.admin.TopicOffset;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.iot.common.configuration.MqttBridgeConfig;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocketAdminTools {
    private Logger logger = LoggerFactory.getLogger(RocketAdminTools.class);

    private MqttBridgeConfig bridgeConfig;
    private DefaultMQAdminExt mqAdminExt;

    private static volatile RocketAdminTools mqAdminTools;

    public static RocketAdminTools getInstance(MqttBridgeConfig bridgeConfig) {
        if (mqAdminTools != null) {
            return mqAdminTools;
        }

        synchronized (RocketAdminTools.class) {
            if (mqAdminTools == null) {
                mqAdminTools = new RocketAdminTools(bridgeConfig);
            }
        }
        return mqAdminTools;
    }

    public RocketAdminTools(MqttBridgeConfig bridgeConfig) {
        this.bridgeConfig = bridgeConfig;
        initMQAdminExt();
    }

    public void initMQAdminExt() {
        try {
            SessionCredentials sessionCredentials = new SessionCredentials(bridgeConfig.getRmqAccessKey(),
                bridgeConfig.getRmqSecretKey());
            AclClientRPCHook rpcHook = new AclClientRPCHook(sessionCredentials);
            this.mqAdminExt = new DefaultMQAdminExt(rpcHook);
            this.mqAdminExt.setNamesrvAddr(bridgeConfig.getRmqNamesrvAddr());
            this.mqAdminExt.setAdminExtGroup(bridgeConfig.getRmqNamesrvAddr());
            this.mqAdminExt.setInstanceName(MqttUtil.createInstanceName(bridgeConfig.getRmqNamesrvAddr()));
            this.mqAdminExt.start();
            logger.info("rocketMQ mqAdminExt started.");
        } catch (MQClientException e) {
            logger.error("start rocketMQ mqAdminExt failed.", e);
        }
    }

    public Map<MessageQueue, TopicOffset> getTopicQueueOffset(String rmqTopic) throws Exception {
        return mqAdminExt.examineTopicStats(rmqTopic).getOffsetTable();
    }

    public void shutdown() {
        mqAdminTools = null;
        this.mqAdminExt.shutdown();
    }
}
