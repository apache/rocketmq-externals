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

package org.apache.rocketmq.redis.replicator.producer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.redis.replicator.cmd.Command;
import org.apache.rocketmq.redis.replicator.conf.Configure;
import org.apache.rocketmq.redis.replicator.conf.ReplicatorConstants;
import org.apache.rocketmq.redis.replicator.rdb.datatype.KeyValuePair;
import org.apache.rocketmq.remoting.exception.RemotingException;

import java.util.List;

public class RocketMQProducer {

    private String topic;
    private boolean global;
    private MQProducer producer;
    private List<MessageQueue> messageQueues;

    public RocketMQProducer(Configure configure) throws MQClientException {
        this.topic = configure.getString(ReplicatorConstants.ROCKETMQ_DATA_TOPIC);
        this.global = configure.getString(ReplicatorConstants.ORDER_MODEL, ReplicatorConstants.ORDER_MODEL_GLOBAL, true).equals(ReplicatorConstants.ORDER_MODEL_GLOBAL);
        DefaultMQProducer producer = new DefaultMQProducer();
        producer.setNamesrvAddr(configure.getString(ReplicatorConstants.ROCKETMQ_NAMESERVER_ADDRESS));
        producer.setProducerGroup(configure.getString(ReplicatorConstants.ROCKETMQ_PRODUCER_GROUP_NAME));
        producer.setInstanceName(configure.getString(ReplicatorConstants.ROCKETMQ_PRODUCER_INSTANCE_NAME));
        this.producer = producer;
        this.producer.start();
        this.messageQueues = this.producer.fetchPublishMessageQueues(this.topic);
    }

    /**
     * Send rdb data which always is key-value pair. System command such as flushall won't be included.
     * <p>
     *
     * @param kv rdb key value pair
     * @return true if send success.
     * @throws MQClientException    MQClientException
     * @throws RemotingException    RemotingException
     * @throws InterruptedException InterruptedException
     * @throws MQBrokerException    MQBrokerException
     */
    public boolean sendKeyValuePair(
            KeyValuePair<?> kv) throws MQClientException, RemotingException, InterruptedException, MQBrokerException {
        return global ? sendGlobalOrder(kv) : sendPartialOrder(kv);
    }

    /**
     * Send realtime redis master command to RMQ. As the command may be a system command like 'flushall',
     * which does't have key. So dispatch the command to different queue may cause unexpected issue.
     * <p>
     *
     * @param command aof command
     * @return true if send success
     * @throws MQClientException    MQClientException
     * @throws RemotingException    RemotingException
     * @throws InterruptedException InterruptedException
     * @throws MQBrokerException    MQBrokerException
     */
    public boolean sendCommand(
            Command command) throws InterruptedException, RemotingException, MQClientException, MQBrokerException {
        Message message = new Message(this.topic, JSON.toJSONBytes(command, SerializerFeature.IgnoreNonFieldGetter));
        SendResult sendResult = this.producer.send(message, this.messageQueues.get(0));
        return sendResult.getSendStatus() == SendStatus.SEND_OK;
    }

    /**
     * Send rdb data to a single queue to keep global order.
     * <p>
     *
     * @param kv rdb key value pair
     * @return true if send success
     * @throws MQClientException    MQClientException
     * @throws RemotingException    RemotingException
     * @throws InterruptedException InterruptedException
     * @throws MQBrokerException    MQBrokerException
     */
    private boolean sendGlobalOrder(
            KeyValuePair<?> kv) throws InterruptedException, RemotingException, MQClientException, MQBrokerException {
        Message message = new Message(this.topic, String.valueOf(kv.getDb().getDbNumber()), JSON.toJSONBytes(kv, SerializerFeature.IgnoreNonFieldGetter));

        SendResult sendResult = this.producer.send(message, this.messageQueues.get(0));
        return sendResult.getSendStatus() == SendStatus.SEND_OK;
    }

    /**
     * Rdb contents are always key-value pair, so we could dispatch identical key to one queue,
     * to support consume concurrently and keep partial orderly.
     * <p>
     *
     * @param kv rdb key value pair
     * @return true if send success
     * @throws MQClientException    MQClientException
     * @throws RemotingException    RemotingException
     * @throws InterruptedException InterruptedException
     * @throws MQBrokerException    MQBrokerException
     */
    private boolean sendPartialOrder(
            KeyValuePair<?> kv) throws MQClientException, RemotingException, InterruptedException, MQBrokerException {
        String key = kv.getKey();
        int index = key.hashCode() % this.messageQueues.size();
        Message message = new Message(this.topic, String.valueOf(kv.getDb().getDbNumber()), JSON.toJSONBytes(kv));
        SendResult sendResult = this.producer.send(message, this.messageQueues.get(index));
        return sendResult.getSendStatus() == SendStatus.SEND_OK;
    }
}
