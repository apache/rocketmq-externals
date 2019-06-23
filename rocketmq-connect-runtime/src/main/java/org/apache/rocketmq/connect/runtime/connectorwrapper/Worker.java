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

package org.apache.rocketmq.connect.runtime.connectorwrapper;

import io.netty.util.internal.ConcurrentSet;
import io.openmessaging.connector.api.Connector;
import io.openmessaging.connector.api.Task;
import io.openmessaging.connector.api.data.Converter;
import io.openmessaging.connector.api.exception.ConnectException;
import io.openmessaging.connector.api.sink.SinkTask;
import io.openmessaging.connector.api.source.SourceTask;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.connect.runtime.common.ConnectKeyValue;
import org.apache.rocketmq.connect.runtime.config.ConnectConfig;
import org.apache.rocketmq.connect.runtime.config.RuntimeConfigDefine;
import org.apache.rocketmq.connect.runtime.service.PositionManagementService;
import org.apache.rocketmq.connect.runtime.service.TaskPositionCommitService;
import org.apache.rocketmq.connect.runtime.store.PositionStorageReaderImpl;
import org.apache.rocketmq.connect.runtime.utils.Plugin;
import org.apache.rocketmq.remoting.protocol.LanguageCode;

/**
 * A worker to schedule all connectors and tasks in a process.
 */
public class Worker {

    /**
     * Current worker id.
     */
    private final String workerId;

    /**
     * Current running connectors.
     */
    private Set<WorkerConnector> workingConnectors = new ConcurrentSet<>();

    /**
     * Current running tasks.
     */
    private Set<Runnable> workingTasks = new ConcurrentSet<>();

    /**
     * Thread pool for connectors and tasks.
     */
    private final ExecutorService taskExecutor;

    /**
     * Position management for source tasks.
     */
    private final PositionManagementService positionManagementService;

    /**
     * A scheduled task to commit source position of source tasks.
     */
    private final TaskPositionCommitService taskPositionCommitService;

    private final ConnectConfig connectConfig;

    private final Plugin plugin;

    public Worker(ConnectConfig connectConfig,
        PositionManagementService positionManagementService, Plugin plugin) {
        this.connectConfig = connectConfig;
        this.workerId = connectConfig.getWorkerId();
        this.taskExecutor = Executors.newCachedThreadPool();
        this.positionManagementService = positionManagementService;
        this.taskPositionCommitService = new TaskPositionCommitService(this);
        this.plugin = plugin;
    }

    public void start() {
        taskPositionCommitService.start();
    }

    /**
     * Start a collection of connectors with the given configs. If a connector is already started with the same configs,
     * it will not start again. If a connector is already started but not contained in the new configs, it will stop.
     *
     * @param connectorConfigs
     * @throws Exception
     */
    public synchronized void startConnectors(Map<String, ConnectKeyValue> connectorConfigs) throws Exception {

        Set<WorkerConnector> stoppedConnector = new HashSet<>();
        for (WorkerConnector workerConnector : workingConnectors) {
            String connectorName = workerConnector.getConnectorName();
            ConnectKeyValue keyValue = connectorConfigs.get(connectorName);
            if (null == keyValue || 0 != keyValue.getInt(RuntimeConfigDefine.CONFIG_DELETED)) {
                workerConnector.stop();
                stoppedConnector.add(workerConnector);
            } else if (!keyValue.equals(workerConnector.getKeyValue())) {
                workerConnector.reconfigure(keyValue);
            }
        }
        workingConnectors.removeAll(stoppedConnector);

        if (null == connectorConfigs || 0 == connectorConfigs.size()) {
            return;
        }
        Map<String, ConnectKeyValue> newConnectors = new HashMap<>();
        for (String connectorName : connectorConfigs.keySet()) {
            boolean isNewConnector = true;
            for (WorkerConnector workerConnector : workingConnectors) {
                if (workerConnector.getConnectorName().equals(connectorName)) {
                    isNewConnector = false;
                    break;
                }
            }
            if (isNewConnector) {
                newConnectors.put(connectorName, connectorConfigs.get(connectorName));
            }
        }

        for (String connectorName : newConnectors.keySet()) {
            ConnectKeyValue keyValue = newConnectors.get(connectorName);
            String connectorClass = keyValue.getString(RuntimeConfigDefine.CONNECTOR_CLASS);
            ClassLoader loader = plugin.getPluginClassLoader(connectorClass);
            Class clazz;
            if (null != loader) {
                clazz = Class.forName(connectorClass, true, loader);
            } else {
                clazz = Class.forName(connectorClass);
            }
            Connector connector = (Connector) clazz.newInstance();
            WorkerConnector workerConnector = new WorkerConnector(connectorName, connector, connectorConfigs.get(connectorName));
            workerConnector.start();
            this.workingConnectors.add(workerConnector);
        }
    }

    /**
     * Start a collection of tasks with the given configs. If a task is already started with the same configs, it will
     * not start again. If a task is already started but not contained in the new configs, it will stop.
     *
     * @param taskConfigs
     * @throws Exception
     */
    public synchronized void startTasks(Map<String, List<ConnectKeyValue>> taskConfigs) throws Exception {

        Set<Runnable> stoppedTasks = new HashSet<>();
        for (Runnable runnable : workingTasks) {
            WorkerSourceTask workerSourceTask = null;
            WorkerSinkTask workerSinkTask = null;
            if (runnable instanceof WorkerSourceTask) {
                workerSourceTask = (WorkerSourceTask) runnable;
            } else {
                workerSinkTask = (WorkerSinkTask) runnable;
            }

            String connectorName = null != workerSourceTask ? workerSourceTask.getConnectorName() : workerSinkTask.getConnectorName();
            ConnectKeyValue taskConfig = null != workerSourceTask ? workerSourceTask.getTaskConfig() : workerSinkTask.getTaskConfig();
            List<ConnectKeyValue> keyValues = taskConfigs.get(connectorName);
            boolean needStop = true;
            if (null != keyValues && keyValues.size() > 0) {
                for (ConnectKeyValue keyValue : keyValues) {
                    if (keyValue.equals(taskConfig)) {
                        needStop = false;
                        break;
                    }
                }
            }
            if (needStop) {
                if (null != workerSourceTask) {
                    workerSourceTask.stop();
                    stoppedTasks.add(workerSourceTask);
                } else {
                    workerSinkTask.stop();
                    stoppedTasks.add(workerSinkTask);
                }

            }
        }
        workingTasks.removeAll(stoppedTasks);

        if (null == taskConfigs || 0 == taskConfigs.size()) {
            return;
        }
        Map<String, List<ConnectKeyValue>> newTasks = new HashMap<>();
        for (String connectorName : taskConfigs.keySet()) {
            for (ConnectKeyValue keyValue : taskConfigs.get(connectorName)) {
                boolean isNewTask = true;
                for (Runnable runnable : workingTasks) {
                    WorkerSourceTask workerSourceTask = null;
                    WorkerSinkTask workerSinkTask = null;
                    if (runnable instanceof WorkerSourceTask) {
                        workerSourceTask = (WorkerSourceTask) runnable;
                    } else {
                        workerSinkTask = (WorkerSinkTask) runnable;
                    }
                    ConnectKeyValue taskConfig = null != workerSourceTask ? workerSourceTask.getTaskConfig() : workerSinkTask.getTaskConfig();
                    if (keyValue.equals(taskConfig)) {
                        isNewTask = false;
                        break;
                    }
                }
                if (isNewTask) {
                    if (!newTasks.containsKey(connectorName)) {
                        newTasks.put(connectorName, new ArrayList<>());
                    }
                    newTasks.get(connectorName).add(keyValue);
                }
            }
        }

        for (String connectorName : newTasks.keySet()) {
            for (ConnectKeyValue keyValue : newTasks.get(connectorName)) {
                String taskClass = keyValue.getString(RuntimeConfigDefine.TASK_CLASS);
                ClassLoader loader = plugin.getPluginClassLoader(taskClass);
                Class taskClazz;
                if (null != loader) {
                    taskClazz = Class.forName(taskClass, true, loader);
                } else {
                    taskClazz = Class.forName(taskClass);
                }
                Task task = (Task) taskClazz.newInstance();

                Class converterClazz = Class.forName(keyValue.getString(RuntimeConfigDefine.SOURCE_RECORD_CONVERTER));
                Converter recordConverter = (Converter) converterClazz.newInstance();

                if (task instanceof SourceTask) {
                    DefaultMQProducer producer = new DefaultMQProducer();
                    producer.setNamesrvAddr(keyValue.getString(RuntimeConfigDefine.NAMESRV_ADDR));
                    String rmqProducerGroup = keyValue.getString(RuntimeConfigDefine.RMQ_PRODUCER_GROUP);
                    if (StringUtils.isEmpty(rmqProducerGroup)) {
                        rmqProducerGroup = connectConfig.getRmqProducerGroup();
                    }
                    producer.setProducerGroup(rmqProducerGroup);
                    int operationTimeout = keyValue.getInt(RuntimeConfigDefine.OPERATION_TIMEOUT);
                    if (operationTimeout <= 0) {
                        producer.setSendMsgTimeout(connectConfig.getOperationTimeout());
                    }
                    producer.setMaxMessageSize(RuntimeConfigDefine.MAX_MESSAGE_SIZE);
                    producer.setLanguage(LanguageCode.JAVA);
                    producer.start();

                    WorkerSourceTask workerSourceTask = new WorkerSourceTask(connectorName,
                        (SourceTask) task, keyValue,
                        new PositionStorageReaderImpl(positionManagementService), recordConverter, producer);
                    this.taskExecutor.submit(workerSourceTask);
                    this.workingTasks.add(workerSourceTask);
                } else if (task instanceof SinkTask) {
                    DefaultMQPullConsumer consumer = new DefaultMQPullConsumer();
                    consumer.setNamesrvAddr(connectConfig.getNamesrvAddr());
                    String consumerGroup = connectConfig.getRmqConsumerGroup();
                    if (null != consumerGroup && !consumerGroup.isEmpty()) {
                        consumer.setConsumerGroup(consumerGroup);
                        consumer.setMaxReconsumeTimes(connectConfig.getRmqMaxRedeliveryTimes());
                        consumer.setConsumerPullTimeoutMillis((long) connectConfig.getRmqMessageConsumeTimeout());
                        consumer.setLanguage(LanguageCode.JAVA);
                    } else {
                        throw new ConnectException(-1, "Consumer Group is necessary for RocketMQ, please set it.");
                    }

                    WorkerSinkTask workerSinkTask = new WorkerSinkTask(connectorName, (SinkTask) task, keyValue, recordConverter, consumer);
                    this.taskExecutor.submit(workerSinkTask);
                    this.workingTasks.add(workerSinkTask);
                }
            }
        }
    }

    /**
     * Commit the position of all working tasks to PositionManagementService.
     */
    public void commitTaskPosition() {
        Map<ByteBuffer, ByteBuffer> positionData = new HashMap<>();
        for (Runnable task : workingTasks) {
            if (task instanceof WorkerSourceTask) {
                positionData.putAll(((WorkerSourceTask) task).getPositionData());
            }
        }
        positionManagementService.putPosition(positionData);
    }

    public String getWorkerId() {
        return workerId;
    }

    public void stop() {

    }

    public Set<WorkerConnector> getWorkingConnectors() {
        return workingConnectors;
    }

    public void setWorkingConnectors(
        Set<WorkerConnector> workingConnectors) {
        this.workingConnectors = workingConnectors;
    }

    public Set<Runnable> getWorkingTasks() {
        return workingTasks;
    }

    public void setWorkingTasks(Set<Runnable> workingTasks) {
        this.workingTasks = workingTasks;
    }
}
