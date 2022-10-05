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
 * @Description TODO
 * @Author zhaorongsheng
 * @Date 2022/10/5 21:48
 * @Version 1.0
 */
public interface MQPullConsumerProvider {

    void setConsumerGroup(String consumerGroup);

    void setOptionParams(Map<String, String> optionParams);

    void setConsumerTimeoutMillisWhenSuspend(long consumerTimeoutMillisWhenSuspend);

    void setInstanceName(String instanceName);

    void setNamesrvAddr(String namesrvAddr);

    void start() throws MQClientException;

    void setOffsetStore(OffsetStore offsetStore);

    OffsetStore getOffsetStore();

    PullResult pull(MessageQueue mq, String subExpression, long offset, int maxNums)
        throws MQClientException, RemotingException, MQBrokerException, InterruptedException;

    Set<MessageQueue> fetchSubscribeMessageQueues(String topic) throws MQClientException;

    long minOffset(MessageQueue mq) throws MQClientException;

    long maxOffset(MessageQueue mq) throws MQClientException;

    void updateConsumeOffset(MessageQueue mq, long offset) throws MQClientException;

    void shutdown();
}
