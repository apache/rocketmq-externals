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
package org.apache.rocketmq.mqtt;

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import java.util.concurrent.ExecutorService;
import org.apache.rocketmq.mqtt.common.MqttChannelEventListener;
import org.apache.rocketmq.mqtt.common.RemotingChannel;
import org.apache.rocketmq.mqtt.processor.MqttRequestProcessor;
import org.apache.rocketmq.remoting.RemotingService;
import org.apache.rocketmq.remoting.common.Pair;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.exception.RemotingTooMuchRequestException;
import org.apache.rocketmq.mqtt.config.MultiNettyServerConfig;

public interface MqttRemotingServer extends RemotingService {

    void registerProcessor(final MqttMessageType messageType, final MqttRequestProcessor processor,
        final ExecutorService executor);

    void registerDefaultProcessor(final MqttRequestProcessor processor, final ExecutorService executor);

    int localListenPort();

    Pair<MqttRequestProcessor, ExecutorService> getProcessorPair(final MqttMessageType messageType);

    void push(RemotingChannel remotingChannel, MqttMessage request,
              long timeoutMillis) throws InterruptedException,
        RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException;

    void invokeOneway(final RemotingChannel remotingChannel, final MqttMessage request, final long timeoutMillis)
        throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException,
        RemotingSendRequestException;

    MqttRemotingServer init(MultiNettyServerConfig mqttServerConfig, MqttChannelEventListener mqttChannelEventListener);
}
