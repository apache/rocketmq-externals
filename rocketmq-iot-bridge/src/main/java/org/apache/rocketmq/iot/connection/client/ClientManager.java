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

package org.apache.rocketmq.iot.connection.client;

import io.netty.channel.Channel;

public interface ClientManager {

    /**
     * Get the Client by its channel
     * @param channel
     * @return the Client which connects to the bridge with the channel, return <b>null</b> if
     */
    public Client get(Channel channel);

    /**
     * Put the Client by its channel
     * @param channel the channel by which the Client connects to the server
     * @param client
     */
    public void put(Channel channel, Client client);

    /**
     * Remove the Client by its channel usually the channel is disconnected
     * @param channel
     * @return the removed client, if the client doesn't exist return <b>null</b>
     */
    public Client remove(Channel channel);
}
