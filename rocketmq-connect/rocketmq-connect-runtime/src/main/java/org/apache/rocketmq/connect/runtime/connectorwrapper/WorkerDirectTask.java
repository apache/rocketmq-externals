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

import com.alibaba.fastjson.JSON;
import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.PositionStorageReader;
import io.openmessaging.connector.api.common.QueueMetaData;
import io.openmessaging.connector.api.data.DataEntryBuilder;
import io.openmessaging.connector.api.data.Schema;
import io.openmessaging.connector.api.data.SinkDataEntry;
import io.openmessaging.connector.api.data.SourceDataEntry;
import io.openmessaging.connector.api.sink.SinkTask;
import io.openmessaging.connector.api.sink.SinkTaskContext;
import io.openmessaging.connector.api.source.SourceTask;
import io.openmessaging.connector.api.source.SourceTaskContext;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.rocketmq.connect.runtime.common.ConnectKeyValue;
import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.apache.rocketmq.connect.runtime.service.PositionManagementService;
import org.apache.rocketmq.connect.runtime.store.PositionStorageReaderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper of {@link SinkTask} and {@link SourceTask} for runtime.
 */
public class WorkerDirectTask implements WorkerTask {

    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_RUNTIME);

    /**
     * Connector name of current task.
     */
    private String connectorName;

    /**
     * The implements of the source task.
     */
    private SourceTask sourceTask;

    /**
     * The implements of the sink task.
     */
    private SinkTask sinkTask;

    /**
     * The configs of current sink task.
     */
    private ConnectKeyValue taskConfig;

    /**
     * Atomic state variable
     */
    private AtomicReference<WorkerTaskState> state;

    private final PositionManagementService positionManagementService;

    private final PositionStorageReader positionStorageReader;

    private final AtomicReference<WorkerState> workerState;

    public WorkerDirectTask(String connectorName,
        SourceTask sourceTask,
        SinkTask sinkTask,
        ConnectKeyValue taskConfig,
        PositionManagementService positionManagementService,
        AtomicReference<WorkerState> workerState) {
        this.connectorName = connectorName;
        this.sourceTask = sourceTask;
        this.sinkTask = sinkTask;
        this.taskConfig = taskConfig;
        this.positionManagementService = positionManagementService;
        this.positionStorageReader = new PositionStorageReaderImpl(positionManagementService);
        this.state = new AtomicReference<>(WorkerTaskState.NEW);
        this.workerState = workerState;
    }

    /**
     * Start a source task, and send data entry to MQ cyclically.
     */
    @Override
    public void run() {
        try {
            starkSinkTask();
            startSourceTask();
            log.info("Direct task start, config:{}", JSON.toJSONString(taskConfig));
            while (WorkerState.STARTED == workerState.get() && WorkerTaskState.RUNNING == state.get()) {
                try {
                    Collection<SourceDataEntry> toSendEntries = sourceTask.poll();
                    if (null != toSendEntries && toSendEntries.size() > 0) {
                        sendRecord(toSendEntries);
                    }
                } catch (Exception e) {
                    log.error("Direct task runtime exception", e);
                    state.set(WorkerTaskState.ERROR);
                }
            }
            stopSourceTask();
            stopSinkTask();
            state.compareAndSet(WorkerTaskState.STOPPING, WorkerTaskState.STOPPED);
            log.info("Direct task stop, config:{}", JSON.toJSONString(taskConfig));
        } catch (Exception e) {
            log.error("Run task failed.", e);
            state.set(WorkerTaskState.ERROR);
        }
    }

    private void sendRecord(Collection<SourceDataEntry> sourceDataEntries) {
        List<SinkDataEntry> sinkDataEntries = new ArrayList<>(sourceDataEntries.size());
        ByteBuffer position = null;
        ByteBuffer partition = null;
        for (SourceDataEntry sourceDataEntry : sourceDataEntries) {
            Schema schema = sourceDataEntry.getSchema();
            Object[] payload = sourceDataEntry.getPayload();
            DataEntryBuilder dataEntryBuilder = new DataEntryBuilder(schema)
                .entryType(sourceDataEntry.getEntryType())
                .queue(sourceDataEntry.getQueueName())
                .timestamp(sourceDataEntry.getTimestamp());
            if (schema.getFields() != null) {
                schema.getFields().forEach(field -> dataEntryBuilder.putFiled(field.getName(), payload[field.getIndex()]));
            }
            SinkDataEntry sinkDataEntry = dataEntryBuilder.buildSinkDataEntry(-1L);
            sinkDataEntries.add(sinkDataEntry);
            position = sourceDataEntry.getSourcePosition();
            partition = sourceDataEntry.getSourcePartition();
        }

        try {
            sinkTask.put(sinkDataEntries);
            try {
                if (null != position && null != partition) {
                    positionManagementService.putPosition(position, partition);
                }
            } catch (Exception e) {
                log.error("Source task save position info failed.", e);
            }
        } catch (Exception e) {
            log.error("Send message error, error info: {}.", e);
        }
    }

    private void starkSinkTask() {
        sinkTask.initialize(new SinkTaskContext() {

            @Override
            public KeyValue configs() {
                return taskConfig;
            }

            @Override
            public void resetOffset(QueueMetaData queueMetaData, Long offset) {

            }

            @Override
            public void resetOffset(Map<QueueMetaData, Long> offsets) {

            }

            @Override
            public void pause(List<QueueMetaData> queues) {

            }

            @Override
            public void resume(List<QueueMetaData> queues) {

            }
        });
        sinkTask.start(taskConfig);
        log.info("Sink task start, config:{}", JSON.toJSONString(taskConfig));
    }

    private void stopSinkTask() {
        sinkTask.stop();
        log.info("Sink task stop, config:{}", JSON.toJSONString(taskConfig));
    }

    private void startSourceTask() {
        state.compareAndSet(WorkerTaskState.NEW, WorkerTaskState.PENDING);
        sourceTask.initialize(new SourceTaskContext() {
            @Override
            public PositionStorageReader positionStorageReader() {
                return positionStorageReader;
            }

            @Override
            public KeyValue configs() {
                return taskConfig;
            }
        });
        sourceTask.start(taskConfig);
        state.compareAndSet(WorkerTaskState.PENDING, WorkerTaskState.RUNNING);
        log.info("Source task start, config:{}", JSON.toJSONString(taskConfig));
    }

    private void stopSourceTask() {
        sourceTask.stop();
        log.info("Source task stop, config:{}", JSON.toJSONString(taskConfig));
    }

    @Override
    public WorkerTaskState getState() {
        return this.state.get();
    }

    @Override
    public void stop() {
        state.compareAndSet(WorkerTaskState.RUNNING, WorkerTaskState.STOPPING);
    }

    @Override
    public void cleanup() {
        if (state.compareAndSet(WorkerTaskState.STOPPED, WorkerTaskState.TERMINATED) ||
            state.compareAndSet(WorkerTaskState.ERROR, WorkerTaskState.TERMINATED)) {
        } else {
            log.error("[BUG] cleaning a task but it's not in STOPPED or ERROR state");
        }
    }

    @Override
    public String getConnectorName() {
        return connectorName;
    }

    @Override
    public ConnectKeyValue getTaskConfig() {
        return taskConfig;
    }

    @Override
    public Object getJsonObject() {
        HashMap obj = new HashMap<String, Object>();
        obj.put("connectorName", connectorName);
        obj.put("configs", JSON.toJSONString(taskConfig));
        obj.put("state", state.get().toString());
        return obj;
    }

    @Override
    public void timeout() {
        this.state.set(WorkerTaskState.ERROR);
    }
}
