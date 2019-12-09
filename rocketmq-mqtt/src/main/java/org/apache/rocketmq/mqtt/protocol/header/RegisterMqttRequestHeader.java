package org.apache.rocketmq.mqtt.protocol.header;/*
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

import org.apache.rocketmq.remoting.CommandCustomHeader;
import org.apache.rocketmq.remoting.annotation.CFNotNull;
import org.apache.rocketmq.remoting.exception.RemotingCommandException;

public class RegisterMqttRequestHeader implements CommandCustomHeader {

    @CFNotNull
    private String mqttBridgeName;

    @CFNotNull
    private String mqttBridgeIP;

    @CFNotNull
    private int listenPort;

    @CFNotNull
    private int mqttListenPort;

    //预留字段
    private String clusterName;

    @Override
    public void checkFields() throws RemotingCommandException {

    }

    public String getMqttBridgeName() {
        return mqttBridgeName;
    }

    public void setMqttBridgeName(String mqttBridgeName) {
        this.mqttBridgeName = mqttBridgeName;
    }

    public String getMqttBridgeIP() {
        return mqttBridgeIP;
    }

    public void setMqttBridgeIP(String mqttBridgeIP) {
        this.mqttBridgeIP = mqttBridgeIP;
    }

    public int getListenPort() {
        return listenPort;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    public int getMqttListenPort() {
        return mqttListenPort;
    }

    public void setMqttListenPort(int mqttListenPort) {
        this.mqttListenPort = mqttListenPort;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }
}
