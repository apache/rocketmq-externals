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

package org.apache.rocketmq.iot.protocol.mqtt.data;

public class Subscription {

    private String id;

    private MqttClient client;

    private int qos;


    public MqttClient getClient() {
        return client;
    }

    public int getQos() {
        return qos;
    }

    public String getId() {
        return id;
    }

    public static class Builder {
        Subscription subscription = new Subscription();

        public Subscription build() {
            return subscription;
        }

        public Builder client(MqttClient client) {
            subscription.client = client;
            subscription.id = client.getId();
            return this;
        }
        public Builder qos(int qos) {
            subscription.qos = qos;
            return this;
        }
        public static Builder newBuilder() {
            return new Builder();
        }
    }
}
