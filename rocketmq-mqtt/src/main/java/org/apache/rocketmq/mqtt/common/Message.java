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

public class Message {
    private String topicName;
    private Integer pushQos;
    private String brokerName;
    private Long queueOffset;
    private byte[] body;

    public Message(Long queueOffset, byte[] body) {
        this.queueOffset = queueOffset;
        this.body = body;
    }

    public Message(String topicName, Integer pushQos, String brokerName, Long queueOffset, byte[] body) {
        this(queueOffset, body);
        this.topicName = topicName;
        this.pushQos = pushQos;
        this.brokerName = brokerName;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public Integer getPushQos() {
        return pushQos;
    }

    public void setPushQos(Integer pushQos) {
        this.pushQos = pushQos;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
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
        return "Message{" +
            "topicName='" + topicName + '\'' +
            ", pushQos=" + pushQos +
            ", brokerName='" + brokerName + '\'' +
            ", queueOffset=" + queueOffset +
            ", body=" + Arrays.toString(body) +
            '}';
    }
}
