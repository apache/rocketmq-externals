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

import org.apache.rocketmq.mqtt.common.RemotingChannel;

import java.util.List;

public interface MqttClientManager {
    boolean register(MQTTSession client);

    void unRegister(RemotingChannel remotingChannel);

    void onClose(RemotingChannel remotingChannel);

    void onException(RemotingChannel remotingChannel);

    void onIdle(RemotingChannel remotingChannel);

    List<String> getAllClientId();

    MQTTSession getClient(RemotingChannel remotingChannel);

    RemotingChannel getChannel(String clientId);

    void startScan(long interval);

    void shutdown();
}
