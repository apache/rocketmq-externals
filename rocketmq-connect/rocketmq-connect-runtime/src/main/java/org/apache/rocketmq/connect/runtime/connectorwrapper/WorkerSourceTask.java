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
import io.openmessaging.connector.api.data.Converter;
import io.openmessaging.connector.api.data.SourceDataEntry;
import io.openmessaging.connector.api.source.SourceTask;
import io.openmessaging.connector.api.source.SourceTaskContext;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.connect.runtime.common.ConnectKeyValue;
import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.apache.rocketmq.connect.runtime.common.MessageWrapper;
import org.apache.rocketmq.connect.runtime.common.PositionWrapper;
import org.apache.rocketmq.connect.runtime.config.RuntimeConfigDefine;
import org.apache.rocketmq.connect.runtime.converter.AbstractConverter;
import org.apache.rocketmq.connect.runtime.service.PositionManagementService;
import org.apache.rocketmq.connect.runtime.store.PositionStorageReaderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper of {@link SourceTask} for runtime.
 */
public class WorkerSourceTask implements WorkerTask {

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
     * The configs of current source task.
     */
    private ConnectKeyValue taskConfig;

    /**
     * Atomic state variable
     */
    private AtomicReference<WorkerTaskState> state;

    private final PositionManagementService positionManagementService;

    /**
     * Used to read the position of source data source.
     */
    private PositionStorageReader positionStorageReader;

    /**
     * A RocketMQ producer to send message to dest MQ.
     */
    private DefaultMQProducer producer;

    /**
     * A converter to parse source data entry to byte[].
     */
    private Converter recordConverter;

    private final AtomicReference<WorkerState> workerState;

    public WorkerSourceTask(String connectorName,
        SourceTask sourceTask,
        ConnectKeyValue taskConfig,
        PositionManagementService positionManagementService,
        Converter recordConverter,
        DefaultMQProducer producer,
        AtomicReference<WorkerState> workerState) {
        this.connectorName = connectorName;
        this.sourceTask = sourceTask;
        this.taskConfig = taskConfig;
        this.positionManagementService = positionManagementService;
        this.positionStorageReader = new PositionStorageReaderImpl(positionManagementService);
        this.producer = producer;
        this.recordConverter = recordConverter;
        this.state = new AtomicReference<>(WorkerTaskState.NEW);
        this.workerState = workerState;
    }

    /**
     * Start a source task, and send data entry to MQ cyclically.
     */
    @Override
    public void run() {
        try {
            producer.start();
            log.info("Source task producer start.");
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
            while (WorkerState.STARTED == workerState.get() && WorkerTaskState.RUNNING == state.get()) {
                try {
                    Collection<SourceDataEntry> toSendEntries = sourceTask.poll();
                    if (null != toSendEntries && toSendEntries.size() > 0) {
                        sendRecord(toSendEntries);
                    }
                } catch (Exception e) {
                    log.error("Source task runtime exception", e);
                    state.set(WorkerTaskState.ERROR);
                }
            }
            sourceTask.stop();
            state.compareAndSet(WorkerTaskState.STOPPING, WorkerTaskState.STOPPED);
            log.info("Source task stop, config:{}", JSON.toJSONString(taskConfig));
        } catch (Exception e) {
            log.error("Run task failed.", e);
            state.set(WorkerTaskState.ERROR);
        } finally {
            if (producer != null) {
                producer.shutdown();
                log.info("Source task producer shutdown.");
            }
        }
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

    /**
     * Send list of sourceDataEntries to MQ.
     *
     * @param sourceDataEntries
     */
    private void sendRecord(Collection<SourceDataEntry> sourceDataEntries) {
        for (SourceDataEntry sourceDataEntry : sourceDataEntries) {
            MessageWrapper messageWrapper = ((AbstractConverter) recordConverter).converter(sourceDataEntry);

            if (messageWrapper.getMessage().getBody().length > RuntimeConfigDefine.MAX_MESSAGE_SIZE) {
                log.error("Send record, message size is greater than {} bytes, payload: {}",
                    RuntimeConfigDefine.MAX_MESSAGE_SIZE, sourceDataEntry.getPayload());
                continue;
            }

            try {
                producer.send(messageWrapper.getMessage(), new SendCallback() {
                    @Override public void onSuccess(org.apache.rocketmq.client.producer.SendResult result) {
                        log.info("Successful send message to RocketMQ:{}", result.getMsgId());
                        try {
                            PositionWrapper positionWrapper = messageWrapper.getPositionWrapper();
                            if (null != positionWrapper.getPartition() && null != positionWrapper.getPosition()) {
                                positionManagementService.putPosition(positionWrapper.getPartition(), positionWrapper.getPosition());
                            }
                        } catch (Exception e) {
                            log.error("Source task save position info failed.", e);
                        }
                    }

                    @Override public void onException(Throwable throwable) {
                        if (null != throwable) {
                            log.error("Source task send record failed {}.", throwable);
                        }
                    }
                });
            } catch (Exception e) {
                log.error("Send message error. message: {}, error info: {}.", messageWrapper.getMessage(), e);
            }
        }
    }

    @Override
    public WorkerTaskState getState() {
        return this.state.get();
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
    public void timeout() {
        this.state.set(WorkerTaskState.ERROR);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("connectorName:" + connectorName)
            .append("\nConfigs:" + JSON.toJSONString(taskConfig))
            .append("\nState:" + state.get().toString());
        return sb.toString();
    }

    @Override
    public Object getJsonObject() {
        HashMap obj = new HashMap<String, Object>();
        obj.put("connectorName", connectorName);
        obj.put("configs", JSON.toJSONString(taskConfig));
        obj.put("state", state.get().toString());
        return obj;
    }
}
