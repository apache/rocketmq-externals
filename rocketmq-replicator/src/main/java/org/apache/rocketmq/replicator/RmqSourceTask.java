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
import io.openmessaging.connector.api.data.*;
import io.openmessaging.connector.api.source.SourceTask;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;

public class RmqSourceTask extends SourceTask {

    private static final Logger log = LoggerFactory.getLogger(RmqSourceTask.class);

    private final String taskId;
    private final TaskConfig config;
    private final DefaultMQPullConsumer consumer;
    private volatile boolean started = false;

    private Map<MessageQueue, Long> mqOffsetMap;
    public RmqSourceTask() {
        this.config = new TaskConfig();
        this.consumer = new DefaultMQPullConsumer();
        this.taskId = Utils.createTaskId(Thread.currentThread().getName());
        mqOffsetMap = new HashMap<MessageQueue, Long>();
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
            this.consumer.start();
            for (TaskTopicInfo tti: topicList) {
                Set<MessageQueue> mqs = consumer.fetchSubscribeMessageQueues(tti.getSourceTopic());
                if (!tti.getQueueId().equals("")) {
                    // divide task by queue
                    for (MessageQueue mq: mqs) {
                        if (Integer.valueOf(tti.getQueueId()) == mq.getQueueId()) {
                            ByteBuffer positionInfo = this.context.positionStorageReader().getPosition(
                                    ByteBuffer.wrap(RmqConstants.getPartition(
                                            mq.getTopic(),
                                            mq.getBrokerName(),
                                            String.valueOf(mq.getQueueId())).getBytes("UTF-8")));

                            if (null != positionInfo && positionInfo.array().length > 0) {
                                String positionJson = new String(positionInfo.array(), "UTF-8");
                                JSONObject jsonObject = JSONObject.parseObject(positionJson);
                                this.config.setNextPosition(jsonObject.getLong(RmqConstants.NEXT_POSITION));
                            } else {
                                this.config.setNextPosition(0L);
                            }
                            mqOffsetMap.put(mq, this.config.getNextPosition());
                        }
                    }
                } else {
                    // divide task by topic
                    for (MessageQueue mq: mqs) {
                        ByteBuffer positionInfo = this.context.positionStorageReader().getPosition(
                                ByteBuffer.wrap(RmqConstants.getPartition(
                                        mq.getTopic(),
                                        mq.getBrokerName(),
                                        String.valueOf(mq.getQueueId())).getBytes("UTF-8")));

                        if (null != positionInfo && positionInfo.array().length > 0) {
                            String positionJson = new String(positionInfo.array(), "UTF-8");
                            JSONObject jsonObject = JSONObject.parseObject(positionJson);
                            this.config.setNextPosition(jsonObject.getLong(RmqConstants.NEXT_POSITION));
                        } else {
                            this.config.setNextPosition(0L);
                        }
                        mqOffsetMap.put(mq, this.config.getNextPosition());
                    }
                }
            }
            started = true;
        } catch (Exception e) {
            log.error("Consumer of task {} start failed.", this.taskId, e);
        }
        log.info("RocketMQ source task started");
    }

    public void stop() {
      
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

        List<SourceDataEntry> res = new ArrayList<SourceDataEntry>();
        if (started) {
            try {
                for (MessageQueue mq : this.mqOffsetMap.keySet()) {
                    PullResult pullResult = consumer.pull(mq, "*",
                            this.mqOffsetMap.get(mq), 32);
                    switch (pullResult.getPullStatus()) {
                        case FOUND: {
                            this.mqOffsetMap.put(mq, pullResult.getNextBeginOffset());
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put(RmqConstants.NEXT_POSITION, pullResult.getNextBeginOffset());
                            List<MessageExt> msgs = pullResult.getMsgFoundList();
                            Schema schema = new Schema();
                            schema.setDataSource(this.config.getSourceRocketmq());
                            schema.setName(mq.getTopic());
                            schema.setFields(new ArrayList<Field>());
                            schema.getFields().add(new Field(0,
                                    FieldName.COMMON_MESSAGE.getKey(), FieldType.STRING));

                            DataEntryBuilder dataEntryBuilder = new DataEntryBuilder(schema);
                            dataEntryBuilder.timestamp(System.currentTimeMillis())
                                    .queue(this.config.getStoreTopic()).entryType(EntryType.CREATE);
                            dataEntryBuilder.putFiled(FieldName.COMMON_MESSAGE.getKey(), JSONObject.toJSONString(msgs));
                            SourceDataEntry sourceDataEntry = dataEntryBuilder.buildSourceDataEntry(
                                    ByteBuffer.wrap(RmqConstants.getPartition(
                                            mq.getTopic(),
                                            mq.getBrokerName(),
                                            String.valueOf(mq.getQueueId())).getBytes("UTF-8")),
                                    ByteBuffer.wrap(jsonObject.toJSONString().getBytes("UTF-8"))
                            );
                            res.add(sourceDataEntry);
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
        return new ArrayList<SourceDataEntry>();
    }

    private Collection<SourceDataEntry> pollBrokerConfig() {
        return new ArrayList<SourceDataEntry>();
    }

    private Collection<SourceDataEntry> pollSubConfig() {
        return new ArrayList<SourceDataEntry>();
    }
}

