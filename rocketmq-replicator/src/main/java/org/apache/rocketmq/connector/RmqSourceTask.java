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
package org.apache.rocketmq.connector;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.data.*;
import io.openmessaging.connector.api.source.SourceTask;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.connector.common.Utils;
import org.apache.rocketmq.connector.config.ConfigUtil;
import org.apache.rocketmq.connector.config.DataType;
import org.apache.rocketmq.connector.config.TaskConfig;
import org.apache.rocketmq.connector.schema.FieldName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;

public class RmqSourceTask extends SourceTask {

    private static final Logger log = LoggerFactory.getLogger(RmqSourceTask.class);

    private final String taskId;
    private final TaskConfig config;
    private final DefaultMQPullConsumer consumer;

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
        this.consumer.setConsumerGroup(Utils.createGroupName(this.config.getSourceTopic()));
        this.consumer.setNamesrvAddr(this.config.getSourceRocketmq());
        try {
            this.consumer.start();
            Set<MessageQueue> mqs = consumer.fetchSubscribeMessageQueues(this.config.getSourceTopic());
            if (!this.config.getQueueId().equals("")) {
                for (MessageQueue mq: mqs) {
                    if (Integer.valueOf(this.config.getQueueId()) == mq.getQueueId()) {
                        ByteBuffer positionInfo = this.context.positionStorageReader().getPosition(
                                ByteBuffer.wrap(RmqConstants.getPartition(mq.getTopic(),
                                        mq.getBrokerName(), String.valueOf(mq.getQueueId()))
                                        .getBytes("UTF-8")));

                        if (null != positionInfo && positionInfo.array().length > 0) {
                            String positionJson = new String(positionInfo.array(), "UTF-8");
                            JSONObject jsonObject = JSONObject.parseObject(positionJson);
                            this.config.setNextPosition(jsonObject.getLong(RmqConstants.NEXT_POSITION));
                        }
                        mqOffsetMap.put(mq, this.config.getNextPosition());
                    }
                }
            } else {
                for (MessageQueue mq: mqs) {
                    ByteBuffer positionInfo = this.context.positionStorageReader().getPosition(
                            ByteBuffer.wrap(RmqConstants.getPartition(mq.getTopic(),
                                    mq.getBrokerName(), String.valueOf(mq.getQueueId()))
                                    .getBytes("UTF-8")));

                    if (null != positionInfo && positionInfo.array().length > 0) {
                        String positionJson = new String(positionInfo.array(), "UTF-8");
                        JSONObject jsonObject = JSONObject.parseObject(positionJson);
                        this.config.setNextPosition(jsonObject.getLong(RmqConstants.NEXT_POSITION));
                    }
                    mqOffsetMap.put(mq, this.config.getNextPosition());
                }
            }

        } catch (Exception e) {
            log.error("consumer of task {} start failed. ", this.taskId, e);
        }
    }

    public void stop() {
        this.consumer.shutdown();
    }

    public void pause() {

    }

    public void resume() {

    }

    private Collection<SourceDataEntry> pollCommonMessage() {

        List<SourceDataEntry> res = new ArrayList<SourceDataEntry>();

        try {
            for (MessageQueue mq : this.mqOffsetMap.keySet()) {
                PullResult pullResult = consumer.pull(mq, "*", this.mqOffsetMap.get(mq), 32);
                switch (pullResult.getPullStatus()) {
                    case FOUND: {
                        this.mqOffsetMap.put(mq, pullResult.getNextBeginOffset());
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put(RmqConstants.NEXT_POSITION, pullResult.getNextBeginOffset());

                        List<MessageExt> msgs = pullResult.getMsgFoundList();
                        for (MessageExt m : msgs) {
                            Schema schema = new Schema();
                            schema.setDataSource(this.config.getSourceRocketmq());
                            schema.setName(this.config.getSourceTopic());
                            schema.setFields(new ArrayList<Field>());
                            schema.getFields().add(new Field(0, FieldName.COMMON_MESSAGE.getKey(), FieldType.STRING));

                            DataEntryBuilder dataEntryBuilder = new DataEntryBuilder(schema);
                            dataEntryBuilder.timestamp(System.currentTimeMillis())
                                    .queue(this.config.getStoreTopic()).entryType(EntryType.CREATE);
                            dataEntryBuilder.putFiled(FieldName.COMMON_MESSAGE.getKey(), JSONObject.toJSONString(m));
                            SourceDataEntry sourceDataEntry = dataEntryBuilder.buildSourceDataEntry(
                                    ByteBuffer.wrap(RmqConstants.getPartition(this.config.getSourceTopic(),
                                            this.config.getBrokerName(),
                                            this.config.getQueueId()).getBytes("UTF-8")),
                                    ByteBuffer.wrap(jsonObject.toJSONString().getBytes("UTF-8"))
                            );
                            res.add(sourceDataEntry);
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            log.error("Rocketmq connector task poll error, current config: {}", JSON.toJSONString(config), e);
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

