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


package org.apache.rocketmq.mqtt.common;

import java.util.Arrays;
import org.apache.rocketmq.remoting.protocol.RemotingSerializable;

public class TransferData extends RemotingSerializable {

    private String brokerName;
    private String topic;
    private Integer qos;
    private boolean isWillMessage = false;
    private byte[] body;
    private Long queueOffset;

    public TransferData() {
    }

    public TransferData(String topic, Integer qos, byte[] body) {
        this.topic = topic;
        this.qos = qos;
        this.body = body;
    }

    public TransferData(String topic, Integer qos, byte[] body, boolean isWillMessage) {
        this(topic, qos, body);
        this.isWillMessage = isWillMessage;
    }

    public TransferData(String topic, Integer qos, byte[] body, String brokerName, Long queueOffset) {
        this(topic, qos, body);
        this.brokerName = brokerName;
        this.queueOffset = queueOffset;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Integer getQos() {
        return qos;
    }

    public void setQos(Integer qos) {
        this.qos = qos;
    }

    public boolean isWillMessage() {
        return isWillMessage;
    }

    public void setWillMessage(boolean willMessage) {
        isWillMessage = willMessage;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public Long getQueueOffset() {
        return queueOffset;
    }

    public void setQueueOffset(Long queueOffset) {
        this.queueOffset = queueOffset;
    }

    @Override public String toString() {
        return "TransferData{" +
            "brokerName='" + brokerName + '\'' +
            ", topic='" + topic + '\'' +
            ", qos=" + qos +
            ", isWillMessage=" + isWillMessage +
            ", body=" + Arrays.toString(body) +
            ", queueOffset=" + queueOffset +
            '}';
    }
}
