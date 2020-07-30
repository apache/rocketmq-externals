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
import io.openmessaging.connector.api.sink.SinkTask;
import io.openmessaging.connector.api.source.SourceTask;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.connect.runtime.ConnectController;
import org.apache.rocketmq.connect.runtime.common.ConnectKeyValue;
import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.apache.rocketmq.connect.runtime.config.ConnectConfig;
import org.apache.rocketmq.connect.runtime.config.RuntimeConfigDefine;
import org.apache.rocketmq.connect.runtime.service.DefaultConnectorContext;
import org.apache.rocketmq.connect.runtime.service.PositionManagementService;
import org.apache.rocketmq.connect.runtime.service.TaskPositionCommitService;
import org.apache.rocketmq.connect.runtime.store.PositionStorageReaderImpl;
import org.apache.rocketmq.connect.runtime.utils.ConnectUtil;
import org.apache.rocketmq.connect.runtime.utils.Plugin;
import org.apache.rocketmq.connect.runtime.utils.PluginClassLoader;
import org.apache.rocketmq.connect.runtime.utils.ServiceThread;
import org.apache.rocketmq.remoting.protocol.LanguageCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A worker to schedule all connectors and tasks in a process.
 */
public class Worker {
    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_RUNTIME);

    /**
     * Current running connectors.
     */
    private Set<WorkerConnector> workingConnectors = new ConcurrentSet<>();

    /**
     * Current running tasks.
     */
    private Map<String /*uniqueTaskId*/, Long/*timestamp*/> pendingTasks = new ConcurrentHashMap<>();

    private Set<String> runningTasks = new ConcurrentSet<>();

    private Set<String> errorTasks = new ConcurrentSet<>();

    private Set<String> cleanedErrorTasks = new ConcurrentSet<>();

    private Map<String/*uniqueTaskId*/, Long/*timestamp*/> stoppingTasks = new ConcurrentHashMap<>();

    private Set<String> stoppedTasks = new ConcurrentSet<>();

    private Set<String> cleanedStoppedTasks = new ConcurrentSet<>();


    Map<String, List<ConnectKeyValue>> latestTaskConfigs = new HashMap<>();
    /**
     * Current running tasks to its Future map.
     */
    private Map<String, Future> taskIdToFutureMap = new ConcurrentHashMap<>();

    /**
     * Current running tasks to its configs
     *
     */
    private Map<String, ConnectKeyValue> taskIdToConfigMap = new ConcurrentHashMap<>();

    /**
     * Current running tasks to its task object
     *
     */
    private Map<String, Runnable> taskIdToTaskRunnableMap = new ConcurrentHashMap<>();

    /**
     * connectorName to taskId map
     *
     */
    private Map<String, Set<String>> connectorNameToTaskIdsMap = new ConcurrentHashMap<>();

    /**
     * Thread pool for connectors and tasks.
     */
    private final ExecutorService taskExecutor;

    /**
     * Position management for source tasks.
     */
    private final PositionManagementService positionManagementService;

    /**
     * Offset management for source tasks.
     */
    private final PositionManagementService offsetManagementService;

    /**
     * A scheduled task to commit source position of source tasks.
     */
    private final TaskPositionCommitService taskPositionCommitService;

    private final ConnectConfig connectConfig;

    private final Plugin plugin;

    private final DefaultMQProducer producer;

    private  static final int MAX_START_TIMEOUT_MILLS = 5000;

    private  static final long MAX_STOP_TIMEOUT_MILLS = 20000;
    // for MQProducer
    private volatile boolean producerStarted = false;

    private StateMachineService stateMachineService;

    public Worker(ConnectConfig connectConfig,
                  PositionManagementService positionManagementService, PositionManagementService offsetManagementService,
                  Plugin plugin) {
        this.connectConfig = connectConfig;
        this.taskExecutor = Executors.newCachedThreadPool();
        this.positionManagementService = positionManagementService;
        this.offsetManagementService = offsetManagementService;
        this.taskPositionCommitService = new TaskPositionCommitService(
            this,
            positionManagementService,
            offsetManagementService);
        this.plugin = plugin;

        this.producer = new DefaultMQProducer();
        this.producer.setNamesrvAddr(connectConfig.getNamesrvAddr());
        this.producer.setInstanceName(ConnectUtil.createInstance(connectConfig.getNamesrvAddr()));
        this.producer.setProducerGroup(connectConfig.getRmqProducerGroup());
        this.producer.setSendMsgTimeout(connectConfig.getOperationTimeout());
        this.producer.setMaxMessageSize(RuntimeConfigDefine.MAX_MESSAGE_SIZE);
        this.producer.setLanguage(LanguageCode.JAVA);
        this.stateMachineService = new StateMachineService();
    }

    public void start() {
        taskPositionCommitService.start();
        stateMachineService.start();
    }

    /**
     * Start a collection of connectors with the given configs. If a connector is already started with the same configs,
     * it will not start again. If a connector is already started but not contained in the new configs, it will stop.
     *
     * @param connectorConfigs
     * @param connectController
     * @throws Exception
     */
    public synchronized void startConnectors(Map<String, ConnectKeyValue> connectorConfigs,
                                             ConnectController connectController) throws Exception {
        Set<WorkerConnector> stoppedConnector = new HashSet<>();
        for (WorkerConnector workerConnector : workingConnectors) {
            String connectorName = workerConnector.getConnectorName();
            ConnectKeyValue keyValue = connectorConfigs.get(connectorName);
            if (null == keyValue || 0 != keyValue.getInt(RuntimeConfigDefine.CONFIG_DELETED)) {
                workerConnector.stop();
                log.info("Connector {} stop", workerConnector.getConnectorName());
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
            final ClassLoader currentThreadLoader = plugin.currentThreadLoader();
            Class clazz;
            boolean isolationFlag = false;
            if (loader instanceof PluginClassLoader) {
                clazz = ((PluginClassLoader) loader).loadClass(connectorClass, false);
                isolationFlag = true;
            } else {
                clazz = Class.forName(connectorClass);
            }
            final Connector connector = (Connector) clazz.getDeclaredConstructor().newInstance();
            WorkerConnector workerConnector = new WorkerConnector(connectorName, connector, connectorConfigs.get(connectorName), new DefaultConnectorContext(connectorName, connectController));
            if (isolationFlag) {
                Plugin.compareAndSwapLoaders(loader);
            }
            workerConnector.initialize();
            workerConnector.start();
            log.info("Connector {} start", workerConnector.getConnectorName());
            Plugin.compareAndSwapLoaders(currentThreadLoader);
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
    public void startTasks(Map<String, List<ConnectKeyValue>> taskConfigs) {
        synchronized (latestTaskConfigs) {
            this.latestTaskConfigs = taskConfigs;
        }
    }


    private boolean isConfigInSet(ConnectKeyValue keyValue, Set<Runnable> set) {
        for (Runnable runnable : set) {
            WorkerSourceTask workerSourceTask = null;
            WorkerSinkTask workerSinkTask = null;
            if (runnable instanceof WorkerSourceTask) {
                workerSourceTask = (WorkerSourceTask) runnable;
            } else {
                workerSinkTask = (WorkerSinkTask) runnable;
            }
            ConnectKeyValue taskConfig = null != workerSourceTask ? workerSourceTask.getTaskConfig() : workerSinkTask.getTaskConfig();
            if (keyValue.equals(taskConfig)) {
                return true;
            }
        }
        return false;
    }



    /**
     * Commit the position of all working tasks to PositionManagementService.
     */






    private void checkRmqProducerState() {
        if (!this.producerStarted) {
            try {
                this.producer.start();
                this.producerStarted = true;
            } catch (MQClientException e) {
                log.error("Start producer failed!", e);
            }
        }
    }

    /**
     * We can choose to persist in-memory task status
     * so we can view history tasks
     */
    public void stop() {
        taskExecutor.shutdownNow();
        stateMachineService.shutdown();
        // shutdown producers
        if (this.producerStarted && this.producer != null) {
            this.producer.shutdown();
            this.producerStarted = false;
        }
    }

    public Set<WorkerConnector> getWorkingConnectors() {
        return workingConnectors;
    }

    public void setWorkingConnectors(
            Set<WorkerConnector> workingConnectors) {
        this.workingConnectors = workingConnectors;
    }


    public void maintainConnectorState() {

    }


    /**
     * Now we don't need to use config to determine if a task has changed or not. If two task has
     * different taskId, then no matter if they have the same config or not, we will stop and then restart
     * them.
     */
    public void maintainTaskState()  {

        Map<String, List<ConnectKeyValue>> taskConfigs = new HashMap<>();
        synchronized (latestTaskConfigs) {
            taskConfigs.putAll(latestTaskConfigs);
        }


        /**
         * We don't compare new and old here because these operation is not time consuming. However
         * for operations that needs create resources like load a class
         */

        Map<String, Set<String>> newConnectorNameToTaskIdsMap = new HashMap<>();
        Map<String, ConnectKeyValue> newTaskIdToConfigMap = new HashMap<>();
        for (String connectorName : taskConfigs.keySet()) {
            newConnectorNameToTaskIdsMap.put(connectorName, new ConcurrentSet<>());
            for (ConnectKeyValue config : taskConfigs.get(connectorName)) {
                String uniqueTaskId = config.getString(RuntimeConfigDefine.UNIQUE_TASK_ID);
                newConnectorNameToTaskIdsMap.get(connectorName).add(uniqueTaskId);
                newTaskIdToConfigMap.put(uniqueTaskId, config);
            }
        }



        // Get New Tasks
        Set<String> newTasks = new HashSet<>();

        for (String taskId : newTaskIdToConfigMap.keySet()) {
            if (!taskIdToConfigMap.containsKey(taskId)) {
                newTasks.add(taskId);
            }
        }


        // Get tasks that should be deleted
        Set<String> toDeleteTasks = new HashSet<>();

        for (String taskId : taskIdToConfigMap.keySet()) {
            if (!newTaskIdToConfigMap.containsKey(taskId)) {
                toDeleteTasks.add(taskId);
            }
        }

        connectorNameToTaskIdsMap = newConnectorNameToTaskIdsMap;
        taskIdToConfigMap = newTaskIdToConfigMap;



        //  STEP 1: try to create new tasks
        /**
         * TODO
         * Here what if we have error when creating a task ? It is not
         * loaded to the runtime at all, but it should have a state already.
         * Perhaps we will use taskId (or taskName) as the uniqueId of a task
         * as the key (like taskName)
         *
         * Currently we cannot put the task to any state if the task failed to
         * load class.
         */
        for (String taskId : newTasks) {
            WorkerTask workerTask = null;
            ConnectKeyValue keyValue = newTaskIdToConfigMap.get(taskId);
            String connectorName = keyValue.getString(RuntimeConfigDefine.CONNECTOR_NAME);
            try {
                workerTask = loadTask(connectorName, taskId,  keyValue);
            } catch (MQClientException e) {
                log.error("Error creating task, failed to create consumer for sink task {}, exception ", workerTask.toString(), e);
            } catch (ReflectiveOperationException e) {
                log.error("Error creating task, class loading failed for sink task {}, exception ", workerTask.toString(), e);
            }

            if (null != workerTask) {
                taskIdToTaskRunnableMap.put(taskId, workerTask);
                Future future = taskExecutor.submit(workerTask);
                taskIdToFutureMap.put(taskId, future);
                this.pendingTasks.put(taskId, System.currentTimeMillis());
            } else {
                // TODO put this task and the cause to error.
            }
        }


        //  STEP 2: check all pending state
        for (String taskId : pendingTasks.keySet()) {
            WorkerTask workerTask = (WorkerTask) taskIdToTaskRunnableMap.get(taskId);
            Long startTimestamp = pendingTasks.get(taskId);
            Long currentTimeMillis = System.currentTimeMillis();
            WorkerTaskState state = workerTask.getState();

            if (WorkerTaskState.ERROR == state) {
                errorTasks.add(taskId);
                pendingTasks.remove(taskId);
            } else if (WorkerTaskState.RUNNING == state) {
                runningTasks.add(taskId);
                pendingTasks.remove(taskId);
            } else if (WorkerTaskState.NEW == state) {
                log.info("[RACE CONDITION] we checked the pending tasks before state turns to PENDING");
            } else if (WorkerTaskState.PENDING == state) {
                if (currentTimeMillis - startTimestamp > MAX_START_TIMEOUT_MILLS) {
                    workerTask.timeout();
                    pendingTasks.remove(taskId);
                    errorTasks.add(taskId);
                }
            } else {
                log.error("[BUG] Illegal State in when checking pending tasks, {} is in {} state",
                        workerTask.getConnectorName(), state.toString());
            }
        }

        //  STEP 3: check running tasks and put to error state
        for (String taskId : runningTasks) {
            WorkerTask workerTask = (WorkerTask) taskIdToTaskRunnableMap.get(taskId);
            WorkerTaskState state = workerTask.getState();


            if (WorkerTaskState.ERROR == state) {
                errorTasks.add(taskId);
                runningTasks.remove(taskId);
            } else if (WorkerTaskState.RUNNING == state) {
                if (toDeleteTasks.contains(taskId)) {
                    workerTask.stop();
                    log.info("Task stopping, connector name {}, config {}", workerTask.getConnectorName(), workerTask.getTaskConfig());
                    runningTasks.remove(taskId);
                    stoppingTasks.put(taskId, System.currentTimeMillis());
                }
            } else {
                log.error("[BUG] Illegal State in when checking running tasks, {} is in {} state",
                    workerTask.getConnectorName(), state.toString());
            }
        }

        //  STEP 4 check stopping tasks
        for (String taskId : stoppingTasks.keySet()) {
            WorkerTask workerTask = (WorkerTask) taskIdToTaskRunnableMap.get(taskId);
            Long stopTimestamp = stoppingTasks.get(taskId);
            Long currentTimeMillis = System.currentTimeMillis();

            Future future = taskIdToFutureMap.get(taskId);
            WorkerTaskState state = workerTask.getState();
            // exited normally

            if (WorkerTaskState.STOPPED == state) {
                // concurrent modification Exception ? Will it pop that in the

                if (null == future || !future.isDone()) {
                    log.error("[BUG] future is null or Stopped task should have its Future.isDone() true, but false");
                }
                stoppingTasks.remove(taskId);
                stoppedTasks.add(taskId);
            } else if (WorkerTaskState.ERROR == state) {
                stoppingTasks.remove(taskId);
                errorTasks.add(taskId);
            } else if (WorkerTaskState.STOPPING == state) {
                if (currentTimeMillis - stopTimestamp > MAX_STOP_TIMEOUT_MILLS) {
                    workerTask.timeout();
                    stoppingTasks.remove(taskId);
                    errorTasks.add(taskId);
                }
            } else {

                log.error("[BUG] Illegal State in when checking stopping tasks, {} is in {} state",
                    workerTask.getConnectorName(), state.toString());
            }
        }

        //  STEP 5 check errorTasks and stopped tasks
        for (String taskId: errorTasks) {
            WorkerTask workerTask = (WorkerTask) taskIdToTaskRunnableMap.get(taskId);
            Future future = taskIdToFutureMap.get(taskId);

            try {
                if (null != future) {
                    future.get(1000, TimeUnit.MILLISECONDS);
                } else {
                    log.error("[BUG] errorTasks reference not found in taskFutureMap");
                }
            } catch (ExecutionException e) {
                Throwable t = e.getCause();
            } catch (CancellationException | TimeoutException |  InterruptedException e) {

            } finally {
                future.cancel(true);
                workerTask.cleanup();
                taskIdToFutureMap.remove(taskId);
                errorTasks.remove(taskId);
                cleanedErrorTasks.add(taskId);

            }
        }


        //  STEP 5 check errorTasks and stopped tasks
        for (String taskId: stoppedTasks) {
            WorkerTask workerTask = (WorkerTask) taskIdToTaskRunnableMap.get(taskId);
            workerTask.cleanup();
            Future future = taskIdToFutureMap.get(taskId);
            try {
                if (null != future) {
                    future.get(1000, TimeUnit.MILLISECONDS);
                } else {
                    log.error("[BUG] stopped Tasks reference not found in taskFutureMap");
                }
            } catch (ExecutionException e) {
                Throwable t = e.getCause();
                log.info("[BUG] Stopped Tasks should not throw any exception");
                t.printStackTrace();
            } catch (CancellationException e) {
                log.info("[BUG] Stopped Tasks throws PrintStackTrace");
                e.printStackTrace();
            } catch (TimeoutException e) {
                log.info("[BUG] Stopped Tasks should not throw any exception");
                e.printStackTrace();
            } catch (InterruptedException e) {
                log.info("[BUG] Stopped Tasks should not throw any exception");
                e.printStackTrace();
            }
            finally {
                future.cancel(true);
                taskIdToFutureMap.remove(taskId);
                stoppedTasks.remove(taskId);
                cleanedStoppedTasks.add(taskId);
            }
        }
    }


    public class StateMachineService extends ServiceThread {
        @Override
        public void run() {
            log.info(this.getServiceName() + " service started");

            try {
                while (!this.isStopped()) {
                    this.waitForRunning(1000);
                    Worker.this.maintainConnectorState();
                    Worker.this.maintainTaskState();
                }
            } catch (RuntimeException e) {
                log.error("StateMachineService got runtime exception", e);
            }

            log.info(this.getServiceName() + " service end");
        }

        @Override
        public String getServiceName() {
            return StateMachineService.class.getSimpleName();
        }
    }

    private WorkerTask loadTask(String connectorName, String taskId, ConnectKeyValue keyValue) throws ReflectiveOperationException, MQClientException {
        String taskClass = keyValue.getString(RuntimeConfigDefine.TASK_CLASS);
        ClassLoader loader = plugin.getPluginClassLoader(taskClass);
        final ClassLoader currentThreadLoader = plugin.currentThreadLoader();
        Class taskClazz;
        boolean isolationFlag = false;
        if (loader instanceof PluginClassLoader) {
            taskClazz = ((PluginClassLoader) loader).loadClass(taskClass, false);
            isolationFlag = true;
        } else {
            taskClazz = Class.forName(taskClass);
        }
        final Task task = (Task) taskClazz.getDeclaredConstructor().newInstance();
        final String converterClazzName = keyValue.getString(RuntimeConfigDefine.SOURCE_RECORD_CONVERTER);
        Converter recordConverter = null;
        if (StringUtils.isNotEmpty(converterClazzName)) {
            Class converterClazz = Class.forName(converterClazzName);
            recordConverter = (Converter) converterClazz.newInstance();
        }
        if (isolationFlag) {
            Plugin.compareAndSwapLoaders(loader);
        }
        if (task instanceof SourceTask) {
            checkRmqProducerState();
            WorkerSourceTask workerSourceTask = new WorkerSourceTask(connectorName, taskId,
                    (SourceTask) task, keyValue,
                    new PositionStorageReaderImpl(positionManagementService), recordConverter, producer);
            Plugin.compareAndSwapLoaders(currentThreadLoader);

            return workerSourceTask;
        } else {
            DefaultMQPullConsumer consumer = new DefaultMQPullConsumer();
            consumer.setNamesrvAddr(connectConfig.getNamesrvAddr());
            consumer.setInstanceName(ConnectUtil.createInstance(connectConfig.getNamesrvAddr()));
            consumer.setConsumerGroup(ConnectUtil.createGroupName(connectConfig.getRmqConsumerGroup()));
            consumer.setMaxReconsumeTimes(connectConfig.getRmqMaxRedeliveryTimes());
            consumer.setBrokerSuspendMaxTimeMillis(connectConfig.getBrokerSuspendMaxTimeMillis());
            consumer.setConsumerPullTimeoutMillis((long) connectConfig.getRmqMessageConsumeTimeout());
            consumer.start();

            WorkerSinkTask workerSinkTask = new WorkerSinkTask(connectorName, taskId,
                    (SinkTask) task, keyValue,
                    new PositionStorageReaderImpl(offsetManagementService),
                    recordConverter, consumer);
            Plugin.compareAndSwapLoaders(currentThreadLoader);
            return workerSinkTask;
        }
    }




    /**
     * We cannot expose the working tasks here, because we want to contain
     * lifecycle of tasks within this class.
     *
     * @return
     */
    public Set<String> getWorkingTasks() { return runningTasks; }

    public Set<String> getErrorTasks() { return errorTasks; }

    public Set<String> getPendingTasks() {
        return pendingTasks.keySet();
    }

    public Set<String> getStoppedTasks() {
        return stoppedTasks;
    }

    public Set<String> getStoppingTasks() {
        return stoppingTasks.keySet();
    }

    public Set<String> getCleanedErrorTasks() {
        return cleanedErrorTasks;
    }

    public Set<String> getCleanedStoppedTasks() {
        return cleanedStoppedTasks;
    }

    public boolean isSourceTask(String taskId) {
        WorkerTask workerTask = (WorkerTask) taskIdToTaskRunnableMap.get(taskId);
        return workerTask instanceof WorkerSourceTask;
    }

    public Map<ByteBuffer, ByteBuffer> getPositionOrOffsetData(String taskId) {
        WorkerTask workerTask = (WorkerTask) taskIdToTaskRunnableMap.get(taskId);
        if (workerTask instanceof  WorkerSourceTask) {
            return ((WorkerSourceTask) workerTask).getPositionData();
        } else {
            return ((WorkerSinkTask) workerTask).getOffsetData();
        }
    }

    public Object getTaskJsonObject(String taskId) {
        WorkerTask workerTask = (WorkerTask) taskIdToTaskRunnableMap.get(taskId);
        return workerTask.getJsonObject();
    }

}
