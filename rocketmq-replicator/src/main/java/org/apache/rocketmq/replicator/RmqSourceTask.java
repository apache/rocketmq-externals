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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.replicator;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.data.DataEntryBuilder;
import io.openmessaging.connector.api.data.EntryType;
import io.openmessaging.connector.api.data.Field;
import io.openmessaging.connector.api.data.FieldType;
import io.openmessaging.connector.api.data.Schema;
import io.openmessaging.connector.api.data.SourceDataEntry;
import io.openmessaging.connector.api.source.SourceTask;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.replicator.common.Utils;
import org.apache.rocketmq.replicator.config.ConfigUtil;
import org.apache.rocketmq.replicator.config.DataType;
import org.apache.rocketmq.replicator.config.TaskConfig;
import org.apache.rocketmq.replicator.config.TaskTopicInfo;
import org.apache.rocketmq.replicator.schema.FieldName;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class RmqSourceTask extends SourceTask {

    private static final Logger log = LoggerFactory.getLogger(RmqSourceTask.class);

    private final String taskId;
    private final TaskConfig config;
    private final DefaultMQPullConsumer consumer;
    private volatile boolean started = false;

    private Map<TaskTopicInfo, Long> mqOffsetMap;

    public RmqSourceTask() {
        this.config = new TaskConfig();
        this.consumer = new DefaultMQPullConsumer();
        this.taskId = Utils.createTaskId(Thread.currentThread().getName());
        mqOffsetMap = new HashMap<>();
    }

    public Collection<SourceDataEntry> poll() {

        if (this.config.getDataType() == DataType.COMMON_MESSAGE.ordinal()) {
            return pollCommonMessage();
        } else if (this.config.getDataType() == DataType.TOPIC_CONFIG.ordinal()) {
            return pollTopicConfig();
        } else if (this.config.getDataType() == DataType.BROKER_CONFIG.ordinal()) {
            return pollBrokerConfig();
        } else {
            return pollSubConfig();
        }
    }

    public void start(KeyValue config) {
        ConfigUtil.load(config, this.config);
        this.consumer.setConsumerGroup(this.taskId);
        this.consumer.setNamesrvAddr(this.config.getSourceRocketmq());
        this.consumer.setInstanceName(Utils.createInstanceName(this.config.getSourceRocketmq()));
        List<TaskTopicInfo> topicList = JSONObject.parseArray(this.config.getTaskTopicList(), TaskTopicInfo.class);

        try {
            if (topicList == null) {
                throw new IllegalStateException("topicList is null");
            }

            this.consumer.start();
            for (TaskTopicInfo tti : topicList) {
                Set<MessageQueue> mqs = consumer.fetchSubscribeMessageQueues(tti.getTopic());
                for (MessageQueue mq : mqs) {
                    if (tti.getQueueId() == mq.getQueueId()) {
                        ByteBuffer positionInfo = this.context.positionStorageReader().getPosition(
                            ByteBuffer.wrap(RmqConstants.getPartition(
                                mq.getTopic(),
                                mq.getBrokerName(),
                                String.valueOf(mq.getQueueId())).getBytes(StandardCharsets.UTF_8)));

                        if (null != positionInfo && positionInfo.array().length > 0) {
                            String positionJson = new String(positionInfo.array(), StandardCharsets.UTF_8);
                            JSONObject jsonObject = JSONObject.parseObject(positionJson);
                            this.config.setNextPosition(jsonObject.getLong(RmqConstants.NEXT_POSITION));
                        } else {
                            this.config.setNextPosition(0L);
                        }
                        mqOffsetMap.put(tti, this.config.getNextPosition());
                    }
                }
            }
            started = true;
        } catch (Exception e) {
            log.error("Consumer of task {} start failed.", this.taskId, e);
        }
        log.info("RocketMQ source task started");
    }

    @Override public void stop() {

        if (started) {
            if (this.consumer != null) {
                this.consumer.shutdown();
            }
            started = false;
        }
    }

    public void pause() {

    }

    public void resume() {

    }

    private Collection<SourceDataEntry> pollCommonMessage() {

        List<SourceDataEntry> res = new ArrayList<>();
        if (started) {
            try {
                for (TaskTopicInfo taskTopicConfig : this.mqOffsetMap.keySet()) {
                    PullResult pullResult = consumer.pull(taskTopicConfig, "*",
                        this.mqOffsetMap.get(taskTopicConfig), 32);
                    switch (pullResult.getPullStatus()) {
                        case FOUND: {
                            this.mqOffsetMap.put(taskTopicConfig, pullResult.getNextBeginOffset());
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put(RmqConstants.NEXT_POSITION, pullResult.getNextBeginOffset());
                            List<MessageExt> msgs = pullResult.getMsgFoundList();
                            Schema schema = new Schema();
                            schema.setDataSource(this.config.getSourceRocketmq());
                            schema.setName(taskTopicConfig.getTopic());
                            schema.setFields(new ArrayList<>());
                            schema.getFields().add(new Field(0,
                                FieldName.COMMON_MESSAGE.getKey(), FieldType.STRING));

                            DataEntryBuilder dataEntryBuilder = new DataEntryBuilder(schema);
                            dataEntryBuilder.timestamp(System.currentTimeMillis())
                                .queue(this.config.getStoreTopic()).entryType(EntryType.CREATE);
                            for (MessageExt msg : msgs) {
                                dataEntryBuilder.putFiled(FieldName.COMMON_MESSAGE.getKey(), new String(msg.getBody()));
                                SourceDataEntry sourceDataEntry = dataEntryBuilder.buildSourceDataEntry(
                                    ByteBuffer.wrap(RmqConstants.getPartition(
                                        taskTopicConfig.getTopic(),
                                        taskTopicConfig.getBrokerName(),
                                        String.valueOf(taskTopicConfig.getQueueId())).getBytes(StandardCharsets.UTF_8)),
                                    ByteBuffer.wrap(jsonObject.toJSONString().getBytes(StandardCharsets.UTF_8))
                                );
                                sourceDataEntry.setQueueName(taskTopicConfig.getTargetTopic());
                                res.add(sourceDataEntry);
                            }

                            break;
                        }
                        default:
                            break;
                    }
                }
            } catch (Exception e) {
                log.error("Rocketmq replicator task poll error, current config: {}", JSON.toJSONString(config), e);
            }
        } else {
            if (System.currentTimeMillis() % 1000 == 0) {
                log.warn("Rocketmq replicator task is not started.");
            }
        }
        return res;
    }

    private Collection<SourceDataEntry> pollTopicConfig() {
        DefaultMQAdminExt srcMQAdminExt;
        return new ArrayList<>();
    }

    private Collection<SourceDataEntry> pollBrokerConfig() {
        return new ArrayList<>();
    }

    private Collection<SourceDataEntry> pollSubConfig() {
        return new ArrayList<>();
    }
}

