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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.rocketmq.jms.domain;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class CommonContext {
    private String accessKey;
    private String secretKey;

    private String consumerId;
    private String producerId;

    private String provider;

    private String appId;

    private String nameServer;

    /**
     * MQType
     */
    private String mqType;

    /**
     * Using for distinguishing client jvm process
     */
    private String instanceName;
    /**
     * Set consumer threadPool Size
     */
    private int consumeThreadNums;
    /**
     * Set send message timeOut
     */
    private int sendMsgTimeoutMillis = -1;

    /**
     * @return the appId
     */
    public String getAppId() {
        return appId;
    }

    /**
     * @param appId the appId to set
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * @return the provider
     */
    public String getProvider() {
        return provider;
    }

    /**
     * @param provider the provider to set
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * @return the instanceName
     */
    public String getInstanceName() {
        return instanceName;
    }

    /**
     * @param instanceName the instanceName to set
     */
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    /**
     * @return the accessKey
     */
    public String getAccessKey() {
        return accessKey;
    }

    /**
     * @param accessKey the accessKey to set
     */
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    /**
     * @return the secretKey
     */
    public String getSecretKey() {
        return secretKey;
    }

    /**
     * @param secretKey the secretKey to set
     */
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * @return consumer thread nums
     */
    public int getConsumeThreadNums() {
        return consumeThreadNums;
    }

    /**
     * @param consumeThreadNums
     */
    public void setConsumeThreadNums(int consumeThreadNums) {
        this.consumeThreadNums = consumeThreadNums;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }

    public String getProducerId() {
        return producerId;
    }

    public void setProducerId(String producerId) {
        this.producerId = producerId;
    }

    public int getSendMsgTimeoutMillis() {
        return sendMsgTimeoutMillis;
    }

    public void setSendMsgTimeoutMillis(int sendMsgTimeoutMillis) {
        this.sendMsgTimeoutMillis = sendMsgTimeoutMillis;
    }

    public String getMqType() {
        return mqType;
    }

    public void setMqType(String mqType) {
        this.mqType = mqType;
    }

    public String getNameServer() {
        return nameServer;
    }

    public void setNameServer(String nameServer) {
        this.nameServer = nameServer;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.DEFAULT_STYLE);
    }

}
