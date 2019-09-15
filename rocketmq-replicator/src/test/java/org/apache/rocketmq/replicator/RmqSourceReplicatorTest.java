/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.replicator;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.body.TopicList;
import org.apache.rocketmq.common.protocol.route.QueueData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import org.mockito.Mockito;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RmqSourceReplicatorTest {

    @Mock
    private DefaultMQAdminExt defaultMQAdminExt;


    @Test
    public void buildWildcardRoute() throws RemotingException, MQClientException, InterruptedException, NoSuchFieldException {

        RmqSourceReplicator rmqSourceReplicator = Mockito.spy(RmqSourceReplicator.class);

        TopicList topicList = new TopicList();
        Set<String> topics = new HashSet<String>();
        topics.add("topic1");
        topics.add("topic2");
        topics.add("sub-topic1-test");
        topics.add("sub-topic2-test");
        topics.add("sub-topic2-xxx");
        topics.add("sub-0");
        topics.add("test-0");
        topics.add("0-test");
        topicList.setTopicList(topics);
        when(defaultMQAdminExt.fetchAllTopicList()).thenReturn(topicList);

        TopicRouteData topicRouteData = new TopicRouteData();
        topicRouteData.setQueueDatas(Collections.<QueueData>emptyList());
        when(defaultMQAdminExt.examineTopicRouteInfo(any(String.class))).thenReturn(topicRouteData);


        Field field = RmqSourceReplicator.class.getDeclaredField("defaultMQAdminExt");
        FieldSetter.setField(rmqSourceReplicator, field, defaultMQAdminExt);

        Set<String> whiteList = new HashSet<String>();
        whiteList.add("topic1");
        whiteList.add("\\w+-test");
        rmqSourceReplicator.setWhiteList(whiteList);
        rmqSourceReplicator.buildRoute();
        Map<String, List<MessageQueue>> queues = rmqSourceReplicator.getTopicRouteMap();
        Set<String> expected = new HashSet<String>();
        expected.add("topic1");
        expected.add("0-test");
        assertThat(queues.size()).isEqualTo(expected.size());
        for (String topic : expected) {
            assertThat(queues.containsKey(topic)).isTrue();
        }
    }
}
