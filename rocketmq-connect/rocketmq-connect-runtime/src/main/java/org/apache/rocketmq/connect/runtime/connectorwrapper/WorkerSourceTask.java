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
import io.openmessaging.connector.api.data.EntryType;
import io.openmessaging.connector.api.data.Schema;
import io.openmessaging.connector.api.data.SourceDataEntry;
import io.openmessaging.connector.api.source.SourceTask;
import io.openmessaging.connector.api.source.SourceTaskContext;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang3.StringUtils;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageAccessor;
import org.apache.rocketmq.connect.runtime.common.ConnectKeyValue;
import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.apache.rocketmq.connect.runtime.config.RuntimeConfigDefine;
import org.apache.rocketmq.connect.runtime.converter.RocketMQConverter;
import org.apache.rocketmq.connect.runtime.service.PositionManagementService;
import org.apache.rocketmq.connect.runtime.store.PositionStorageReaderImpl;
import org.apache.rocketmq.remoting.exception.RemotingException;
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
            ByteBuffer partition = sourceDataEntry.getSourcePartition();
            Optional<ByteBuffer> opartition = Optional.ofNullable(partition);
            ByteBuffer position = sourceDataEntry.getSourcePosition();
            Optional<ByteBuffer> oposition = Optional.ofNullable(position);
            sourceDataEntry.setSourcePartition(null);
            sourceDataEntry.setSourcePosition(null);
            Message sourceMessage = new Message();
            sourceMessage.setTopic(sourceDataEntry.getQueueName());
            if (null == recordConverter || recordConverter instanceof RocketMQConverter) {
                if (StringUtils.isNotEmpty(sourceDataEntry.getShardingKey())) {
                    MessageAccessor.putProperty(sourceMessage, RuntimeConfigDefine.CONNECT_SHARDINGKEY, sourceDataEntry.getShardingKey());
                }
                if (StringUtils.isNotEmpty(sourceDataEntry.getQueueName())) {
                    MessageAccessor.putProperty(sourceMessage, RuntimeConfigDefine.CONNECT_TOPICNAME, sourceDataEntry.getQueueName());
                }
                if (opartition.isPresent()) {
                    MessageAccessor.putProperty(sourceMessage, RuntimeConfigDefine.CONNECT_SOURCE_PARTITION, new String(opartition.get().array()));
                }
                if (oposition.isPresent()) {
                    MessageAccessor.putProperty(sourceMessage, RuntimeConfigDefine.CONNECT_SOURCE_POSITION, new String(oposition.get().array()));
                }
                EntryType entryType = sourceDataEntry.getEntryType();
                Optional<EntryType> oentryType = Optional.ofNullable(entryType);
                if (oentryType.isPresent()) {
                    MessageAccessor.putProperty(sourceMessage, RuntimeConfigDefine.CONNECT_ENTRYTYPE, oentryType.get().name());
                }
                Long timestamp = sourceDataEntry.getTimestamp();
                Optional<Long> otimestamp = Optional.ofNullable(timestamp);
                if (otimestamp.isPresent()) {
                    MessageAccessor.putProperty(sourceMessage, RuntimeConfigDefine.CONNECT_TIMESTAMP, otimestamp.get().toString());
                }
                Schema schema = sourceDataEntry.getSchema();
                Optional<Schema> oschema = Optional.ofNullable(schema);
                if (oschema.isPresent()) {
                    MessageAccessor.putProperty(sourceMessage, RuntimeConfigDefine.CONNECT_SCHEMA, JSON.toJSONString(oschema.get()));
                }
                Object[] payload = sourceDataEntry.getPayload();
                if (null != payload && null != payload[0]) {
                    Object object = payload[0];
                    final byte[] messageBody = (String.valueOf(object)).getBytes();
                    if (messageBody.length > RuntimeConfigDefine.MAX_MESSAGE_SIZE) {
                        log.error("Send record, message size is greater than {} bytes, payload: {}", RuntimeConfigDefine.MAX_MESSAGE_SIZE, sourceDataEntry.getPayload());
                        return;
                    }
                    sourceMessage.setBody(messageBody);
                }
            } else {
                byte[] payload = recordConverter.objectToByte(sourceDataEntry.getPayload());
                Object[] newPayload = new Object[1];
                newPayload[0] = Base64.getEncoder().encodeToString(payload);
                sourceDataEntry.setPayload(newPayload);
                final byte[] messageBody = JSON.toJSONString(sourceDataEntry).getBytes();
                if (messageBody.length > RuntimeConfigDefine.MAX_MESSAGE_SIZE) {
                    log.error("Send record, message size is greater than {} bytes, payload: {}", RuntimeConfigDefine.MAX_MESSAGE_SIZE, sourceDataEntry.getPayload());
                    return;
                }
                sourceMessage.setBody(messageBody);
            }
            try {
                producer.send(sourceMessage, new SendCallback() {
                    @Override public void onSuccess(org.apache.rocketmq.client.producer.SendResult result) {
                        log.info("Successful send message to RocketMQ:{}", result.getMsgId());
                        try {
                            if (null != partition && null != position) {
                                positionManagementService.putPosition(partition, position);
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
            } catch (MQClientException e) {
                log.error("Send message error. message: {}, error info: {}.", sourceMessage, e);
            } catch (RemotingException e) {
                log.error("Send message error. message: {}, error info: {}.", sourceMessage, e);
            } catch (InterruptedException e) {
                log.error("Send message error. message: {}, error info: {}.", sourceMessage, e);
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
