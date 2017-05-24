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

package org.apache.rocketmq.replicator.redis.mq;

import com.alibaba.fastjson.JSON;
import java.util.List;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.replicator.redis.ReplicatorRuntimeException;
import org.apache.rocketmq.replicator.redis.cmd.Command;
import org.apache.rocketmq.replicator.redis.conf.Configure;
import org.apache.rocketmq.replicator.redis.rdb.datatype.KeyValuePair;

import static org.apache.rocketmq.replicator.redis.conf.ReplicatorConstants.ORDER_MODEL;
import static org.apache.rocketmq.replicator.redis.conf.ReplicatorConstants.ORDER_MODEL_DEFAULT_VALUE;
import static org.apache.rocketmq.replicator.redis.conf.ReplicatorConstants.ROCKETMQ_DATA_TOPIC;
import static org.apache.rocketmq.replicator.redis.conf.ReplicatorConstants.ROCKETMQ_NAMESERVER_ADDRESS;
import static org.apache.rocketmq.replicator.redis.conf.ReplicatorConstants.ROCKETMQ_PRODUCER_GROUP_NAME;

public class RedisDataProducer {

    private DefaultMQProducer producer;
    private String topic;
    private List<MessageQueue> messageQueueList;

    public RedisDataProducer() {
        this.producer = new DefaultMQProducer(Configure.get(ROCKETMQ_PRODUCER_GROUP_NAME));
        this.producer.setNamesrvAddr(Configure.get(ROCKETMQ_NAMESERVER_ADDRESS));
        try {
            this.producer.start();
            this.topic = Configure.get(ROCKETMQ_DATA_TOPIC, true);
            this.messageQueueList = this.producer.fetchPublishMessageQueues(this.topic);
        }
        catch (MQClientException e) {
            throw new ReplicatorRuntimeException("Unable to create RocketMQ Producer instance", e);
        }

    }

    /**
     * Send rdb data which always is key-value pair. System command such as flushall won't be included.
     *
     * @param kv
     * @return
     * @throws MQClientException
     * @throws RemotingException
     * @throws InterruptedException
     * @throws MQBrokerException
     */
    public boolean sendRdbKeyValuePair(
        KeyValuePair<?> kv) throws MQClientException, RemotingException, InterruptedException, MQBrokerException {

        if (Configure.get(ORDER_MODEL, ORDER_MODEL_DEFAULT_VALUE).equals(ORDER_MODEL_DEFAULT_VALUE)) {
            return sendGlobalOrder(kv);
        }
        else {
            return sendPartialOrder(kv);
        }
    }

    /**
     * Send rdb data to a single queue to keep global order.
     *
     * @param kv
     * @return
     * @throws InterruptedException
     * @throws RemotingException
     * @throws MQClientException
     * @throws MQBrokerException
     */
    private boolean sendGlobalOrder(
        KeyValuePair<?> kv) throws InterruptedException, RemotingException, MQClientException, MQBrokerException {
        Message message = new Message(this.topic, String.valueOf(kv.getDb().getDbNumber())
            , generateMsgKey(kv.getKey()), JSON.toJSONBytes(kv));

        SendResult sendResult = this.producer.send(message, this.messageQueueList.get(0));
        return sendResult.getSendStatus() == SendStatus.SEND_OK;
    }

    /**
     * Rdb contents are always key-value pair, so we could dispatch identical key to one queue,
     * to support consume concurrently and keep partial orderly.
     *
     * @param kv
     * @return
     * @throws MQClientException
     * @throws RemotingException
     * @throws InterruptedException
     * @throws MQBrokerException
     */
    private boolean sendPartialOrder(
        KeyValuePair<?> kv) throws MQClientException, RemotingException, InterruptedException, MQBrokerException {

        String key = kv.getKey();
        int index = key.hashCode() % this.messageQueueList.size();

        Message message = new Message(this.topic, String.valueOf(kv.getDb().getDbNumber())
            , generateMsgKey(key), JSON.toJSONBytes(kv));

        SendResult sendResult = this.producer.send(message, this.messageQueueList.get(index));
        return sendResult.getSendStatus() == SendStatus.SEND_OK;
    }

    /**
     * Send realtime redis master command to RMQ. As the command may be a system command like 'flushall',
     * which does't have key. So dispatch the command to different queue may cause unexpected issue.
     *
     * @param command
     * @return
     * @throws InterruptedException
     * @throws RemotingException
     * @throws MQClientException
     * @throws MQBrokerException
     */
    public boolean sendCommand(
        Command command) throws InterruptedException, RemotingException, MQClientException, MQBrokerException {
        Message message = new Message(this.topic, JSON.toJSONBytes(command));

        SendResult sendResult = this.producer.send(message, this.messageQueueList.get(0));
        return sendResult.getSendStatus() == SendStatus.SEND_OK;
    }

    private String generateMsgKey(String key) {
        return key + System.currentTimeMillis();
    }
}
