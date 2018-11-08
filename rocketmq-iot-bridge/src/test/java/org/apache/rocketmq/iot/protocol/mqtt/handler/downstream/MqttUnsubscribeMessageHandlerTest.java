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

package org.apache.rocketmq.iot.protocol.mqtt.handler.downstream;

import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribePayload;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.rocketmq.iot.common.data.Message;
import org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl.MqttUnsubscribeMessagHandler;
import org.junit.Assert;
import org.mockito.Mockito;

public class MqttUnsubscribeMessageHandlerTest extends AbstractMqttMessageHandlerTest {

    private String topicFilter1 = "test/in/memory/+";
    private String topicFilter2 = "test/in/disk/topic-c";
    private String topicFilter3 = "test-topic-filter-3";

    @Override public void setupMessage() {
        message.setType(Message.Type.MQTT_UNSUBSCRIBE);
        message.setPayload(getMqttUnsubscribeMessage());
    }

    private MqttUnsubscribeMessage getMqttUnsubscribeMessage() {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
            MqttMessageType.UNSUBSCRIBE,
            false,
            MqttQoS.AT_MOST_ONCE,
            false,
            0
        );
        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(1);

        List<String> topicFilters = new ArrayList<>();

        MqttUnsubscribePayload payload = new MqttUnsubscribePayload(topicFilters);
        return new MqttUnsubscribeMessage(
            fixedHeader,
            variableHeader,
            payload
        );
    }

    @Override public void assertConditions() {
        MqttUnsubscribeMessage unsubscribeMessage = (MqttUnsubscribeMessage) message.getPayload();
        MqttUnsubAckMessage ackMessage = embeddedChannel.readOutbound();

        Assert.assertEquals(unsubscribeMessage.variableHeader().messageId(), ackMessage.variableHeader().messageId());
    }

    @Override public void mock() {
        Set<String> mockedSubscribedTopicFiltersOfClient = new HashSet<>();
        mockedSubscribedTopicFiltersOfClient.add(topicFilter1);
        mockedSubscribedTopicFiltersOfClient.add(topicFilter2);
        mockedSubscribedTopicFiltersOfClient.add(topicFilter3);

        client.setId("test-client-id");

        Mockito.when(
            subscriptionStore.getTopicFilters(
                Mockito.anyString()
            )
        ).thenReturn(
            mockedSubscribedTopicFiltersOfClient
        );
    }

    @Override protected void initMessageHandler() {
        messageHandler = new MqttUnsubscribeMessagHandler(subscriptionStore);
    }
}
