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

import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.consumer.store.OffsetStore;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingException;

import java.util.Map;
import java.util.Set;

/**
 * Custom pull consumer interface
 */
public interface MQPullConsumerProvider {

    /**
     * Set rocket mq consumer group.
     */
    void setConsumerGroup(String consumerGroup);

    /**
     * Set consumer timeout.
     */
    void setConsumerTimeoutMillisWhenSuspend(long consumerTimeoutMillisWhenSuspend);

    /**
     * Set consumer instance name.
     */
    void setInstanceName(String instanceName);

    /**
     * Set name server address.
     */
    void setNamesrvAddr(String namesrvAddr);

    /**
     * Set options params.
     */
    void setOptionParams(Map<String, String> optionParams);

    /**
     * Start consumer.
     */
    void start() throws MQClientException;

    /**
     * Get offset store.
     * @return
     */
    OffsetStore getOffsetStore();

    /**
     * Set offset store.
     *
     * It is called with getOffsetStore().
     */
    void setOffsetStore(OffsetStore offsetStore);

    /**
     * Pull messages from specified MessageQueue with subExpression, offset and maxNums.
     */
    PullResult pull(MessageQueue mq, String subExpression, long offset, int maxNums)
        throws MQClientException, RemotingException, MQBrokerException, InterruptedException;

    /**
     * Get all MessageQueues from the specified topic.
     */
    Set<MessageQueue> fetchSubscribeMessageQueues(String topic) throws MQClientException;

    /**
     * Get the minimum offset of the specified MessageQueue.
     */
    long minOffset(MessageQueue mq) throws MQClientException;

    /**
     * Get the maximum offset of the specified MessageQueue.
     */
    long maxOffset(MessageQueue mq) throws MQClientException;

    /**
     * Commit offset of the specified MessageQueue
     */
    void commitConsumeOffset(MessageQueue mq, long offset) throws MQClientException;

    void shutdown();
}
