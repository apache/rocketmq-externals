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

package org.apache.rocketmq.replicator;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.common.QueueMetaData;
import io.openmessaging.connector.api.data.Field;
import io.openmessaging.connector.api.data.Schema;
import io.openmessaging.connector.api.data.SinkDataEntry;
import io.openmessaging.connector.api.sink.SinkTask;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.replicator.common.ConstDefine;
import org.apache.rocketmq.replicator.common.Utils;
import org.apache.rocketmq.replicator.config.ConfigUtil;
import org.apache.rocketmq.replicator.config.TaskConfig;
import org.apache.rocketmq.replicator.schema.FieldName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RmqSinkTask extends SinkTask {

    private Logger log = LoggerFactory.getLogger(RmqSinkTask.class);
    private final String taskId;
    private final TaskConfig taskConfig;
    private DefaultMQProducer producer;
    private volatile boolean started = false;

    public RmqSinkTask() {
        this.taskConfig = new TaskConfig();
        this.taskId = Utils.createTaskId(Thread.currentThread().getName());
    }

    @Override
    public void put(Collection<SinkDataEntry> sinkDataEntries) {
        for (SinkDataEntry dataEntry : sinkDataEntries) {
            if (this.started) {
                Schema schema = dataEntry.getSchema();
                Object[] payload = dataEntry.getPayload();

                String brokerName = (String) getPayloadValue(FieldName.BROKER_NAME.getKey(), schema, payload);
                String topic = (String) getPayloadValue(FieldName.TOPIC.getKey(), schema, payload);
                Integer queueId = (Integer) getPayloadValue(FieldName.QUEUE_ID.getKey(), schema, payload);
                MessageQueue queue = new MessageQueue(topic, brokerName, queueId);

                String offsetMsgId = (String) getPayloadValue(FieldName.OFFSET_MESSAGE_ID.getKey(), schema, payload);
                String clientMsgId = (String) getPayloadValue(FieldName.CLIENT_MESSAGE_ID.getKey(), schema, payload);
                String tags = (String) getPayloadValue(FieldName.TAGS.getKey(), schema, payload);
                String keys = (String) getPayloadValue(FieldName.KEYS.getKey(), schema, payload);
                String msgBody = (String) getPayloadValue(FieldName.COMMON_MESSAGE.getKey(), schema, payload);
                Message msg = new Message();
                msg.setTopic(topic);
                msg.setBody(msgBody.getBytes(StandardCharsets.UTF_8));
                msg.setKeys(String.format("%s %s %s", offsetMsgId, clientMsgId, keys));
                msg.setTags(tags);

                try {
                    SendResult sendResult = this.producer.send(msg, queue);
                    if (sendResult.getSendStatus() != SendStatus.SEND_OK) {
                        log.error("RocketMQ replicator send error, SendStatus={}, msgId={}", sendResult.getSendStatus(), offsetMsgId);
                    }
                } catch (Exception e) {
                    log.error("RocketMQ replicator task put error, clientMsgId={}, offsetMsgId={}", clientMsgId, offsetMsgId, e);
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException("RocketMQ replicator task stop.");
            }
        }

        if (!this.started) {
            log.error("RocketMQ replicator task is not started.");
        }
    }

    private Object getPayloadValue(String fieldName, Schema schema, Object[] payload) {
        Field field = schema.getField(fieldName);
        if (field == null && payload.length > field.getIndex()) {
            return null;
        }

        return payload[field.getIndex()];
    }

    @Override
    public void commit(Map<QueueMetaData, Long> map) {
    }

    @Override
    public void start(KeyValue config) {
        ConfigUtil.load(config, this.taskConfig);
        RPCHook rpcHook = null;
        if (this.taskConfig.isTargetAclEnable()) {
            rpcHook = new AclClientRPCHook(new SessionCredentials(this.taskConfig.getTargetAccessKey(), this.taskConfig.getTargetSecretKey()));
        }
        this.producer = new DefaultMQProducer(rpcHook);
        this.producer.setProducerGroup(ConstDefine.REPLICATOR_SINK_TASK_GROUP);
        this.producer.setNamesrvAddr(this.taskConfig.getTargetRocketmq());
        this.producer.setInstanceName(Utils.createUniqIntanceName(this.taskConfig.getTargetRocketmq()));
        try {
            this.producer.start();
            this.started = true;
        } catch (MQClientException e) {
            log.error("Producer of task {} start failed.", this.taskId, e);
        }
        log.info("RocketMQ sink task started");
    }

    @Override
    public void stop() {
        if (this.started) {
            this.producer.shutdown();
            this.started = false;
        }
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }
}
