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

import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.admin.TopicStatsTable;
import org.apache.rocketmq.common.protocol.body.GroupList;
import org.apache.rocketmq.common.protocol.body.TopicList;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.console.model.request.SendTopicMessageRequest;
import org.apache.rocketmq.console.model.request.TopicConfigInfo;

import java.util.List;

public interface TopicService {
    TopicList fetchAllTopicList(boolean skipSysProcess);

    TopicStatsTable stats(String topic);

    TopicRouteData route(String topic);

    GroupList queryTopicConsumerInfo(String topic);

    void createOrUpdate(TopicConfigInfo topicCreateOrUpdateRequest);

    TopicConfig examineTopicConfig(String topic, String brokerName);

    List<TopicConfigInfo> examineTopicConfig(String topic);

    boolean deleteTopic(String topic, String clusterName);

    boolean deleteTopic(String topic);

    boolean deleteTopicInBroker(String brokerName, String topic);

    SendResult sendTopicMessageRequest(SendTopicMessageRequest sendTopicMessageRequest);

}
