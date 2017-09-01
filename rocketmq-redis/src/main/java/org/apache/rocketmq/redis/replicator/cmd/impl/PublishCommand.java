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

package org.apache.rocketmq.redis.replicator.cmd.impl;

import org.apache.rocketmq.redis.replicator.cmd.Command;

public class PublishCommand implements Command {

    private static final long serialVersionUID = 1L;

    private String channel;
    private String message;
    private byte[] rawChannel;
    private byte[] rawMessage;

    public PublishCommand() {
    }

    public PublishCommand(String channel, String message) {
        this(channel, message, null, null);
    }

    public PublishCommand(String channel, String message, byte[] rawChannel, byte[] rawMessage) {
        this.channel = channel;
        this.message = message;
        this.rawChannel = rawChannel;
        this.rawMessage = rawMessage;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public byte[] getRawChannel() {
        return rawChannel;
    }

    public void setRawChannel(byte[] rawChannel) {
        this.rawChannel = rawChannel;
    }

    public byte[] getRawMessage() {
        return rawMessage;
    }

    public void setRawMessage(byte[] rawMessage) {
        this.rawMessage = rawMessage;
    }

    @Override
    public String toString() {
        return "PublishCommand{" +
            "channel='" + channel + '\'' +
            ", message='" + message + '\'' +
            '}';
    }
}
