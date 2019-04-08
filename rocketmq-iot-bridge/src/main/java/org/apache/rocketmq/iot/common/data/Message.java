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

package org.apache.rocketmq.iot.common.data;

import java.util.Map;
import org.apache.rocketmq.iot.connection.client.Client;

public class Message {

    public enum Type {
        MQTT_CONNECT, /* the MQTT CONNECT message from the client */
        MQTT_CONNACK, /* the MQTT CONNACK message from the client */
        MQTT_PUBLISH,/* the MQTT PUBLISH message from the client */
        MQTT_PUBACK,/* the MQTT PUBACK message from the client */
        MQTT_PUBREC,/* the MQTT PUBREC message from the client */
        MQTT_PUBREL,/* the MQTT PUBREL message from the client */
        MQTT_PUBCOMP,/* the MQTT PUBCOMP message from the client */
        MQTT_SUBSCRIBE,/* the MQTT SUBSCRIBE message from the client */
        MQTT_SUBACK,/* the MQTT SUBACK message from the client */
        MQTT_UNSUBSCRIBE,/* the MQTT UNSUBSCRIBE message from the client */
        MQTT_UNSUBACK,/* the MQTT UNSUBACK message from the client */
        MQTT_PINGREQ,/* the MQTT PINGREQ message from the client */
        MQTT_PINGRESP,/* the MQTT PINGRESP message will be sent to the client */
        MQTT_DISCONNECT,/* the MQTT DISCONNECT message from the client */
        MQTT_SEND,/* the MQTT PUBLISH message will be sent to the client */
    }

    private String id;
    private Map<String, String> headers;
    private Type type;
    private Object payload;
    private Client client;
    private String topic;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
