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
import io.openmessaging.connector.api.data.Converter;
import io.openmessaging.connector.api.data.DataEntryBuilder;
import io.openmessaging.connector.api.data.EntryType;
import io.openmessaging.connector.api.data.Field;
import io.openmessaging.connector.api.data.Schema;
import io.openmessaging.connector.api.data.SinkDataEntry;
import io.openmessaging.connector.api.data.SourceDataEntry;
import io.openmessaging.connector.api.sink.SinkTask;
import io.openmessaging.connector.api.sink.SinkTaskContext;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.consumer.PullStatus;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.connect.runtime.common.ConnectKeyValue;
import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.apache.rocketmq.connect.runtime.config.RuntimeConfigDefine;
import org.apache.rocketmq.connect.runtime.converter.JsonConverter;
import org.apache.rocketmq.connect.runtime.converter.RocketMQConverter;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper of {@link SinkTask} for runtime.
 */
public class WorkerSinkTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_RUNTIME);

    /**
     * The configuration key that provides the list of topicNames that are inputs for this SinkTask.
     */
    public static final String QUEUENAMES_CONFIG = "topicNames";

    /**
     * Connector name of current task.
     */
    private String connectorName;

    /**
     * The implements of the sink task.
     */
    private SinkTask sinkTask;

    /**
     * The configs of current sink task.
     */
    private ConnectKeyValue taskConfig;

    /**
     * A switch for the sink task.
     */
    private AtomicBoolean isStopping;

    /**
     * A RocketMQ consumer to pull message from MQ.
     */
    private final DefaultMQPullConsumer consumer;

    /**
     *
     */
    private final PositionStorageReader offsetStorageReader;

    /**
     * A converter to parse sink data entry to object.
     */
    private Converter recordConverter;

    /**
     * Current position info of the source task.
     */
    private Map<ByteBuffer, ByteBuffer> offsetData = new HashMap<>();

    private final ConcurrentHashMap<MessageQueue, Long> messageQueuesOffsetMap;

    private final ConcurrentHashMap<MessageQueue, QueueState> messageQueuesStateMap;

    private static final Integer TIMEOUT = 3 * 1000;

    private static final Integer MAX_MESSAGE_NUM = 32;

    private static final String COMMA = ",";

    public static final String OFFSET_COMMIT_TIMEOUT_MS_CONFIG = "offset.flush.timeout.ms";

    private long nextCommitTime = 0;

    public WorkerSinkTask(String connectorName,
        SinkTask sinkTask,
        ConnectKeyValue taskConfig,
        PositionStorageReader offsetStorageReader,
        Converter recordConverter,
        DefaultMQPullConsumer consumer) {
        this.connectorName = connectorName;
        this.sinkTask = sinkTask;
        this.taskConfig = taskConfig;
        this.isStopping = new AtomicBoolean(false);
        this.consumer = consumer;
        this.offsetStorageReader = offsetStorageReader;
        this.recordConverter = recordConverter;
        this.messageQueuesOffsetMap = new ConcurrentHashMap<>(256);
        this.messageQueuesStateMap = new ConcurrentHashMap<>(256);
    }

    /**
     * Start a sink task, and receive data entry from MQ cyclically.
     */
    @Override
    public void run() {
        try {
            sinkTask.initialize(new SinkTaskContext() {
                @Override
                public void resetOffset(QueueMetaData queueMetaData, Long offset) {
                    String shardingKey = queueMetaData.getShardingKey();
                    String queueName = queueMetaData.getQueueName();
                    if (StringUtils.isNotEmpty(shardingKey) && StringUtils.isNotEmpty(queueName)) {
                        String[] s = shardingKey.split(COMMA);
                        if (s.length == 2 && StringUtils.isNotEmpty(s[0]) && StringUtils.isNotEmpty(s[1])) {
                            String brokerName = s[0];
                            Integer queueId = Integer.valueOf(s[1]);
                            MessageQueue messageQueue = new MessageQueue(queueName, brokerName, queueId);
                            messageQueuesOffsetMap.put(messageQueue, offset);
                            offsetData.put(convertToByteBufferKey(messageQueue), convertToByteBufferValue(offset));
                            return;
                        }
                    }
                    log.warn("Missing parameters, queueMetaData {}", queueMetaData);
                }

                @Override
                public void resetOffset(Map<QueueMetaData, Long> offsets) {
                    for (Map.Entry<QueueMetaData, Long> entry : offsets.entrySet()) {
                        String shardingKey = entry.getKey().getShardingKey();
                        String queueName = entry.getKey().getQueueName();
                        if (StringUtils.isNotEmpty(shardingKey) && StringUtils.isNotEmpty(queueName)) {
                            String[] s = shardingKey.split(COMMA);
                            if (s.length == 2 && StringUtils.isNotEmpty(s[0]) && StringUtils.isNotEmpty(s[1])) {
                                String brokerName = s[0];
                                Integer queueId = Integer.valueOf(s[1]);
                                MessageQueue messageQueue = new MessageQueue(queueName, brokerName, queueId);
                                messageQueuesOffsetMap.put(messageQueue, entry.getValue());
                                offsetData.put(convertToByteBufferKey(messageQueue), convertToByteBufferValue(entry.getValue()));
                                continue;
                            }
                        }
                        log.warn("Missing parameters, queueMetaData {}", entry.getKey());
                    }
                }

                @Override
                public void pause(List<QueueMetaData> queueMetaDatas) {
                    if (null != queueMetaDatas && queueMetaDatas.size() > 0) {
                        for (QueueMetaData queueMetaData : queueMetaDatas) {
                            String shardingKey = queueMetaData.getShardingKey();
                            String queueName = queueMetaData.getQueueName();
                            if (StringUtils.isNotEmpty(shardingKey) && StringUtils.isNotEmpty(queueName)) {
                                String[] s = shardingKey.split(COMMA);
                                if (s.length == 2 && StringUtils.isNotEmpty(s[0]) && StringUtils.isNotEmpty(s[1])) {
                                    String brokerName = s[0];
                                    Integer queueId = Integer.valueOf(s[1]);
                                    MessageQueue messageQueue = new MessageQueue(queueName, brokerName, queueId);
                                    messageQueuesStateMap.put(messageQueue, QueueState.PAUSE);
                                    continue;
                                }
                            }
                            log.warn("Missing parameters, queueMetaData {}", queueMetaData);
                        }
                    }
                }

                @Override
                public void resume(List<QueueMetaData> queueMetaDatas) {
                    if (null != queueMetaDatas && queueMetaDatas.size() > 0) {
                        for (QueueMetaData queueMetaData : queueMetaDatas) {
                            String shardingKey = queueMetaData.getShardingKey();
                            String queueName = queueMetaData.getQueueName();
                            if (StringUtils.isNotEmpty(shardingKey) && StringUtils.isNotEmpty(queueName)) {
                                String[] s = shardingKey.split(COMMA);
                                if (s.length == 2 && StringUtils.isNotEmpty(s[0]) && StringUtils.isNotEmpty(s[1])) {
                                    String brokerName = s[0];
                                    Integer queueId = Integer.valueOf(s[1]);
                                    MessageQueue messageQueue = new MessageQueue(queueName, brokerName, queueId);
                                    messageQueuesStateMap.remove(messageQueue);
                                    continue;
                                }
                            }
                            log.warn("Missing parameters, queueMetaData {}", queueMetaData);
                        }
                    }
                }

                @Override
                public KeyValue configs() {
                    return taskConfig;
                }
            });
            String topicNamesStr = taskConfig.getString(QUEUENAMES_CONFIG);

            if (!StringUtils.isEmpty(topicNamesStr)) {
                String[] topicNames = topicNamesStr.split(COMMA);
                for (String topicName : topicNames) {
                    final Set<MessageQueue> messageQueues = consumer.fetchSubscribeMessageQueues(topicName);
                    for (MessageQueue messageQueue : messageQueues) {
                        final long offset = consumer.searchOffset(messageQueue, TIMEOUT);
                        messageQueuesOffsetMap.put(messageQueue, offset);
                    }
                    messageQueues.addAll(messageQueues);
                }
                log.debug("{} Initializing and starting task for topicNames {}", this, topicNames);
            } else {
                log.error("Lack of sink comsume topicNames config");
            }

            for (Map.Entry<MessageQueue, Long> entry : messageQueuesOffsetMap.entrySet()) {
                MessageQueue messageQueue = entry.getKey();
                ByteBuffer byteBuffer = offsetStorageReader.getPosition(convertToByteBufferKey(messageQueue));
                if (null != byteBuffer) {
                    messageQueuesOffsetMap.put(messageQueue, convertToOffset(byteBuffer));
                }
            }
            sinkTask.start(taskConfig);
            log.info("Sink task start, config:{}", JSON.toJSONString(taskConfig));
            while (!isStopping.get()) {
                pullMessageFromQueues();
            }
            log.info("Sink task stop, config:{}", JSON.toJSONString(taskConfig));
        } catch (Exception e) {
            log.error("Run task failed.", e);
        }
    }

    private void pullMessageFromQueues() throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        for (Map.Entry<MessageQueue, Long> entry : messageQueuesOffsetMap.entrySet()) {
            final PullResult pullResult = consumer.pullBlockIfNotFound(entry.getKey(), "*", entry.getValue(), MAX_MESSAGE_NUM);
            if (pullResult.getPullStatus().equals(PullStatus.FOUND)) {
                final List<MessageExt> messages = pullResult.getMsgFoundList();
                removePauseQueueMessage(entry.getKey(), messages);
                receiveMessages(messages);
                messageQueuesOffsetMap.put(entry.getKey(), pullResult.getNextBeginOffset());
                offsetData.put(convertToByteBufferKey(entry.getKey()), convertToByteBufferValue(pullResult.getNextBeginOffset()));
                preCommit();
            }
        }
    }

    private void preCommit() {
        long commitInterval = taskConfig.getLong(OFFSET_COMMIT_TIMEOUT_MS_CONFIG, 1000);
        if (nextCommitTime <= 0) {
            long now = System.currentTimeMillis();
            nextCommitTime = now + commitInterval;
        }
        if (nextCommitTime < System.currentTimeMillis()) {
            Map<QueueMetaData, Long> queueMetaDataLongMap = new HashMap<>(512);
            if (messageQueuesOffsetMap.size() > 0) {
                for (Map.Entry<MessageQueue, Long> messageQueueLongEntry : messageQueuesOffsetMap.entrySet()) {
                    QueueMetaData queueMetaData = new QueueMetaData();
                    queueMetaData.setQueueName(messageQueueLongEntry.getKey().getTopic());
                    queueMetaData.setShardingKey(messageQueueLongEntry.getKey().getBrokerName() + COMMA + messageQueueLongEntry.getKey().getQueueId());
                    queueMetaDataLongMap.put(queueMetaData, messageQueueLongEntry.getValue());
                }
            }
            sinkTask.preCommit(queueMetaDataLongMap);
            nextCommitTime = 0;
        }
    }

    private void removePauseQueueMessage(MessageQueue messageQueue, List<MessageExt> messages) {
        if (null != messageQueuesStateMap.get(messageQueue)) {
            final Iterator<MessageExt> iterator = messages.iterator();
            while (iterator.hasNext()) {
                final MessageExt message = iterator.next();
                String msgId = message.getMsgId();
                log.info("BrokerName {}, topicName {}, queueId {} is pause, Discard the message {}", messageQueue.getBrokerName(), messageQueue.getTopic(), message.getQueueId(), msgId);
                iterator.remove();
            }
        }
    }

    public void stop() {
        isStopping.set(true);
        consumer.shutdown();
        sinkTask.stop();
    }

    /**
     * receive message from MQ.
     *
     * @param messages
     */
    private void receiveMessages(List<MessageExt> messages) {
        List<SinkDataEntry> sinkDataEntries = new ArrayList<>(32);
        for (MessageExt message : messages) {
            SinkDataEntry sinkDataEntry = convertToSinkDataEntry(message);
            sinkDataEntries.add(sinkDataEntry);
            String msgId = message.getMsgId();
            log.info("Received one message success : msgId {}", msgId);
        }
        sinkTask.put(sinkDataEntries);
    }

    private SinkDataEntry convertToSinkDataEntry(MessageExt message) {
        Map<String, String> properties = message.getProperties();
        String queueName;
        EntryType entryType;
        Schema schema;
        Long timestamp;
        Object[] datas = new Object[1];
        if (null == recordConverter || recordConverter instanceof RocketMQConverter) {
            queueName = properties.get(RuntimeConfigDefine.CONNECT_TOPICNAME);
            String connectEntryType = properties.get(RuntimeConfigDefine.CONNECT_ENTRYTYPE);
            entryType = StringUtils.isNotEmpty(connectEntryType) ? EntryType.valueOf(connectEntryType) : null;
            String connectTimestamp = properties.get(RuntimeConfigDefine.CONNECT_TIMESTAMP);
            timestamp = StringUtils.isNotEmpty(connectTimestamp) ? Long.valueOf(connectTimestamp) : null;
            String connectSchema = properties.get(RuntimeConfigDefine.CONNECT_SCHEMA);
            schema = StringUtils.isNotEmpty(connectSchema) ? JSON.parseObject(connectSchema, Schema.class) : null;
            datas = new Object[1];
            datas[0] = new String(message.getBody());
        } else {
            final byte[] messageBody = message.getBody();
            final SourceDataEntry sourceDataEntry = JSON.parseObject(new String(messageBody), SourceDataEntry.class);
            final Object[] payload = sourceDataEntry.getPayload();
            final byte[] decodeBytes = Base64.getDecoder().decode((String) payload[0]);
            Object recodeObject;
            if (recordConverter instanceof JsonConverter) {
                JsonConverter jsonConverter = (JsonConverter) recordConverter;
                jsonConverter.setClazz(Object[].class);
                recodeObject = recordConverter.byteToObject(decodeBytes);
                datas = (Object[]) recodeObject;
            }
            schema = sourceDataEntry.getSchema();
            entryType = sourceDataEntry.getEntryType();
            queueName = sourceDataEntry.getQueueName();
            timestamp = sourceDataEntry.getTimestamp();
        }
        DataEntryBuilder dataEntryBuilder = new DataEntryBuilder(schema);
        dataEntryBuilder.entryType(entryType);
        dataEntryBuilder.queue(queueName);
        dataEntryBuilder.timestamp(timestamp);
        SinkDataEntry sinkDataEntry = dataEntryBuilder.buildSinkDataEntry(message.getQueueOffset());
        List<Field> fields = schema.getFields();
        if (null != fields && !fields.isEmpty()) {
            for (Field field : fields) {
                dataEntryBuilder.putFiled(field.getName(), datas[field.getIndex()]);
            }
        }
        return sinkDataEntry;
    }

    public String getConnectorName() {
        return connectorName;
    }

    public ConnectKeyValue getTaskConfig() {
        return taskConfig;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("connectorName:" + connectorName)
            .append("\nConfigs:" + JSON.toJSONString(taskConfig));
        return sb.toString();
    }

    private enum QueueState {
        PAUSE
    }

    private ByteBuffer convertToByteBufferKey(MessageQueue messageQueue) {
        return ByteBuffer.wrap((messageQueue.getTopic() + COMMA + messageQueue.getBrokerName() + COMMA + messageQueue.getQueueId()).getBytes());
    }

    private MessageQueue convertToMessageQueue(ByteBuffer byteBuffer) {
        byte[] array = byteBuffer.array();
        String s = String.valueOf(array);
        String[] split = s.split(COMMA);
        return new MessageQueue(split[0], split[1], Integer.valueOf(split[2]));
    }

    private ByteBuffer convertToByteBufferValue(Long offset) {
        return ByteBuffer.wrap(String.valueOf(offset).getBytes());
    }

    private Long convertToOffset(ByteBuffer byteBuffer) {
        return Long.valueOf(new String(byteBuffer.array()));
    }

    public Map<ByteBuffer, ByteBuffer> getOffsetData() {
        return offsetData;
    }

}
