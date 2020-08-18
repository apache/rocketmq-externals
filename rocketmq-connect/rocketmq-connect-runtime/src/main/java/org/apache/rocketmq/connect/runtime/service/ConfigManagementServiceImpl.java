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

package org.apache.rocketmq.connect.runtime.service;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.Connector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.rocketmq.connect.runtime.common.ConfigWrapper;
import org.apache.rocketmq.connect.runtime.common.ConnAndTaskConfigs;
import org.apache.rocketmq.connect.runtime.common.ConnectKeyValue;
import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.apache.rocketmq.connect.runtime.config.ConnectConfig;
import org.apache.rocketmq.connect.runtime.config.WorkerRole;
import org.apache.rocketmq.connect.runtime.config.RuntimeConfigDefine;
import org.apache.rocketmq.connect.runtime.converter.ConfigConverter;
import org.apache.rocketmq.connect.runtime.converter.JsonConverter;
import org.apache.rocketmq.connect.runtime.converter.ListConverter;
import org.apache.rocketmq.connect.runtime.rpc.ConfigServer;
import org.apache.rocketmq.connect.runtime.store.FileBaseKeyValueStore;
import org.apache.rocketmq.connect.runtime.store.KeyValueStore;
import org.apache.rocketmq.connect.runtime.utils.ConnectUtil;
import org.apache.rocketmq.connect.runtime.utils.FilePathConfigUtil;
import org.apache.rocketmq.connect.runtime.utils.Plugin;
import org.apache.rocketmq.connect.runtime.utils.UIDUtil;
import org.apache.rocketmq.connect.runtime.utils.datasync.BrokerBasedLog;
import org.apache.rocketmq.connect.runtime.utils.datasync.DataSynchronizer;
import org.apache.rocketmq.connect.runtime.utils.datasync.DataSynchronizerCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigManagementServiceImpl implements ConfigManagementService {
    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_RUNTIME);

    /**
     * Configs of current worker.
     */
    private final ConnectConfig connectConfig;

    /**
     * Current connector configs in the store.
     */
    private KeyValueStore<String, ConnectKeyValue> connectorKeyValueStore;

    /**
     * Current task configs in the store.
     */
    private KeyValueStore<String, List<ConnectKeyValue>> taskKeyValueStore;

    /**
     * All listeners to trigger while config change.
     */
    private Set<ConnectorConfigUpdateListener> connectorConfigUpdateListener;

    /**
     * Synchronize config with other workers.
     */
    private DataSynchronizer<String, ConfigWrapper> dataSynchronizer;

    private final ClusterManagementService clusterManagementService;

    private final ConfigServer configServer;

    private final Plugin plugin;

    private final String configManagePrefix = "ConfigManage";


    public ConfigManagementServiceImpl(ConnectConfig connectConfig, ClusterManagementService clusterManagementService, Plugin plugin) {
        this.connectConfig = connectConfig;
        this.clusterManagementService = clusterManagementService;
        this.clusterManagementService.registerListener(new LeaderChangeListenerImpl());
        this.connectorConfigUpdateListener = new HashSet<>();
        this.dataSynchronizer = new BrokerBasedLog<>(connectConfig,
            connectConfig.getConfigStoreTopic(),
            ConnectUtil.createGroupName(configManagePrefix),
            new ConfigChangeCallback(),
            new JsonConverter(),
            new ConfigConverter());
        this.connectorKeyValueStore = new FileBaseKeyValueStore<>(
            FilePathConfigUtil.getConnectorConfigPath(connectConfig.getStorePathRootDir()),
            new JsonConverter(),
            new JsonConverter(ConnectKeyValue.class));
        this.taskKeyValueStore = new FileBaseKeyValueStore<>(
            FilePathConfigUtil.getTaskConfigPath(connectConfig.getStorePathRootDir()),
            new JsonConverter(),
            new ListConverter(ConnectKeyValue.class));
        this.plugin = plugin;
        this.configServer = new ConfigServer(this);
    }

    @Override
    public void start() {
        connectorKeyValueStore.load();
        taskKeyValueStore.load();
        dataSynchronizer.start();
        if (connectConfig.getWorkerRole() == WorkerRole.LEADER) {
            log.info("This worker is a leader, leaderID is " + connectConfig.getWorkerID());
            connectConfig.setLeaderID(connectConfig.getWorkerID());
            startRPCServer();
            sendOnlineConfig();
        }
    }

    @Override
    public void stop() {

        connectorKeyValueStore.persist();
        taskKeyValueStore.persist();
    }


    /**
     * verify the leader in topic
     *
     * @return boolean
     */
    public boolean checkLeaderState(String leader) {
        if (leader.equals("")) {
            log.error("Receive an CONFIG_CHANGE_KEY without a leader");
            return false;
        }
        if (connectConfig.getWorkerRole() == WorkerRole.LEADER) {
            return true;
        }
        else if (connectConfig.getWorkerRole() != WorkerRole.LEADER && clusterManagementService.getAllAliveWorkers().contains(leader)) {
            log.info("This worker is a slave, and leader is {}", connectConfig.getLeaderID());
            this.connectConfig.setLeaderID(leader);
            return true;
        }
        log.error("The leader is not in the current cluster");
        return false;
    }


    public void startRPCServer() {
        try {
            this.configServer.start();
            this.configServer.blockUntilShutdown();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public Map<String, ConnectKeyValue> getConnectorConfigs() {

        Map<String, ConnectKeyValue> result = new HashMap<>();
        Map<String, ConnectKeyValue> connectorConfigs = connectorKeyValueStore.getKVMap();
        for (String connectorName : connectorConfigs.keySet()) {
            ConnectKeyValue config = connectorConfigs.get(connectorName);
            if (0 != config.getInt(RuntimeConfigDefine.CONFIG_DELETED)) {
                continue;
            }
            result.put(connectorName, config);
        }
        return result;
    }

    @Override
    public Map<String, ConnectKeyValue> getConnectorConfigsIncludeDeleted() {

        Map<String, ConnectKeyValue> result = new HashMap<>();
        Map<String, ConnectKeyValue> connectorConfigs = connectorKeyValueStore.getKVMap();
        for (String connectorName : connectorConfigs.keySet()) {
            ConnectKeyValue config = connectorConfigs.get(connectorName);
            result.put(connectorName, config);
        }
        return result;
    }

    @Override
    public String putConnectorConfig(String connectorName, ConnectKeyValue configs) throws Exception {

        ConnectKeyValue exist = connectorKeyValueStore.get(connectorName);
        if (null != exist) {
            Long updateTimestamp = exist.getLong(RuntimeConfigDefine.UPDATE_TIMESATMP);
            if (null != updateTimestamp) {
                configs.put(RuntimeConfigDefine.UPDATE_TIMESATMP, updateTimestamp);
            }
        }
        if (configs.equals(exist)) {
            return "Connector with same config already exist.";
        }

        Long currentTimestamp = System.currentTimeMillis();
        configs.put(RuntimeConfigDefine.UPDATE_TIMESATMP, currentTimestamp);

        for (String requireConfig : RuntimeConfigDefine.REQUEST_CONFIG) {
            if (!configs.containsKey(requireConfig)) {
                return "Request config key: " + requireConfig;
            }
        }

        String connectorClass = configs.getString(RuntimeConfigDefine.CONNECTOR_CLASS);
        ClassLoader classLoader = plugin.getPluginClassLoader(connectorClass);
        Class clazz;
        if (null != classLoader) {
            clazz = Class.forName(connectorClass, true, classLoader);
        } else {
            clazz = Class.forName(connectorClass);
        }
        Connector connector = (Connector) clazz.newInstance();
        String errorMessage = connector.verifyAndSetConfig(configs);
        if (errorMessage != null && errorMessage.length() > 0) {
            return errorMessage;
        }
        connectorKeyValueStore.put(connectorName, configs);
        recomputeTaskConfigs(connectorName, connector, currentTimestamp);
        return "";
    }

    @Override
    public void recomputeTaskConfigs(String connectorName, Connector connector, Long currentTimestamp) {
        List<KeyValue> taskConfigs = connector.taskConfigs();
        List<ConnectKeyValue> converterdConfigs = new ArrayList<>();
        int count = 0;
        for (KeyValue keyValue : taskConfigs) {
            ConnectKeyValue newKeyValue = new ConnectKeyValue();
            for (String key : keyValue.keySet()) {
                newKeyValue.put(key, keyValue.getString(key));
            }
            newKeyValue.put(RuntimeConfigDefine.TASK_CLASS, connector.taskClass().getName());
            newKeyValue.put(RuntimeConfigDefine.UPDATE_TIMESATMP, currentTimestamp);
            newKeyValue.put(RuntimeConfigDefine.TASK_ID, UIDUtil.generateUniqueTaskId(connectorName, currentTimestamp, count));
            newKeyValue.put(RuntimeConfigDefine.CONNECTOR_NAME, connectorName);
            converterdConfigs.add(newKeyValue);
            count++;
        }
        putTaskConfigs(connectorName, converterdConfigs);
        sendSynchronizeConfig();
        triggerListener();
    }

    @Override
    public void removeConnectorConfig(String connectorName) {

        ConnectKeyValue config = new ConnectKeyValue();

        config.put(RuntimeConfigDefine.UPDATE_TIMESATMP, System.currentTimeMillis());
        config.put(RuntimeConfigDefine.CONFIG_DELETED, 1);
        Map<String, ConnectKeyValue> connectorConfig = new HashMap<>();
        connectorConfig.put(connectorName, config);
        List<ConnectKeyValue> taskConfigList = new ArrayList<>();
        taskConfigList.add(config);

        connectorKeyValueStore.put(connectorName, config);
        putTaskConfigs(connectorName, taskConfigList);
        sendSynchronizeConfig();
        triggerListener();
    }

    @Override
    public Map<String, List<ConnectKeyValue>> getTaskConfigs() {
        Map<String, List<ConnectKeyValue>> result = new HashMap<>();
        Map<String, List<ConnectKeyValue>> taskConfigs = taskKeyValueStore.getKVMap();
        Map<String, ConnectKeyValue> filteredConnector = getConnectorConfigs();
        for (String connectorName : taskConfigs.keySet()) {
            if (!filteredConnector.containsKey(connectorName)) {
                continue;
            }
            result.put(connectorName, taskConfigs.get(connectorName));
        }
        return result;
    }

    private void putTaskConfigs(String connectorName, List<ConnectKeyValue> configs) {

        List<ConnectKeyValue> exist = taskKeyValueStore.get(connectorName);
        if (null != exist && exist.size() > 0) {
            taskKeyValueStore.remove(connectorName);
        }
        taskKeyValueStore.put(connectorName, configs);
    }

    @Override
    public void persist() {

        this.connectorKeyValueStore.persist();
        this.taskKeyValueStore.persist();
    }

    @Override
    public void registerListener(ConnectorConfigUpdateListener listener) {

        this.connectorConfigUpdateListener.add(listener);
    }

    private void triggerListener() {

        if (null == this.connectorConfigUpdateListener) {
            return;
        }
        for (ConnectorConfigUpdateListener listener : this.connectorConfigUpdateListener) {
            listener.onConfigUpdate();
        }
    }

    private void sendOnlineConfig() {
        ConnAndTaskConfigs connAndTaskConfigs = new ConnAndTaskConfigs();
        connAndTaskConfigs.setConnectorConfigs(connectorKeyValueStore.getKVMap());
        connAndTaskConfigs.setTaskConfigs(taskKeyValueStore.getKVMap());
        ConfigWrapper config = new ConfigWrapper();
        config.setLeader(connectConfig.getLeaderID());
        config.setConnAndTaskConfigs(connAndTaskConfigs);
        dataSynchronizer.send(ConfigChangeEnum.ONLINE_KEY.name(), config);
    }

    private void sendSynchronizeConfig() {

        ConnAndTaskConfigs connAndTaskConfigs = new ConnAndTaskConfigs();
        connAndTaskConfigs.setConnectorConfigs(connectorKeyValueStore.getKVMap());
        connAndTaskConfigs.setTaskConfigs(taskKeyValueStore.getKVMap());
        ConfigWrapper config = new ConfigWrapper();
        config.setLeader(connectConfig.getLeaderID());
        config.setConnAndTaskConfigs(connAndTaskConfigs);
        dataSynchronizer.send(ConfigChangeEnum.CONFIG_CHANG_KEY.name(), config);
    }

    private class LeaderChangeListenerImpl implements ClusterManagementService.LeaderStatusListener {

        @Override
        public void onLeaderChange() {
            connectConfig.setWorkerRole(WorkerRole.LEADER);
            connectConfig.setLeaderID(connectConfig.getWorkerID());
            startRPCServer();
            sendSynchronizeConfig();
            log.info("Finish the master-slave switch, leader ID is {}", connectConfig.getLeaderID());
        }
    }


    private class ConfigChangeCallback implements DataSynchronizerCallback<String, ConfigWrapper> {

        @Override
        public void onCompletion(Throwable error, String key, ConfigWrapper result) {

            if (!checkLeaderState(result.getLeader()))
                return;
            boolean changed = false;
            switch (ConfigChangeEnum.valueOf(key)) {
                case ONLINE_KEY:
                    log.info("Receive ON_LINE key, leader ip is {}", result.getLeader());
                    mergeConfig(result);
                    changed = true;
                    break;
                case CONFIG_CHANG_KEY:
                    changed = mergeConfig(result);
                    break;
                default:
                    break;
            }
            if (changed) {
                triggerListener();
            }
        }
    }

    /**
     * Merge new received configs with the configs in memory.
     *
     * @param configWrapper
     * @return
     */
    private boolean mergeConfig(ConfigWrapper configWrapper) {
        ConnAndTaskConfigs newConnAndTaskConfig = configWrapper.getConnAndTaskConfigs();
        boolean changed = false;
        for (String connectorName : newConnAndTaskConfig.getConnectorConfigs().keySet()) {
            ConnectKeyValue newConfig = newConnAndTaskConfig.getConnectorConfigs().get(connectorName);
            ConnectKeyValue oldConfig = getConnectorConfigsIncludeDeleted().get(connectorName);
            if (null == oldConfig) {
                changed = true;
                connectorKeyValueStore.put(connectorName, newConfig);
                taskKeyValueStore.put(connectorName, newConnAndTaskConfig.getTaskConfigs().get(connectorName));
            } else {
                long oldUpdateTime = oldConfig.getLong(RuntimeConfigDefine.UPDATE_TIMESATMP);
                long newUpdateTime = newConfig.getLong(RuntimeConfigDefine.UPDATE_TIMESATMP);
                if (newUpdateTime > oldUpdateTime) {
                    changed = true;
                    connectorKeyValueStore.put(connectorName, newConfig);
                    taskKeyValueStore.put(connectorName, newConnAndTaskConfig.getTaskConfigs().get(connectorName));
                }
            }
        }
        return changed;
    }

    private enum ConfigChangeEnum {

        /**
         * Insert or update config.
         */
        CONFIG_CHANG_KEY,

        /**
         * A worker online.
         */
        ONLINE_KEY
    }

    public Plugin getPlugin() {
        return this.plugin;
    }
}
