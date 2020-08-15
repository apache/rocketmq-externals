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

package org.apache.rocketmq.iot.storage.subscription;

import java.util.List;
import java.util.Set;
import org.apache.rocketmq.iot.connection.client.Client;
import org.apache.rocketmq.iot.protocol.mqtt.data.Subscription;

public interface SubscriptionStore {
    /**
     * Get the id list of the subscriptions which subscribe to the topic
     * @param topic
     * @return id list of the subscriptions which subscribe to the topic
     */
    List<Subscription> get(String topic);

    /**
     * Check if a topic exists or not
     * @param topic
     * @return
     */
    boolean hasTopic(String topic);

    /**
     * Add a new topic to existing subscriptions
     * @param topic the actual name topic instead of topicFilter
     */
    void addTopic(String topic);

    /**
     * Append the client to the topic
     * @param topic the topic to which the client subscribes
     * @param subscription the subscription of the client
     * @return the subscription list of the client
     */
    void append(String topic, Subscription subscription);

    /**
     * Remove the subscription of a client from the topic
     */
    void remove(String topic, Client client);

    /**
     * Get the topics which match the filter
     * @param filter the topic filter which contains wildcards ('+' and '#')
     * @return matched topics
     */
    List<String> getTopics(String filter);

    /**
     * Get the topicFilters which are provided by the client when subscribed
     * @param clientId the identifier of the client
     * @return the topicFilters
     */
    Set<String> getTopicFilters(String clientId);

    /**
     * Start the Session Store
     */
    void start();
    /**
     * Shutdown the Session Store
     */
    void shutdown();
}
