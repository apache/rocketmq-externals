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

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.rocketmq.iot.protocol.mqtt.constant.MqttConstant;
import org.apache.rocketmq.iot.protocol.mqtt.data.MqttClient;
import org.apache.rocketmq.iot.protocol.mqtt.data.Subscription;
import org.apache.rocketmq.iot.storage.subscription.impl.InMemorySubscriptionStore;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class InMemorySubscriptionTest {

    private InMemorySubscriptionStore subscriptionStore = new InMemorySubscriptionStore();
    private String topic1;
    private String topic2;
    private String topic3;
    private MqttClient client1;
    private MqttClient client2;
    private Subscription subscription1;
    private Subscription subscription2;
    private Map<String, Set<String>> rootTopic2Topics;

    @Before
    public void setup() throws IllegalAccessException {
        subscriptionStore = new InMemorySubscriptionStore();
        topic1 = "test/in/memory/topic-a";
        topic2 = "test/in/memory/topic-b";
        topic3 = "test/in/disk/topic-c";
        client1 = Mockito.spy(new MqttClient());
        client2 = Mockito.spy(new MqttClient());
        client1.setId("test-client-1");
        client2.setId("test-client-2");
        subscription1 = Subscription.Builder.newBuilder().client(client1).build();
        subscription2 = Subscription.Builder.newBuilder().client(client2).build();
        subscriptionStore.addTopic(topic1);
        subscriptionStore.addTopic(topic2);
        subscriptionStore.addTopic(topic3);
    }

    @Test
    public void testAppend() {
        /* test append with topicFilter without wildcard */
        String topicFilter1 = "test/in/memory/topic-a"; // equals to topic1
        subscriptionStore.append(topicFilter1, subscription1);
        subscriptionStore.append(topicFilter1, subscription2);

        List<Subscription> subscriptions1 = subscriptionStore.get(topic1);
        List<Subscription> subscriptions2 = subscriptionStore.get(topic2);
        List<Subscription> subscriptions3 = subscriptionStore.get(topic3);
        String rootTopic1 = topic1.split(MqttConstant.SUBSCRIPTION_SEPARATOR)[0];
        Assert.assertTrue(inList(subscriptions1, subscription1));
        Assert.assertTrue(inList(subscriptions1, subscription2));
        Assert.assertFalse(inList(subscriptions2, subscription1));
        Assert.assertFalse(inList(subscriptions2, subscription2));
        Assert.assertFalse(inList(subscriptions3, subscription1));
        Assert.assertFalse(inList(subscriptions3, subscription2));
        subscriptionStore.remove(topic1, client1);
        subscriptionStore.remove(topic1, client2);

        /* test append with topicFilter with wildcard */
        /* option 1: + wildcard */
        String topicFilter2 = "test/in/memory/+";
        subscriptionStore.append(topicFilter2, subscription1);
        subscriptionStore.append(topicFilter2, subscription2);

        subscriptions1 = subscriptionStore.get(topic1);
        subscriptions2 = subscriptionStore.get(topic2);
        subscriptions3 = subscriptionStore.get(topic3);
        Assert.assertTrue(inList(subscriptions1, subscription1));
        Assert.assertTrue(inList(subscriptions1, subscription2));
        Assert.assertTrue(inList(subscriptions2, subscription1));
        Assert.assertTrue(inList(subscriptions2, subscription2));
        Assert.assertFalse(inList(subscriptions3, subscription1));
        Assert.assertFalse(inList(subscriptions3, subscription2));
        subscriptionStore.remove(topic1, client1);
        subscriptionStore.remove(topic1, client2);
        subscriptionStore.remove(topic2, client1);
        subscriptionStore.remove(topic2, client2);

        /* option 2: # wildcard */
        String topicFilter3 = "test/in/#";
        subscriptionStore.append(topicFilter3, subscription1);
        subscriptionStore.append(topicFilter3, subscription2);
        subscriptions1 = subscriptionStore.get(topic1);
        subscriptions2 = subscriptionStore.get(topic2);
        subscriptions3 = subscriptionStore.get(topic3);
        Assert.assertTrue(inList(subscriptions1, subscription1));
        Assert.assertTrue(inList(subscriptions1, subscription2));
        Assert.assertTrue(inList(subscriptions2, subscription1));
        Assert.assertTrue(inList(subscriptions2, subscription2));
        Assert.assertTrue(inList(subscriptions3, subscription1));
        Assert.assertTrue(inList(subscriptions3, subscription2));
        subscriptionStore.remove(topic1, client1);
        subscriptionStore.remove(topic1, client2);
        subscriptionStore.remove(topic2, client1);
        subscriptionStore.remove(topic2, client2);
        subscriptionStore.remove(topic2, client1);
        subscriptionStore.remove(topic2, client2);

        /* test re-append */
        subscriptionStore.append(topic1, subscription1);
        subscriptionStore.append(topic1, subscription1);
        subscriptionStore.append(topic1, subscription1);

        subscriptions1 = subscriptionStore.get(topic1);
        Assert.assertTrue(inList(subscriptions1, subscription1));
        /* re-append should not include multiple subscription for same client of same topic */
        Assert.assertTrue(subscriptions1.size() == 1);

    }

    @Test
    public void testRemove() {
        subscriptionStore.append(topic1, subscription1);
        subscriptionStore.append(topic1, subscription2);
        subscriptionStore.append(topic2, subscription1);
        subscriptionStore.append(topic2, subscription2);

        subscriptionStore.remove(topic1, client1);
        Assert.assertTrue(!inList(subscriptionStore.get(topic1), subscription1));
        Assert.assertTrue(inList(subscriptionStore.get(topic1), subscription2));
        Assert.assertTrue(!subscriptionStore.get(topic1).isEmpty());
        Assert.assertTrue(inList(subscriptionStore.get(topic2), subscription1));
        Assert.assertTrue(inList(subscriptionStore.get(topic2), subscription2));
        Assert.assertTrue(!subscriptionStore.get(topic2).isEmpty());

        subscriptionStore.remove(topic1, client2);
        Assert.assertTrue(!inList(subscriptionStore.get(topic1), subscription1));
        Assert.assertTrue(!inList(subscriptionStore.get(topic1), subscription2));
        Assert.assertTrue(subscriptionStore.get(topic1).isEmpty());
        Assert.assertTrue(inList(subscriptionStore.get(topic2), subscription1));
        Assert.assertTrue(inList(subscriptionStore.get(topic2), subscription2));
        Assert.assertTrue(!subscriptionStore.get(topic2).isEmpty());

        subscriptionStore.remove(topic2, client1);
        Assert.assertTrue(!inList(subscriptionStore.get(topic1), subscription1));
        Assert.assertTrue(!inList(subscriptionStore.get(topic1), subscription2));
        Assert.assertTrue(subscriptionStore.get(topic1).isEmpty());
        Assert.assertTrue(!inList(subscriptionStore.get(topic2), subscription1));
        Assert.assertTrue(inList(subscriptionStore.get(topic2), subscription2));
        Assert.assertTrue(!subscriptionStore.get(topic2).isEmpty());

        subscriptionStore.remove(topic2, client2);
        Assert.assertTrue(!inList(subscriptionStore.get(topic1), subscription1));
        Assert.assertTrue(!inList(subscriptionStore.get(topic1), subscription2));
        Assert.assertTrue(subscriptionStore.get(topic1).isEmpty());
        Assert.assertTrue(!inList(subscriptionStore.get(topic2), subscription1));
        Assert.assertTrue(!inList(subscriptionStore.get(topic2), subscription2));
        Assert.assertTrue(subscriptionStore.get(topic1).isEmpty());
    }

    @Test
    public void testAddTopic() throws IllegalAccessException {
        String topic = "test-topic";
        String topicFilter = "test/filter/+";
        subscriptionStore.addTopic(topic);
        subscriptionStore.addTopic(topicFilter);
        Map<String, Set<Subscription>> topic2Subscriptions = (Map<String, Set<Subscription>>) FieldUtils.getField(InMemorySubscriptionStore.class, "topic2Subscriptions", true).get(subscriptionStore);
        Assert.assertTrue(topic2Subscriptions.containsKey(topic));
        /* topicFilter should be ignored */
        Assert.assertFalse(topic2Subscriptions.containsKey(topicFilter));
    }

    private <E> boolean inList(List<E> actualLsit, E expected) {
        if (actualLsit == null) return false;
        for (E actual: actualLsit) {
            if (actual == expected) {
                return true;
            }
        }
        return false;
    }
}
