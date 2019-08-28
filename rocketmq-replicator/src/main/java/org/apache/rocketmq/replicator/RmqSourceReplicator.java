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
package org.apache.rocketmq.replicator;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.Task;
import io.openmessaging.connector.api.source.SourceConnector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.route.QueueData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.replicator.common.ConstDefine;
import org.apache.rocketmq.replicator.common.Utils;
import org.apache.rocketmq.replicator.config.ConfigDefine;
import org.apache.rocketmq.replicator.config.DataType;
import org.apache.rocketmq.replicator.config.TaskDivideConfig;
import org.apache.rocketmq.replicator.strategy.DivideStrategyEnum;
import org.apache.rocketmq.replicator.strategy.DivideTaskByQueue;
import org.apache.rocketmq.replicator.strategy.DivideTaskByTopic;
import org.apache.rocketmq.replicator.strategy.TaskDivideStrategy;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RmqSourceReplicator extends SourceConnector {

    private static final Logger log = LoggerFactory.getLogger(RmqSourceReplicator.class);

    private boolean syncDLQ = false;

    private boolean syncRETRY = false;

    private KeyValue replicatorConfig;

    private Map<String, List<MessageQueue>> topicRouteMap;

    private TaskDivideStrategy taskDivideStrategy;

    private Set<String> whiteList;

    private volatile boolean configValid = false;

    private int taskParallelism = 1;

    private DefaultMQAdminExt defaultMQAdminExt;

    private volatile boolean adminStarted;

    public RmqSourceReplicator() {
        topicRouteMap = new HashMap<String, List<MessageQueue>>();
        whiteList = new HashSet<String>();
    }

    private synchronized void startMQAdminTools() {
        if (!configValid || adminStarted) {
            return;
        }
        RPCHook rpcHook = null;
        this.defaultMQAdminExt = new DefaultMQAdminExt(rpcHook);
        this.defaultMQAdminExt.setNamesrvAddr(this.replicatorConfig.getString(ConfigDefine.CONN_SOURCE_RMQ));
        this.defaultMQAdminExt.setAdminExtGroup(Utils.createGroupName(ConstDefine.REPLICATOR_ADMIN_PREFIX));
        this.defaultMQAdminExt.setInstanceName(Utils.createInstanceName(this.replicatorConfig.getString(ConfigDefine.CONN_SOURCE_RMQ)));
        try {
            defaultMQAdminExt.start();
            log.info("RocketMQ defaultMQAdminExt started");
        } catch (MQClientException e) {
            log.error("Replicator start failed for `defaultMQAdminExt` exception.", e);
        }
        adminStarted = true;
    }

    @Override
    public String verifyAndSetConfig(KeyValue config) {

        // Check the need key.
        for (String requestKey : ConfigDefine.REQUEST_CONFIG) {
            if (!config.containsKey(requestKey)) {
                return "Request config key: " + requestKey;
            }
        }

        // Check the whitelist, whitelist is required.
        String whileListStr = config.getString(ConfigDefine.CONN_WHITE_LIST);
        String[] wl = whileListStr.trim().split(",");
        if (wl.length <= 0)
            return "White list must be not empty.";
        else {
            for (String t : wl) {
                this.whiteList.add(t.trim());
            }
        }

        if (config.containsKey(ConfigDefine.CONN_TASK_DIVIDE_STRATEGY) &&
            config.getInt(ConfigDefine.CONN_TASK_DIVIDE_STRATEGY) == DivideStrategyEnum.BY_QUEUE.ordinal()) {
            this.taskDivideStrategy = new DivideTaskByQueue();
        } else {
            this.taskDivideStrategy = new DivideTaskByTopic();
        }

        if (config.containsKey(ConfigDefine.CONN_TASK_PARALLELISM)) {
            this.taskParallelism = config.getInt(ConfigDefine.CONN_TASK_PARALLELISM);
        }

        this.replicatorConfig = config;
        this.configValid = true;
        return "";
    }

    @Override
    public void start() {
        startMQAdminTools();
    }

    public void stop() {
    }

    public void pause() {

    }

    public void resume() {

    }

    public Class<? extends Task> taskClass() {

        return RmqSourceTask.class;
    }

    public List<KeyValue> taskConfigs() {
        if (!configValid) {
            return new ArrayList<KeyValue>();
        }

        startMQAdminTools();

        try {
            for (String topic : this.whiteList) {
                if ((syncRETRY && topic.startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) ||
                    (syncDLQ && topic.startsWith(MixAll.DLQ_GROUP_TOPIC_PREFIX)) ||
                    !topic.equals(ConfigDefine.CONN_STORE_TOPIC)) {

                    TopicRouteData topicRouteData = defaultMQAdminExt.examineTopicRouteInfo(topic);
                    if (!topicRouteMap.containsKey(topic)) {
                        topicRouteMap.put(topic, new ArrayList<MessageQueue>());
                    }
                    for (QueueData qd : topicRouteData.getQueueDatas()) {
                        for (int i = 0; i < qd.getReadQueueNums(); i++) {
                            MessageQueue mq = new MessageQueue(topic, qd.getBrokerName(), i);
                            topicRouteMap.get(topic).add(mq);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Fetch topic list error.", e);
        } finally {
            defaultMQAdminExt.shutdown();
        }

        TaskDivideConfig tdc = new TaskDivideConfig(
            this.replicatorConfig.getString(ConfigDefine.CONN_SOURCE_RMQ),
            this.replicatorConfig.getString(ConfigDefine.CONN_STORE_TOPIC),
            this.replicatorConfig.getString(ConfigDefine.CONN_SOURCE_RECORD_CONVERTER),
            DataType.COMMON_MESSAGE.ordinal(),
            this.taskParallelism
        );
        return this.taskDivideStrategy.divide(this.topicRouteMap, tdc);
    }
}

