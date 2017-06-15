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

package org.apache.rocketmq.console.service;

import org.apache.rocketmq.common.protocol.body.ConsumerConnection;
import org.apache.rocketmq.common.protocol.body.ConsumerRunningInfo;
import org.apache.rocketmq.console.model.ConsumerGroupRollBackStat;
import org.apache.rocketmq.console.model.GroupConsumeInfo;
import org.apache.rocketmq.console.model.TopicConsumerInfo;
import org.apache.rocketmq.console.model.request.ConsumerConfigInfo;
import org.apache.rocketmq.console.model.request.DeleteSubGroupRequest;
import org.apache.rocketmq.console.model.request.ResetOffsetRequest;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ConsumerService {
    List<GroupConsumeInfo> queryGroupList();

    GroupConsumeInfo queryGroup(String consumerGroup);


    List<TopicConsumerInfo> queryConsumeStatsListByGroupName(String groupName);

    List<TopicConsumerInfo> queryConsumeStatsList(String topic, String groupName);

    Map<String, TopicConsumerInfo> queryConsumeStatsListByTopicName(String topic);

    Map<String /*consumerGroup*/, ConsumerGroupRollBackStat> resetOffset(ResetOffsetRequest resetOffsetRequest);

    List<ConsumerConfigInfo> examineSubscriptionGroupConfig(String group);

    boolean deleteSubGroup(DeleteSubGroupRequest deleteSubGroupRequest);

    boolean createAndUpdateSubscriptionGroupConfig(ConsumerConfigInfo consumerConfigInfo);

    Set<String> fetchBrokerNameSetBySubscriptionGroup(String group);

    ConsumerConnection getConsumerConnection(String consumerGroup);

    ConsumerRunningInfo getConsumerRunningInfo(String consumerGroup, String clientId, boolean jstack);
}
