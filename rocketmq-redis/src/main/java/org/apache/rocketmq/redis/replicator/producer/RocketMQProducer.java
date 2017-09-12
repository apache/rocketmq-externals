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

import java.util.List;
import java.util.Objects;
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
import org.apache.rocketmq.redis.replicator.rdb.datatype.KeyValuePair;
import org.apache.rocketmq.remoting.exception.RemotingException;

import static com.alibaba.fastjson.JSON.toJSONBytes;
import static com.alibaba.fastjson.serializer.SerializerFeature.IgnoreNonFieldGetter;
import static java.lang.String.valueOf;
import static org.apache.rocketmq.redis.replicator.conf.ReplicatorConstants.ORDER_MODEL;
import static org.apache.rocketmq.redis.replicator.conf.ReplicatorConstants.ORDER_MODEL_GLOBAL;
import static org.apache.rocketmq.redis.replicator.conf.ReplicatorConstants.ROCKETMQ_DATA_TOPIC;
import static org.apache.rocketmq.redis.replicator.conf.ReplicatorConstants.ROCKETMQ_NAMESERVER_ADDRESS;
import static org.apache.rocketmq.redis.replicator.conf.ReplicatorConstants.ROCKETMQ_PRODUCER_GROUP_NAME;
import static org.apache.rocketmq.redis.replicator.conf.ReplicatorConstants.ROCKETMQ_PRODUCER_INSTANCE_NAME;

public class RocketMQProducer {

    private String topic;
    private boolean global;
    private MQProducer producer;
    private List<MessageQueue> messageQueues;

    public RocketMQProducer(Configure configure) throws MQClientException {
        Objects.requireNonNull(configure);
        this.topic = configure.getString(ROCKETMQ_DATA_TOPIC);
        this.global = configure.getString(ORDER_MODEL, ORDER_MODEL_GLOBAL, true).equals(ORDER_MODEL_GLOBAL);
        DefaultMQProducer producer = new DefaultMQProducer();
        producer.setNamesrvAddr(configure.getString(ROCKETMQ_NAMESERVER_ADDRESS));
        producer.setProducerGroup(configure.getString(ROCKETMQ_PRODUCER_GROUP_NAME));
        producer.setInstanceName(configure.getString(ROCKETMQ_PRODUCER_INSTANCE_NAME));
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
        Message msg = new Message(this.topic, toJSONBytes(command, IgnoreNonFieldGetter));
        SendResult rs = this.producer.send(msg, this.messageQueues.get(0));
        return rs.getSendStatus() == SendStatus.SEND_OK;
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
        Message msg = new Message(this.topic, valueOf(kv.getDb().getDbNumber()), toJSONBytes(kv, IgnoreNonFieldGetter));

        SendResult rs = this.producer.send(msg, this.messageQueues.get(0));
        return rs.getSendStatus() == SendStatus.SEND_OK;
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
        Message msg = new Message(this.topic, valueOf(kv.getDb().getDbNumber()), toJSONBytes(kv));
        SendResult rs = this.producer.send(msg, this.messageQueues.get(index));
        return rs.getSendStatus() == SendStatus.SEND_OK;
    }
}
