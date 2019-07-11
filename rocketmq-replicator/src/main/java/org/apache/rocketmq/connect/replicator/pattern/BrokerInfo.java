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

package org.apache.rocketmq.connect.replicator.pattern;

import java.util.List;
import java.util.Properties;
import org.apache.rocketmq.common.protocol.body.ConsumerConnection;
import org.apache.rocketmq.common.protocol.body.ConsumerOffsetSerializeWrapper;
import org.apache.rocketmq.common.protocol.body.SubscriptionGroupWrapper;
import org.apache.rocketmq.common.protocol.body.TopicConfigSerializeWrapper;

public class BrokerInfo {
    Properties brokerConfig;

    TopicConfigSerializeWrapper topicConfig;

    ConsumerOffsetSerializeWrapper consumerOffsetSerializeWrapper;

    String deleayOffset;

    SubscriptionGroupWrapper subscriptionGroupWrapper;

    List<ConsumerConnection> consumerConnections;

    public Properties getBrokerConfig() {
        return brokerConfig;
    }

    public void setBrokerConfig(Properties brokerConfig) {
        this.brokerConfig = brokerConfig;
    }

    public TopicConfigSerializeWrapper getTopicConfig() {
        return topicConfig;
    }

    public void setTopicConfig(TopicConfigSerializeWrapper topicConfig) {
        this.topicConfig = topicConfig;
    }

    public ConsumerOffsetSerializeWrapper getConsumerOffsetSerializeWrapper() {
        return consumerOffsetSerializeWrapper;
    }

    public void setConsumerOffsetSerializeWrapper(ConsumerOffsetSerializeWrapper consumerOffsetSerializeWrapper) {
        this.consumerOffsetSerializeWrapper = consumerOffsetSerializeWrapper;
    }

    public String getDeleayOffset() {
        return deleayOffset;
    }

    public void setDeleayOffset(String deleayOffset) {
        this.deleayOffset = deleayOffset;
    }

    public SubscriptionGroupWrapper getSubscriptionGroupWrapper() {
        return subscriptionGroupWrapper;
    }

    public void setSubscriptionGroupWrapper(SubscriptionGroupWrapper subscriptionGroupWrapper) {
        this.subscriptionGroupWrapper = subscriptionGroupWrapper;
    }

    public List<ConsumerConnection> getConsumerConnections() {
        return consumerConnections;
    }

    public void setConsumerConnections(List<ConsumerConnection> consumerConnections) {
        this.consumerConnections = consumerConnections;
    }

    @Override
    public String toString() {
        return "BrokerInfo [brokerConfig=" + brokerConfig + ", topicConfig=" + topicConfig
            + ", consumerOffsetSerializeWrapper=" + consumerOffsetSerializeWrapper + ", deleayOffset="
            + deleayOffset + ", subscriptionGroupWrapper=" + subscriptionGroupWrapper + ", consumerConnections="
            + consumerConnections + "]";
    }

}
