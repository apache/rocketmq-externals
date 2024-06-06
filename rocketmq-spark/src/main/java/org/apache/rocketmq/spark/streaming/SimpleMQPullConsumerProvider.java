/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.spark.streaming;

import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.consumer.store.OffsetStore;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingException;

import java.util.Map;
import java.util.Set;

/**
 * Simple mq pull consumer provider which implemented by DefaultMQPullConsumer
 */
public class SimpleMQPullConsumerProvider implements MQPullConsumerProvider {
    private DefaultMQPullConsumer consumer;

    public SimpleMQPullConsumerProvider() {
        consumer = new DefaultMQPullConsumer();
    }

    @Override
    public void setConsumerGroup(String consumerGroup) {
        consumer.setConsumerGroup(consumerGroup);
    }

    @Override
    public void setOptionParams(Map<String, String> optionParams) {
        // ignore
    }

    @Override
    public void setConsumerTimeoutMillisWhenSuspend(long consumerTimeoutMillisWhenSuspend) {
        consumer.setConsumerTimeoutMillisWhenSuspend(consumerTimeoutMillisWhenSuspend);
    }

    @Override
    public void setInstanceName(String instanceName) {
        consumer.setInstanceName(instanceName);
    }

    @Override
    public void setNamesrvAddr(String namesrvAddr) {
        consumer.setNamesrvAddr(namesrvAddr);
    }

    @Override
    public void start() throws MQClientException {
        consumer.start();
    }

    @Override
    public void setOffsetStore(OffsetStore offsetStore) {
        consumer.setOffsetStore(offsetStore);
    }

    @Override
    public OffsetStore getOffsetStore() {
        return consumer.getDefaultMQPullConsumerImpl().getOffsetStore();
    }

    @Override
    public PullResult pull(MessageQueue mq, String subExpression, long offset, int maxNums)
            throws MQBrokerException, RemotingException, InterruptedException, MQClientException {
        return consumer.pull(mq, subExpression, offset, maxNums);
    }

    @Override
    public Set<MessageQueue> fetchSubscribeMessageQueues(String topic) throws MQClientException {
        return consumer.fetchSubscribeMessageQueues(topic);
    }

    @Override
    public long minOffset(MessageQueue mq) throws MQClientException {
        return consumer.minOffset(mq);
    }

    @Override
    public long maxOffset(MessageQueue mq) throws MQClientException {
        return consumer.maxOffset(mq);
    }

    @Override
    public void commitConsumeOffset(MessageQueue mq, long offset) throws MQClientException {
        consumer.updateConsumeOffset(mq, offset);
    }

    @Override
    public void shutdown() {
        consumer.shutdown();
    }
}
