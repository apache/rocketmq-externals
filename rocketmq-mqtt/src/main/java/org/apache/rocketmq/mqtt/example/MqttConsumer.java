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

package org.apache.rocketmq.mqtt.example;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.paho.client.mqttv3.MqttConnectOptions.MQTT_VERSION_3_1_1;

public class MqttConsumer {

    private static Logger log = LoggerFactory.getLogger(MqttConsumer.class);

    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeyException, InterruptedException {
        String subscriptionTopic = "test_topic_0921/a";
        String broker = "tcp://localhost:1883";
        String clientId = "consumer_from_desktop";
        int qosLevel = 1;
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setKeepAliveInterval(3000000);
        mqttConnectOptions.setAutomaticReconnect(false);
        mqttConnectOptions.setMqttVersion(MQTT_VERSION_3_1_1);
        mqttConnectOptions.setConnectionTimeout(100);
        MemoryPersistence persistence = new MemoryPersistence();

        try {
            MqttAsyncClient sampleClient = new MqttAsyncClient(broker, clientId, persistence);
            sampleClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    /**
                     * 客户端连接成功
                     */
                    System.out.println("connect success");

                    final String topicFilter[] = {subscriptionTopic};
                    final int[] qos = {qosLevel};
                    try {
                        sampleClient.subscribe(topicFilter, qos);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void connectionLost(Throwable throwable) {
                    System.out.println(throwable.getMessage());
                    throwable.printStackTrace();
                }

                @Override
                public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                    System.out.println(
                            "receive msg from topic " + s + " , body is " + new String(mqttMessage.getPayload()) + "packetId is " + mqttMessage.getId());
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                    System.out.println("send msg succeed topic is : " + iMqttDeliveryToken.getTopics()[0]);
                }
            });

            System.out.println("Connecting to broker: " + broker);
            sampleClient.connect(mqttConnectOptions).waitForCompletion();
            Thread.sleep(Integer.MAX_VALUE);
        } catch (MqttException me) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
            System.exit(1);
        }
    }
}
