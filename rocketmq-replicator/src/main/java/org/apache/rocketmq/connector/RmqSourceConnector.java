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
package org.apache.rocketmq.connector;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.Task;
import io.openmessaging.connector.api.source.SourceConnector;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.constant.PermName;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.body.TopicList;
import org.apache.rocketmq.common.protocol.route.QueueData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.connector.config.ConfigDefine;
import org.apache.rocketmq.connector.strategy.DivideStrategyEnum;
import org.apache.rocketmq.connector.strategy.DivideTaskByQueue;
import org.apache.rocketmq.connector.strategy.DivideTaskByTopic;
import org.apache.rocketmq.connector.strategy.TaskDivideStrategy;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RmqSourceConnector extends SourceConnector {

    private static final Logger log = LoggerFactory.getLogger(RmqSourceConnector.class);

    private KeyValue config;

    private Map<String, List<MessageQueue>> topicRouteMap;

    private final TaskDivideStrategy taskDivideStrategy;

    public RmqSourceConnector() {

        topicRouteMap = new HashMap<String, List<MessageQueue>>();

        if (this.config.getInt(ConfigDefine.TASK_DIVIDE_STRATEGY) == DivideStrategyEnum.BY_TOPIC.ordinal()) {
            taskDivideStrategy = new DivideTaskByTopic();
        } else {
            taskDivideStrategy = new DivideTaskByQueue();
        }
    }

    public String verifyAndSetConfig(KeyValue config) {

        for(String requestKey : ConfigDefine.REQUEST_CONFIG){
            if(!config.containsKey(requestKey)){
                return "Request config key: " + requestKey;
            }
        }
        this.config = config;
        return "";
    }

    public void start() {
    }

    public void stop() {

    }

    public void pause() {

    }

    public void resume() {

    }

    public Class<? extends Task> taskClass() {
        return null;
    }

    public List<KeyValue> taskConfigs() {

        System.setProperty(MixAll.NAMESRV_ADDR_PROPERTY, this.config.getString(ConfigDefine.SOURCE_RMQ));
        RPCHook rpcHook = null;
        DefaultMQAdminExt defaultMQAdminExt = new DefaultMQAdminExt(rpcHook);
        defaultMQAdminExt.setInstanceName(Long.toString(System.currentTimeMillis()));
        try {
            defaultMQAdminExt.start();
            TopicList topicList = defaultMQAdminExt.fetchAllTopicList();
            for (String topic: topicList.getTopicList()) {
                if (!topic.equals(ConfigDefine.STORE_TOPIC)) {
                    TopicRouteData topicRouteData = defaultMQAdminExt.examineTopicRouteInfo(topic);
                    if (!topicRouteMap.containsKey(topic)) {
                        topicRouteMap.put(topic, new ArrayList<MessageQueue>());
                    }
                    for (QueueData qd: topicRouteData.getQueueDatas()) {
                        if (PermName.isReadable(qd.getPerm())) {
                            for (int i = 0; i < qd.getReadQueueNums(); i++) {
                                MessageQueue mq = new MessageQueue(topic, qd.getBrokerName(), i);
                                topicRouteMap.get(topic).add(mq);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("fetch topic list error: ", e);
        } finally {
            defaultMQAdminExt.shutdown();
        }

        return this.taskDivideStrategy.divide(this.topicRouteMap,
                this.config.getString(ConfigDefine.SOURCE_RMQ), this.config.getString(ConfigDefine.STORE_TOPIC));
    }
}

