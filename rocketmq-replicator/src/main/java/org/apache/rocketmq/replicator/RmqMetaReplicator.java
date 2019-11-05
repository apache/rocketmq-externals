/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.body.ConsumeStatsList;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.replicator.common.ConstDefine;
import org.apache.rocketmq.replicator.common.Utils;
import org.apache.rocketmq.replicator.config.RmqConnectorConfig;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RmqMetaReplicator extends SourceConnector {
    private static final Logger log = LoggerFactory.getLogger(RmqSourceReplicator.class);

    private static final Set<String> INNER_CONSUMER_GROUPS = new HashSet<>();

    private RmqConnectorConfig replicatorConfig;

    private volatile boolean configValid = false;
    private Set<String> knownGroups;
    private DefaultMQAdminExt srcMQAdminExt;
    private volatile boolean adminStarted;
    private ScheduledExecutorService executor;

    static {
        INNER_CONSUMER_GROUPS.add("TOOLS_CONSUMER");
        INNER_CONSUMER_GROUPS.add("FILTERSRV_CONSUMER");
        INNER_CONSUMER_GROUPS.add("__MONITOR_CONSUMER");
        INNER_CONSUMER_GROUPS.add("CLIENT_INNER_PRODUCER");
        INNER_CONSUMER_GROUPS.add("SELF_TEST_P_GROUP");
        INNER_CONSUMER_GROUPS.add("SELF_TEST_C_GROUP");
        INNER_CONSUMER_GROUPS.add("SELF_TEST_TOPIC");
        INNER_CONSUMER_GROUPS.add("OFFSET_MOVED_EVENT");
        INNER_CONSUMER_GROUPS.add("CID_ONS-HTTP-PROXY");
        INNER_CONSUMER_GROUPS.add("CID_ONSAPI_PERMISSION");
        INNER_CONSUMER_GROUPS.add("CID_ONSAPI_OWNER");
        INNER_CONSUMER_GROUPS.add("CID_ONSAPI_PULL");
    }

    public RmqMetaReplicator() {
        replicatorConfig = new RmqConnectorConfig();
        knownGroups = new HashSet<>();
        executor = Executors.newSingleThreadScheduledExecutor(new BasicThreadFactory.Builder().namingPattern("RmqMetaReplicator-SourceWatcher-%d").daemon(true).build());
    }

    @Override public String verifyAndSetConfig(KeyValue config) {
        log.info("verifyAndSetConfig...");
        try {
            replicatorConfig.validate(config);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }

        this.configValid = true;
        return "";
    }

    @Override public void start() {
        log.info("starting...");
        startMQAdminTools();
        executor.scheduleAtFixedRate(() ->
        {
            try {
                refreshConsuemrGroups();
            } catch (Exception e) {
                log.error("refresh consumer groups failed.", e);
            }
        }, replicatorConfig.getRefreshInterval(), replicatorConfig.getRefreshInterval(), TimeUnit.SECONDS);
    }

    @Override public void stop() {
        log.info("stopping...");
        this.executor.shutdown();
    }

    @Override public void pause() {

    }

    @Override public void resume() {

    }

    @Override public Class<? extends Task> taskClass() {
        return MetaSourceTask.class;
    }

    @Override public List<KeyValue> taskConfigs() {
        log.debug("preparing taskConfig...");
        if (!configValid) {
            return new ArrayList<>();
        }

        startMQAdminTools();

        try {
            this.knownGroups = this.fetchConsumerGroups();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Utils.groupPartitions(new ArrayList<>(this.knownGroups), this.replicatorConfig.getTaskParallelism(), replicatorConfig);
    }

    private synchronized void startMQAdminTools() {
        if (!configValid || adminStarted) {
            return;
        }
        RPCHook rpcHook = null;
        this.srcMQAdminExt = new DefaultMQAdminExt(rpcHook);
        this.srcMQAdminExt.setNamesrvAddr(this.replicatorConfig.getSrcNamesrvs());
        this.srcMQAdminExt.setAdminExtGroup(Utils.createGroupName(ConstDefine.REPLICATOR_ADMIN_PREFIX));
        this.srcMQAdminExt.setInstanceName(Utils.createInstanceName(this.replicatorConfig.getSrcNamesrvs()));

        try {
            this.srcMQAdminExt.start();
            log.info("RocketMQ srcMQAdminExt started");
        } catch (MQClientException e) {
            log.error("Replicator start failed for `srcMQAdminExt` exception.", e);
        }
        adminStarted = true;
    }

    private void refreshConsuemrGroups() throws InterruptedException, RemotingConnectException, MQBrokerException, RemotingTimeoutException, MQClientException, RemotingSendRequestException {
        log.debug("refreshConsuemrGroups...");
        Set<String> groups = fetchConsumerGroups();
        Set<String> newGroups = new HashSet<>();
        Set<String> deadGroups = new HashSet<>();
        newGroups.addAll(groups);
        newGroups.removeAll(knownGroups);
        deadGroups.addAll(knownGroups);
        deadGroups.removeAll(groups);
        if (!newGroups.isEmpty() || !deadGroups.isEmpty()) {
            log.info("reconfig consumer groups, new Groups: {} , dead groups: {}, previous groups: {}", newGroups, deadGroups, knownGroups);
            knownGroups = groups;
            context.requestTaskReconfiguration();
        }
    }

    private Set<String> fetchConsumerGroups() throws InterruptedException, RemotingTimeoutException, MQClientException, RemotingSendRequestException, RemotingConnectException, MQBrokerException {
        return listGroups().stream().filter(this::skipInnerGroup).collect(Collectors.toSet());
    }

    private Set<String> listGroups() throws InterruptedException, RemotingTimeoutException, MQClientException, RemotingSendRequestException, RemotingConnectException, MQBrokerException {
        Set<String> groups = new HashSet<>();
        ClusterInfo clusterInfo = this.srcMQAdminExt.examineBrokerClusterInfo();
        String[] addrs = clusterInfo.retrieveAllAddrByCluster(this.replicatorConfig.getSrcCluster());
        for (String addr : addrs) {
            ConsumeStatsList stats = this.srcMQAdminExt.fetchConsumeStatsInBroker(addr, true, 3 * 1000);
            stats.getConsumeStatsList().stream().map(kv -> kv.keySet()).forEach(groups::addAll);
        }
        return groups;
    }

    private boolean skipInnerGroup(String group) {
        if (INNER_CONSUMER_GROUPS.contains(group) || group.startsWith("CID_RMQ_SYS_") || group.startsWith("PositionManage") ||
            group.startsWith("ConfigManage") || group.startsWith("OffsetManage")) {
            return false;
        }
        return true;
    }
}
