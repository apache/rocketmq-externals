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

public class SMoveCommand implements Command {

    private static final long serialVersionUID = 1L;

    private String source;
    private String destination;
    private String member;
    private byte[] rawSource;
    private byte[] rawDestination;
    private byte[] rawMember;

    public SMoveCommand() {
    }

    public SMoveCommand(String source, String destination, String member) {
        this(source, destination, member, null, null, null);
    }

    public SMoveCommand(String source, String destination, String member, byte[] rawSource, byte[] rawDestination,
        byte[] rawMember) {
        this.source = source;
        this.destination = destination;
        this.member = member;
        this.rawSource = rawSource;
        this.rawDestination = rawDestination;
        this.rawMember = rawMember;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getMember() {
        return member;
    }

    public void setMember(String member) {
        this.member = member;
    }

    public byte[] getRawSource() {
        return rawSource;
    }

    public void setRawSource(byte[] rawSource) {
        this.rawSource = rawSource;
    }

    public byte[] getRawDestination() {
        return rawDestination;
    }

    public void setRawDestination(byte[] rawDestination) {
        this.rawDestination = rawDestination;
    }

    public byte[] getRawMember() {
        return rawMember;
    }

    public void setRawMember(byte[] rawMember) {
        this.rawMember = rawMember;
    }

    @Override
    public String toString() {
        return "SMoveCommand{" +
            "source='" + source + '\'' +
            ", destination='" + destination + '\'' +
            ", member='" + member + '\'' +
            '}';
    }
}
