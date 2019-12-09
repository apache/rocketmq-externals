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
package org.apache.rocketmq.mqtt.client;

import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;
import org.apache.rocketmq.mqtt.common.MqttChannelEventListener;
import org.apache.rocketmq.mqtt.common.RemotingChannel;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttClientHousekeepingService implements MqttChannelEventListener {
    private static final Logger log = LoggerFactory.getLogger(MqttClientHousekeepingService.class);

    private final MqttClientManager mqttClientManager;

    public MqttClientHousekeepingService(final MqttClientManager mqttClientManager) {
        this.mqttClientManager = mqttClientManager;
    }

    public void start(long interval) {
        this.mqttClientManager.startScan(interval);
    }

    public void shutdown() {
        this.mqttClientManager.shutdown();
    }

    @Override
    public void onChannelConnect(String remoteAddr, RemotingChannel remotingChannel) {
        log.info("Remoting channel connected: {}", RemotingHelper.parseSocketAddressAddr(remotingChannel.remoteAddress()));
    }

    @Override
    public void onChannelClose(String remoteAddr, RemotingChannel remotingChannel) {
        log.info("Remoting channel closed: {}", RemotingHelper.parseSocketAddressAddr(remotingChannel.remoteAddress()));
        this.mqttClientManager.onClose(remotingChannel);
    }

    @Override
    public void onChannelException(String remoteAddr, RemotingChannel remotingChannel) {
        log.info("Remoting channel exception: {}", RemotingHelper.parseSocketAddressAddr(remotingChannel.remoteAddress()));
        this.mqttClientManager.onException(remotingChannel);

    }

    @Override
    public void onChannelIdle(String remoteAddr, RemotingChannel remotingChannel) {
        log.info("Remoting channel idle: {}", RemotingHelper.parseSocketAddressAddr(remotingChannel.remoteAddress()));
        this.mqttClientManager.onIdle(remotingChannel);
    }
}
