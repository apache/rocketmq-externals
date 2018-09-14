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

package org.apache.rocketmq.iot.connection.client.impl;

import io.netty.channel.Channel;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.rocketmq.iot.connection.client.Client;
import org.apache.rocketmq.iot.connection.client.ClientManager;

public class ClientManagerImpl implements ClientManager {

    private ConcurrentHashMap<Channel, Client> channel2Client = new ConcurrentHashMap<>();

    @Override public Client get(Channel channel) {
        return channel2Client.get(channel);
    }

    @Override public void put(Channel channel, Client client) {
        channel2Client.put(channel, client);
    }

    @Override public Client remove(Channel channel) {
        return channel2Client.remove(channel);
    }
}
