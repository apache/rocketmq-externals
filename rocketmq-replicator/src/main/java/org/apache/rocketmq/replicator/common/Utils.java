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
package org.apache.rocketmq.replicator.common;

import com.alibaba.fastjson.JSONObject;
import io.openmessaging.KeyValue;
import io.openmessaging.internal.DefaultKeyValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.replicator.config.DataType;
import org.apache.rocketmq.replicator.config.RmqConnectorConfig;
import org.apache.rocketmq.replicator.config.TaskConfigEnum;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.apache.rocketmq.tools.command.CommandUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    public static String createGroupName(String prefix) {
        return new StringBuilder().append(prefix).append("-").append(System.currentTimeMillis()).append("-").append(ThreadLocalRandom.current().nextInt()).toString();
    }

    public static String createGroupName(String prefix, String postfix) {
        return new StringBuilder().append(prefix).append("-").append(postfix).toString();
    }

    public static String createTaskId(String prefix) {
        return new StringBuilder().append(prefix).append("-").append(System.currentTimeMillis()).toString();
    }

    public static String createInstanceName(String namesrvAddr) {
        String[] namesrvArray = namesrvAddr.split(";");
        List<String> namesrvList = new ArrayList<>();
        for (String ns : namesrvArray) {
            if (!namesrvList.contains(ns)) {
                namesrvList.add(ns);
            }
        }
        Collections.sort(namesrvList);
        return String.valueOf(namesrvList.toString().hashCode());
    }

    public static List<BrokerData> examineBrokerData(DefaultMQAdminExt defaultMQAdminExt, String topic,
        String cluster) throws RemotingException, MQClientException, InterruptedException {
        List<BrokerData> brokerList = new ArrayList<>();

        TopicRouteData topicRouteData = defaultMQAdminExt.examineTopicRouteInfo(topic);
        if (topicRouteData.getBrokerDatas() != null) {
            for (BrokerData broker : topicRouteData.getBrokerDatas()) {
                if (StringUtils.equals(broker.getCluster(), cluster)) {
                    brokerList.add(broker);
                }
            }
        }
        return brokerList;
    }

    public static void createTopic(DefaultMQAdminExt defaultMQAdminExt, TopicConfig topicConfig, String clusterName) {
        try {
            Set<String> masterSet =
                CommandUtil.fetchMasterAddrByClusterName(defaultMQAdminExt, clusterName);
            for (String addr : masterSet) {
                defaultMQAdminExt.createAndUpdateTopicConfig(addr, topicConfig);
                log.info("create topic to {} success.%n", addr);
            }

            if (topicConfig.isOrder()) {
                Set<String> brokerNameSet =
                    CommandUtil.fetchBrokerNameByClusterName(defaultMQAdminExt, clusterName);
                StringBuilder orderConf = new StringBuilder();
                String splitor = "";
                for (String s : brokerNameSet) {
                    orderConf.append(splitor).append(s).append(":")
                        .append(topicConfig.getWriteQueueNums());
                    splitor = ";";
                }
                defaultMQAdminExt.createOrUpdateOrderConf(topicConfig.getTopicName(),
                    orderConf.toString(), true);
                log.info("set cluster orderConf=[{}]", orderConf);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("create topic: " + topicConfig + "failed", e);
        }
    }

    public static List<KeyValue> groupPartitions(List<String> elements, int numGroups, RmqConnectorConfig tdc) {
        if (numGroups <= 0)
            throw new IllegalArgumentException("Number of groups must be positive.");

        List<KeyValue> result = new ArrayList<>(numGroups);

        // Each group has either n+1 or n raw partitions
        int perGroup = elements.size() / numGroups;
        int leftover = elements.size() - (numGroups * perGroup);

        int assigned = 0;
        for (int group = 0; group < numGroups; group++) {
            int numThisGroup = group < leftover ? perGroup + 1 : perGroup;
            KeyValue keyValue = new DefaultKeyValue();
            List<String> groupList = new ArrayList<>();
            for (int i = 0; i < numThisGroup; i++) {
                groupList.add(elements.get(assigned));
                assigned++;
            }
            keyValue.put(TaskConfigEnum.TASK_STORE_ROCKETMQ.getKey(), tdc.getStoreTopic());
            keyValue.put(TaskConfigEnum.TASK_SOURCE_ROCKETMQ.getKey(), tdc.getSrcNamesrvs());
            keyValue.put(TaskConfigEnum.TASK_SOURCE_CLUSTER.getKey(), tdc.getSrcCluster());
            keyValue.put(TaskConfigEnum.TASK_OFFSET_SYNC_TOPIC.getKey(), tdc.getOffsetSyncTopic());
            keyValue.put(TaskConfigEnum.TASK_DATA_TYPE.getKey(), DataType.OFFSET.ordinal());
            keyValue.put(TaskConfigEnum.TASK_GROUP_INFO.getKey(), JSONObject.toJSONString(groupList));
            keyValue.put(TaskConfigEnum.TASK_SOURCE_RECORD_CONVERTER.getKey(), tdc.getConverter());
            result.add(keyValue);

            log.debug("allocate group partition: {}", keyValue);
        }

        return result;
    }

    public static ImmutablePair<DefaultMQAdminExt, DefaultMQAdminExt> startMQAdminTools(
        RmqConnectorConfig replicatorConfig) throws MQClientException {
        RPCHook rpcHook = null;
        DefaultMQAdminExt srcMQAdminExt = new DefaultMQAdminExt(rpcHook);
        srcMQAdminExt.setNamesrvAddr(replicatorConfig.getSrcNamesrvs());
        srcMQAdminExt.setAdminExtGroup(Utils.createGroupName(ConstDefine.REPLICATOR_ADMIN_PREFIX));
        srcMQAdminExt.setInstanceName(Utils.createInstanceName(replicatorConfig.getSrcNamesrvs()));

        DefaultMQAdminExt targetMQAdminExt = new DefaultMQAdminExt(rpcHook);
        targetMQAdminExt.setNamesrvAddr(replicatorConfig.getTargetNamesrvs());
        targetMQAdminExt.setAdminExtGroup(Utils.createGroupName(ConstDefine.REPLICATOR_ADMIN_PREFIX));
        targetMQAdminExt.setInstanceName(Utils.createInstanceName(replicatorConfig.getTargetNamesrvs()));

        srcMQAdminExt.start();
        log.info("RocketMQ srcMQAdminExt started");

        targetMQAdminExt.start();
        log.info("RocketMQ targetMQAdminExt started");

        return ImmutablePair.of(srcMQAdminExt, targetMQAdminExt);
    }
}
