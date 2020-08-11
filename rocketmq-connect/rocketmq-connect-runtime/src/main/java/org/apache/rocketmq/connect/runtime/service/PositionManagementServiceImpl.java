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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.rocketmq.connect.runtime.common.PositionValue;
import org.apache.rocketmq.connect.runtime.config.ConnectConfig;
import org.apache.rocketmq.connect.runtime.converter.JsonConverter;
import org.apache.rocketmq.connect.runtime.converter.PositionMapConverter;
import org.apache.rocketmq.connect.runtime.converter.PositionValueConverter;
import org.apache.rocketmq.connect.runtime.store.FileBaseKeyValueStore;
import org.apache.rocketmq.connect.runtime.store.KeyValueStore;
import org.apache.rocketmq.connect.runtime.utils.ConnectUtil;
import org.apache.rocketmq.connect.runtime.utils.FilePathConfigUtil;
import org.apache.rocketmq.connect.runtime.utils.datasync.BrokerBasedLog;
import org.apache.rocketmq.connect.runtime.utils.datasync.DataSynchronizer;
import org.apache.rocketmq.connect.runtime.utils.datasync.DataSynchronizerCallback;

public class PositionManagementServiceImpl implements PositionManagementService {

    /**
     * Current position info in store.
     */
    private KeyValueStore<String, PositionValue> positionStore;

    /**
     * Synchronize data with other workers.
     */
    private DataSynchronizer<String, Map<String, PositionValue>> dataSynchronizer;

    /**
     * Listeners.
     */
    private Set<PositionUpdateListener> positionUpdateListener;

    private final String positionManagePrefix = "PositionManage";

    public PositionManagementServiceImpl(ConnectConfig connectConfig) {

        this.positionStore = new FileBaseKeyValueStore<>(FilePathConfigUtil.getPositionPath(connectConfig.getStorePathRootDir()),
            new JsonConverter(),
            new PositionValueConverter());
        this.dataSynchronizer = new BrokerBasedLog<>(connectConfig,
            connectConfig.getPositionStoreTopic(),
            ConnectUtil.createGroupName(positionManagePrefix),
            new PositionChangeCallback(),
            new JsonConverter(),
            new PositionMapConverter());
        this.positionUpdateListener = new HashSet<>();
    }

    @Override
    public void start() {

        positionStore.load();
        dataSynchronizer.start();
        sendOnlinePositionInfo();
    }

    @Override
    public void stop() {

        positionStore.persist();
        dataSynchronizer.stop();
    }

    @Override
    public void persist() {

        positionStore.persist();
    }

    @Override
    public Map<String, PositionValue> getPositionTable() {

        return positionStore.getKVMap();
    }

    @Override
    public void putPosition(Map<String, PositionValue> positions) {

        positionStore.putAll(positions);
        sendSynchronizePosition();
    }

    @Override
    public void removePosition(List<String> taskIds) {

        if (null == taskIds) {
            return;
        }
        for (String taskId : taskIds) {
            positionStore.remove(taskId);
        }
    }

    @Override
    public void registerListener(PositionUpdateListener listener) {

        this.positionUpdateListener.add(listener);
    }

    private void sendOnlinePositionInfo() {

        dataSynchronizer.send(PositionChangeEnum.ONLINE_KEY.name(), positionStore.getKVMap());
    }

    private void sendSynchronizePosition() {

        dataSynchronizer.send(PositionChangeEnum.POSITION_CHANG_KEY.name(), positionStore.getKVMap());
    }

    private class PositionChangeCallback implements DataSynchronizerCallback<String, Map<String, PositionValue>> {

        @Override
        public void onCompletion(Throwable error, String key, Map<String, PositionValue> result) {

            // update positionStore
            PositionManagementServiceImpl.this.persist();

            boolean changed = false;
            switch (PositionChangeEnum.valueOf(key)) {
                case ONLINE_KEY:
                    mergePositionInfo(result);
                    changed = true;
                    sendSynchronizePosition();
                    break;
                case POSITION_CHANG_KEY:
                    changed = mergePositionInfo(result);
                    break;
                default:
                    break;
            }
            if (changed) {
                triggerListener();
            }

        }
    }

    private void triggerListener() {
        for (PositionUpdateListener positionUpdateListener : positionUpdateListener) {
            positionUpdateListener.onPositionUpdate();
        }
    }

    /**
     * Merge new received position info with local store.
     *
     * @param result
     * @return
     */
    private boolean mergePositionInfo(Map<String, PositionValue> result) {

        boolean changed = false;
        if (null == result || 0 == result.size()) {
            return changed;
        }

        for (Map.Entry<String, PositionValue> newEntry : result.entrySet()) {
            boolean find = false;
            for (Map.Entry<String, PositionValue> existedEntry : positionStore.getKVMap().entrySet()) {
                if (newEntry.getKey().equals(existedEntry.getKey())) {
                    find = true;
                    if (!newEntry.getValue().equals(existedEntry.getValue())) {
                        changed = true;
                        existedEntry.setValue(newEntry.getValue());
                    }
                    break;
                }
            }
            if (!find) {
                positionStore.put(newEntry.getKey(), newEntry.getValue());
            }
        }
        return changed;
    }

    private enum PositionChangeEnum {

        /**
         * Insert or update position info.
         */
        POSITION_CHANG_KEY,

        /**
         * A worker online.
         */
        ONLINE_KEY
    }
}

