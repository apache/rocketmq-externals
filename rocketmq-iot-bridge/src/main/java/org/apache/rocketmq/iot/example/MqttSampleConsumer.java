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

package org.apache.rocketmq.iot.example;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttSampleConsumer {

    private static Logger log = LoggerFactory.getLogger(MqttSampleConsumer.class);

    public static void main(String[] args) {
        String topic = "mqtt-sample";
        int qos = 0;
        String broker = "tcp://127.0.0.1:1883";
        String clinetId = "JavaSampleConsumer";

        MemoryPersistence persistence = new MemoryPersistence();

        {
            try {
                MqttClient sampleClient = new MqttClient(broker, clinetId, persistence);
                MqttConnectOptions connectOptions = new MqttConnectOptions();
                connectOptions.setCleanSession(true);
                log.info("Connecting to broker: " + broker);
                sampleClient.connect(connectOptions);
                log.info("Connected");
                sampleClient.setCallback(new MqttCallback() {
                    @Override public void connectionLost(Throwable throwable) {

                    }

                    @Override public void messageArrived(String s, MqttMessage message) throws Exception {
                        System.exit(0);
                    }

                    @Override public void deliveryComplete(IMqttDeliveryToken token) {

                    }
                });
                sampleClient.subscribe(topic, qos);
            } catch (MqttException me) {
                log.error("reason " + me.getReasonCode());
                log.error("msg " + me.getMessage());
                log.error("loc " + me.getLocalizedMessage());
                log.error("cause " + me.getCause());
                log.error("excep " + me);
                me.printStackTrace();
                System.exit(1);
            }
        }
    }

}
