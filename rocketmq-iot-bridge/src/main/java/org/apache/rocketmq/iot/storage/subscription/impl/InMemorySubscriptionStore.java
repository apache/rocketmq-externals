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

package org.apache.rocketmq.iot.storage.subscription.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.rocketmq.iot.connection.client.Client;
import org.apache.rocketmq.iot.protocol.mqtt.constant.MqttConstant;
import org.apache.rocketmq.iot.protocol.mqtt.data.Subscription;
import org.apache.rocketmq.iot.storage.subscription.SubscriptionStore;

public class InMemorySubscriptionStore implements SubscriptionStore {

    private ConcurrentHashMap<String, Set<String>> rootTopic2Topics = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Set<Subscription>> topic2Subscriptions = new ConcurrentHashMap<>();
    private Map<String, Set<String>> clientId2TopicFilters = new HashMap<>();

    /**
     * get the id list of the clients which subscribe to the topic
     *
     * @param topic
     * @return id list of the clients which subscribe to the topic
     */
    @Override public List<Subscription> get(String topic) {
        if (!topic2Subscriptions.containsKey(topic)) {
            return Collections.emptyList();
        }
        return topic2Subscriptions.get(topic).stream().collect(Collectors.toList());
    }

    /**
     * check if a topic exists or not
     *
     * @param topic
     * @return
     */
    @Override public boolean hasTopic(String topic) {
        return topic2Subscriptions.containsKey(topic);
    }

    /**
     * add a new topic to existing subscriptions
     *
     * @param topic the actual name topic instead of topicFilter
     */
    @Override public void addTopic(String topic) {
        if (topic2Subscriptions.containsKey(topic)) {
            return;
        }
        if (topic.contains(MqttConstant.SUBSCRIPTION_FLAG_PLUS) || topic.contains(MqttConstant.SUBSCRIPTION_FLAG_SHARP)) {
            return;
        }
        topic2Subscriptions.put(topic, new HashSet<>());
        String rootTopic = getRootTopic(topic);
        if (!rootTopic2Topics.containsKey(rootTopic)) {
            rootTopic2Topics.put(rootTopic, new HashSet<>());
        }
        rootTopic2Topics.get(rootTopic).add(topic);
    }

    /**
     * append the client to the topic
     *
     * @param topic
     * @param subscription
     * @return the subscription list of the client
     */
    @Override public void append(String topic, Subscription subscription) {
        clientId2TopicFilters.putIfAbsent(subscription.getClient().getId(), new HashSet<>());
        clientId2TopicFilters.get(subscription.getClient().getId()).add(topic);
        if (!topic.contains(MqttConstant.SUBSCRIPTION_FLAG_PLUS) && !topic.contains(MqttConstant.SUBSCRIPTION_FLAG_SHARP)) {
            String rootTopic = getRootTopic(topic);
            if (!topic2Subscriptions.containsKey(topic)) {
                addTopic(topic);
            }
            if (subscription == null) {
                return;
            }
            synchronized (this) {
                rootTopic2Topics.get(rootTopic).add(topic);
                topic2Subscriptions.get(topic).add(subscription);
            }
        } else {
            String rootTopic = getRootTopic(topic);
            if (rootTopic2Topics.containsKey(rootTopic)) {
                rootTopic2Topics.get(rootTopic)
                    .stream()
                    .filter(t -> match(topic, t))
                    .forEach(t -> append(t, subscription)
                    );
            }

        }
    }

    private String getRootTopic(String topic) {
        return topic.split(MqttConstant.SUBSCRIPTION_SEPARATOR)[0];
    }

    /**
     * remote the client from the topic
     *
     * @param topic
     * @param client
     */
    @Override public void remove(String topic, Client client) {
        if (topic2Subscriptions.containsKey(topic)) {
            Set<Subscription> subscriptions = topic2Subscriptions.get(topic);
            if (subscriptions == null) {
                return;
            }
            synchronized (subscriptions) {
                for (Iterator<Subscription> iter = subscriptions.iterator(); iter.hasNext(); ) {
                    Subscription subscription = iter.next();
                    if (subscription.getClient().getId().equals(client.getId())) {
                        iter.remove();
                    }
                }
            }
        }
    }

    /**
     * get the topics which match the filter
     *
     * @param filter@return matched topics
     */
    @Override public List<String> getTopics(String filter) {
        String rootTopic = getRootTopic(filter);
        if (!rootTopic2Topics.containsKey(rootTopic)) {
            return Collections.emptyList();
        }
        return rootTopic2Topics.get(rootTopic).stream().filter(t -> match(filter, t)).collect(Collectors.toList());
    }

    @Override public Set<String> getTopicFilters(String clientId) {
        return clientId2TopicFilters.getOrDefault(clientId, Collections.emptySet());
    }

    private boolean match(String filter, String topic) {
        if (!filter.contains(MqttConstant.SUBSCRIPTION_FLAG_PLUS) && !filter.contains(MqttConstant.SUBSCRIPTION_FLAG_SHARP)) {
            return filter.equals(topic);
        }
        String[] filterTopics = filter.split(MqttConstant.SUBSCRIPTION_SEPARATOR);
        String[] actualTopics = topic.split(MqttConstant.SUBSCRIPTION_SEPARATOR);

        int i = 0;
        for (; i < filterTopics.length && i < actualTopics.length; i++) {
            if (MqttConstant.SUBSCRIPTION_FLAG_PLUS.equals(filterTopics[i])) {
                continue;
            }
            if (MqttConstant.SUBSCRIPTION_FLAG_SHARP.equals(filterTopics[i])) {
                return true;
            }
            if (!filterTopics[i].equals(actualTopics[i])) {
                return false;
            }
        }
        return i == actualTopics.length;
    }

    /**
     * start the Session Store
     */
    @Override public void start() {

    }

    /**
     * shutdown the Session Store
     */
    @Override public void shutdown() {

    }
}