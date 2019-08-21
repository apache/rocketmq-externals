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

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.rocketmq.connect.runtime.config.ConnectConfig;
import org.apache.rocketmq.connect.runtime.converter.ByteBufferConverter;
import org.apache.rocketmq.connect.runtime.converter.ByteMapConverter;
import org.apache.rocketmq.connect.runtime.converter.JsonConverter;
import org.apache.rocketmq.connect.runtime.store.FileBaseKeyValueStore;
import org.apache.rocketmq.connect.runtime.store.KeyValueStore;
import org.apache.rocketmq.connect.runtime.utils.ConnectUtil;
import org.apache.rocketmq.connect.runtime.utils.FilePathConfigUtil;
import org.apache.rocketmq.connect.runtime.utils.datasync.BrokerBasedLog;
import org.apache.rocketmq.connect.runtime.utils.datasync.DataSynchronizer;
import org.apache.rocketmq.connect.runtime.utils.datasync.DataSynchronizerCallback;

public class OffsetManagementServiceImpl implements PositionManagementService {

    /**
     * Current offset info in store.
     */
    private KeyValueStore<ByteBuffer, ByteBuffer> offsetStore;

    /**
     * Synchronize data with other workers.
     */
    private DataSynchronizer<String, Map<ByteBuffer, ByteBuffer>> dataSynchronizer;

    private final String offsetManagePrefix = "OffsetManage";

    /**
     * Listeners.
     */
    private Set<PositionUpdateListener> offsetUpdateListener;

    public OffsetManagementServiceImpl(ConnectConfig connectConfig) {

        this.offsetStore = new FileBaseKeyValueStore<>(FilePathConfigUtil.getOffsetPath(connectConfig.getStorePathRootDir()),
            new ByteBufferConverter(),
            new ByteBufferConverter());
        this.dataSynchronizer = new BrokerBasedLog(connectConfig,
            connectConfig.getOffsetStoreTopic(),
            ConnectUtil.createGroupName(offsetManagePrefix),
            new OffsetChangeCallback(),
            new JsonConverter(),
            new ByteMapConverter());
        this.offsetUpdateListener = new HashSet<>();
    }

    @Override
    public void start() {

        offsetStore.load();
        dataSynchronizer.start();
        sendOnlineOffsetInfo();
    }

    @Override
    public void stop() {

        offsetStore.persist();
        dataSynchronizer.stop();
    }

    @Override
    public void persist() {

        offsetStore.persist();
    }

    @Override
    public Map<ByteBuffer, ByteBuffer> getPositionTable() {

        return offsetStore.getKVMap();
    }

    @Override
    public void putPosition(Map<ByteBuffer, ByteBuffer> offsets) {

        offsetStore.putAll(offsets);
        sendSynchronizeOffset();
    }

    @Override
    public void removePosition(List<ByteBuffer> offsets) {

        if (null == offsets) {
            return;
        }
        for (ByteBuffer offset : offsets) {
            offsetStore.remove(offset);
        }
    }

    @Override
    public void registerListener(PositionUpdateListener listener) {

        this.offsetUpdateListener.add(listener);
    }

    private void sendOnlineOffsetInfo() {

        dataSynchronizer.send(OffsetChangeEnum.ONLINE_KEY.name(), offsetStore.getKVMap());
    }

    private void sendSynchronizeOffset() {

        dataSynchronizer.send(OffsetChangeEnum.OFFSET_CHANG_KEY.name(), offsetStore.getKVMap());
    }

    private class OffsetChangeCallback implements DataSynchronizerCallback<String, Map<ByteBuffer, ByteBuffer>> {

        @Override
        public void onCompletion(Throwable error, String key, Map<ByteBuffer, ByteBuffer> result) {

            // update offsetStore
            OffsetManagementServiceImpl.this.persist();

            boolean changed = false;
            switch (OffsetChangeEnum.valueOf(key)) {
                case ONLINE_KEY:
                    mergeOffsetInfo(result);
                    changed = true;
                    sendSynchronizeOffset();
                    break;
                case OFFSET_CHANG_KEY:
                    changed = mergeOffsetInfo(result);
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
        for (PositionUpdateListener offsetUpdateListener : offsetUpdateListener) {
            offsetUpdateListener.onPositionUpdate();
        }
    }

    /**
     * Merge new received offset info with local store.
     *
     * @param result
     * @return
     */
    private boolean mergeOffsetInfo(Map<ByteBuffer, ByteBuffer> result) {

        boolean changed = false;
        if (null == result || 0 == result.size()) {
            return changed;
        }

        for (Map.Entry<ByteBuffer, ByteBuffer> newEntry : result.entrySet()) {
            boolean find = false;
            for (Map.Entry<ByteBuffer, ByteBuffer> existedEntry : offsetStore.getKVMap().entrySet()) {
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
                offsetStore.put(newEntry.getKey(), newEntry.getValue());
            }
        }
        return changed;
    }

    private enum OffsetChangeEnum {

        /**
         * Insert or update offset info.
         */
        OFFSET_CHANG_KEY,

        /**
         * A worker online.
         */
        ONLINE_KEY
    }
}

