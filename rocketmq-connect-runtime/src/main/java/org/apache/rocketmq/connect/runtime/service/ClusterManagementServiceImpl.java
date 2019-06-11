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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.apache.rocketmq.connect.runtime.config.ConnectConfig;
import org.apache.rocketmq.connect.runtime.converter.JsonConverter;
import org.apache.rocketmq.connect.runtime.utils.datasync.BrokerBasedLog;
import org.apache.rocketmq.connect.runtime.utils.datasync.DataSynchronizer;
import org.apache.rocketmq.connect.runtime.utils.datasync.DataSynchronizerCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterManagementServiceImpl implements ClusterManagementService {

    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_RUNTIME);

    /**
     * Default topic to send/consume online or offline message.
     */
    private static final String CLUSTER_MESSAGE_TOPIC = "cluster-topic";

    /**
     * Record all alive workers in memory.
     */
    private Map<String, Long> aliveWorker = new HashMap<>();

    /**
     * Data synchronizer to synchronize data with other workers.
     */
    private DataSynchronizer<String, Map> dataSynchronizer;

    /**
     * Listeners to trigger while worker change.
     */
    private Set<ClusterManagementService.WorkerStatusListener> workerStatusListener;

    /**
     * Thread pool for scheduled tasks.
     */
    private final ScheduledExecutorService scheduledExecutorService;

    /**
     * Configs of current worker.
     */
    private final ConnectConfig connectConfig;

    public ClusterManagementServiceImpl(ConnectConfig connectConfig) {
        this.connectConfig = connectConfig;
        this.dataSynchronizer = new BrokerBasedLog<>(connectConfig,
            CLUSTER_MESSAGE_TOPIC,
            connectConfig.getWorkerId() + System.currentTimeMillis(),
            new ClusterChangeCallback(),
            new JsonConverter(),
            new JsonConverter());
        this.workerStatusListener = new HashSet<>();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor((r) ->
            new Thread(r, "HeartBeatScheduledThread"));
    }

    @Override
    public void start() {

        dataSynchronizer.start();

        // On worker online
        sendOnlineHeartBeat();

        // check whether a machine is offline
        this.scheduledExecutorService.scheduleAtFixedRate(() -> {

            try {

                boolean changed = false;
                for (String workerId : aliveWorker.keySet()) {
                    if ((aliveWorker.get(workerId) + ClusterManagementService.WORKER_TIME_OUT) < System.currentTimeMillis()) {
                        changed = true;
                        aliveWorker.remove(workerId);
                    }
                }
                if (!changed) {
                    return;
                }
                for (WorkerStatusListener listener : ClusterManagementServiceImpl.this.workerStatusListener) {
                    listener.onWorkerChange();
                }
            } catch (Exception e) {
                log.error("schedule cluster alive workers error.", e);
            }
        }, 1000, 20 * 1000, TimeUnit.MILLISECONDS);

        // Send heart beat periodically.
        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                sendAliveHeartBeat();
            } catch (Exception e) {
                log.error("schedule alive heart beat error.", e);
            }
        }, 1000, 10 * 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {

        sendOffLineHeartBeat();
        this.scheduledExecutorService.shutdown();
        try {
            this.scheduledExecutorService.awaitTermination(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("shutdown scheduledExecutorService error.", e);
        }
        dataSynchronizer.stop();
    }

    public void sendAliveHeartBeat() {

        aliveWorker.put(connectConfig.getWorkerId(), System.currentTimeMillis());
        dataSynchronizer.send(HeartBeatEnum.ALIVE.name(), aliveWorker);
    }

    public void sendOnlineHeartBeat() {

        aliveWorker.put(connectConfig.getWorkerId(), System.currentTimeMillis());
        dataSynchronizer.send(HeartBeatEnum.ONLINE_BEGIN.name(), aliveWorker);
    }

    public void sendOnlineFinishHeartBeat() {

        aliveWorker.put(connectConfig.getWorkerId(), System.currentTimeMillis());
        dataSynchronizer.send(HeartBeatEnum.ONLINE_FINISH.name(), aliveWorker);
    }

    public void sendOffLineHeartBeat() {

        Map<String, Long> offlineMap = new HashMap<>();
        offlineMap.put(connectConfig.getWorkerId(), System.currentTimeMillis());
        dataSynchronizer.send(HeartBeatEnum.OFFLINE.name(), offlineMap);
    }

    @Override
    public Map<String, Long> getAllAliveWorkers() {

        return this.aliveWorker;
    }

    @Override
    public void registerListener(WorkerStatusListener listener) {

        this.workerStatusListener.add(listener);
    }

    /**
     * Merge new received alive worker with info stored in memory.
     *
     * @param newAliveWorkerInfo
     * @return
     */
    private boolean mergeAliveWorker(Map<String, Long> newAliveWorkerInfo) {

        removeExpiredWorker(newAliveWorkerInfo);
        boolean changed = false;
        for (String workerId : newAliveWorkerInfo.keySet()) {

            Long lastAliveTime = aliveWorker.get(workerId);
            if (null == lastAliveTime) {

                changed = true;
                aliveWorker.put(workerId, newAliveWorkerInfo.get(workerId));
            } else {

                if (newAliveWorkerInfo.get(workerId) > lastAliveTime) {
                    changed = true;
                    aliveWorker.put(workerId, newAliveWorkerInfo.get(workerId));
                }
            }
        }

        return removeExpiredWorker(aliveWorker) && changed;
    }

    /**
     * Remove expired workers in {@link ClusterManagementServiceImpl#aliveWorker}.
     *
     * @param aliveWorker
     * @return
     */
    private boolean removeExpiredWorker(Map<String, Long> aliveWorker) {

        boolean changed = false;
        Iterator<String> iterator = aliveWorker.keySet().iterator();
        while (iterator.hasNext()) {
            String workerId = iterator.next();
            if (aliveWorker.get(workerId) + ClusterManagementService.WORKER_TIME_OUT < System.currentTimeMillis()) {
                changed = true;
                iterator.remove();
            }
        }
        return changed;
    }

    /**
     * Callback from {@link ClusterManagementServiceImpl#dataSynchronizer}.
     */
    private class ClusterChangeCallback implements DataSynchronizerCallback<String, Map> {

        @Override
        public void onCompletion(Throwable error, String heartBeatEnum, Map result) {

            boolean changed = true;
            switch (HeartBeatEnum.valueOf(heartBeatEnum)) {

                case ALIVE:
                    changed = mergeAliveWorker(result);
                    break;
                case ONLINE_BEGIN:
                    mergeAliveWorker(result);
                    changed = false;
                    sendOnlineFinishHeartBeat();
                    break;
                case ONLINE_FINISH:
                    mergeAliveWorker(result);
                    changed = true;
                    break;
                case OFFLINE:
                    for (Object key : result.keySet()) {
                        String workerId = (String) key;
                        Long offlineTime = (Long) result.get(workerId);
                        Long lastOnlineTime = aliveWorker.get(workerId);
                        if (null == lastOnlineTime || lastOnlineTime > offlineTime) {
                            changed = false;
                        } else {
                            changed = true;
                            aliveWorker.remove(workerId);
                        }
                    }
                    break;
                default:
                    break;
            }
            if (!changed) {
                return;
            }
            for (WorkerStatusListener listener : ClusterManagementServiceImpl.this.workerStatusListener) {
                listener.onWorkerChange();
            }
        }
    }

    private enum HeartBeatEnum {

        /**
         * Send when first online.
         */
        ONLINE_BEGIN,
        /**
         * Send after receive online_begin.
         */
        ONLINE_FINISH,
        /**
         * Send when offline.
         */
        OFFLINE,
        /**
         * Alive heartbeat
         */
        ALIVE
    }
}
