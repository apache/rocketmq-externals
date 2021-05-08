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

package org.apache.rocketmq.iot.canary;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.rocketmq.iot.canary.config.CanaryConfig;
import org.apache.rocketmq.iot.canary.service.AvailabilityResult;
import org.apache.rocketmq.iot.canary.service.AvailabilityService;
import org.apache.rocketmq.iot.canary.util.FalconOutput;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class FalconDataServiceTest {
    private static final double delay = 3.0;
    private AvailabilityService availabilityService;

    @Before
    public void init() throws Exception {
        CanaryConfig canaryConfig = new CanaryConfig(new Properties());
        availabilityService = new AvailabilityService(canaryConfig);

        // publisherClient
        MqttClient publisherClient = Mockito.mock(MqttClient.class);
        when(publisherClient.isConnected()).thenReturn(true);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Exception {
                // field publisherAvai
                AvailabilityResult publisherAvai = new AvailabilityResult();
                Field fieldPublisherAvai = AvailabilityService.class.getDeclaredField("publisherAvai");
                fieldPublisherAvai.setAccessible(true);
                fieldPublisherAvai.set(availabilityService, publisherAvai);

                // field subscriberAvai
                AvailabilityResult subscriberAvai = new AvailabilityResult();
                Field fieldSubscriberAvai = AvailabilityService.class.getDeclaredField("subscriberAvai");
                fieldSubscriberAvai.setAccessible(true);
                fieldSubscriberAvai.set(availabilityService, subscriberAvai);

                // field totalAvai
                AvailabilityResult totalAvai = new AvailabilityResult();
                Field fieldTotalAvai = AvailabilityService.class.getDeclaredField("totalAvai");
                fieldTotalAvai.setAccessible(true);
                fieldTotalAvai.set(availabilityService, totalAvai);

                // field latencyList
                List<Double> latencyList = new ArrayList<>();
                Field fieldLatencyList = AvailabilityService.class.getDeclaredField("latencyList");
                fieldLatencyList.setAccessible(true);
                fieldLatencyList.set(availabilityService, latencyList);

                for (int i = 0; i < 10; i++) {
                    publisherAvai.addSuccess();
                    subscriberAvai.addSuccess();
                    totalAvai.addSuccess();
                    latencyList.add(delay);
                }
                return null;
            }
        }).when(publisherClient).publish(anyString(), any(MqttMessage.class));
        Field fieldPublisherClient = AvailabilityService.class.getDeclaredField("publisherMqttClient");
        fieldPublisherClient.setAccessible(true);
        fieldPublisherClient.set(availabilityService, publisherClient);

        // falconOutput
        FalconOutput falconOutput = Mockito.mock(FalconOutput.class);
        doNothing().when(falconOutput).pushFalconData(anyDouble(), anyString(), anyString());
        Field fieldFalconOutput = AvailabilityService.class.getDeclaredField("falconOutput");
        fieldFalconOutput.setAccessible(true);
        fieldFalconOutput.set(availabilityService, falconOutput);
    }

    @Test
    public void testAvailability() throws Exception {
        availabilityService.publishMessages();
        availabilityService.pushAvailabilityData();

        Field fieldRatio = AvailabilityResult.class.getDeclaredField("ratio");
        fieldRatio.setAccessible(true);

        // publisher availability
        Field fieldPublisherAvai = AvailabilityService.class.getDeclaredField("publisherAvai");
        fieldPublisherAvai.setAccessible(true);
        AvailabilityResult publisherAvai = (AvailabilityResult) fieldPublisherAvai.get(availabilityService);
        double ratio = (double) fieldRatio.get(publisherAvai);
        Assert.assertTrue(ratio == 1.0);

        // subscriber availability
        Field fieldSubscriberAvai = AvailabilityService.class.getDeclaredField("subscriberAvai");
        fieldSubscriberAvai.setAccessible(true);
        AvailabilityResult subscriberAvai = (AvailabilityResult) fieldSubscriberAvai.get(availabilityService);
        ratio = (double) fieldRatio.get(subscriberAvai);
        Assert.assertTrue(ratio == 1.0);

        // total availability
        Field fieldTotalAvai = AvailabilityService.class.getDeclaredField("totalAvai");
        fieldTotalAvai.setAccessible(true);
        AvailabilityResult totalAvai = (AvailabilityResult) fieldTotalAvai.get(availabilityService);
        ratio = (double) fieldRatio.get(totalAvai);
        Assert.assertTrue(ratio == 1.0);
    }

    @Test
    public void testPushDelayData() throws Exception {
        availabilityService.publishMessages();
        double eteLatency = availabilityService.getETELatency();
        Assert.assertTrue(eteLatency == delay);
        availabilityService.pushDelayData();
        eteLatency = availabilityService.getETELatency();
        Assert.assertTrue(eteLatency == 0.0);
    }

    @After
    public void tearDown() throws Exception {
        availabilityService.shutdown();
    }
}
