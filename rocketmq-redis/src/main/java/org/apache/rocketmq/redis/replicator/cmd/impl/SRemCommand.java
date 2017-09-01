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

import java.util.Arrays;
import org.apache.rocketmq.redis.replicator.cmd.Command;

public class SRemCommand implements Command {

    private static final long serialVersionUID = 1L;

    private String key;
    private String[] members;
    private byte[] rawKey;
    private byte[][] rawMembers;

    public SRemCommand() {
    }

    public SRemCommand(String key, String[] members) {
        this(key, members, null, null);
    }

    public SRemCommand(String key, String[] members, byte[] rawKey, byte[][] rawMembers) {
        this.key = key;
        this.members = members;
        this.rawKey = rawKey;
        this.rawMembers = rawMembers;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String[] getMembers() {
        return members;
    }

    public void setMembers(String[] members) {
        this.members = members;
    }

    public byte[] getRawKey() {
        return rawKey;
    }

    public void setRawKey(byte[] rawKey) {
        this.rawKey = rawKey;
    }

    public byte[][] getRawMembers() {
        return rawMembers;
    }

    public void setRawMembers(byte[][] rawMembers) {
        this.rawMembers = rawMembers;
    }

    @Override
    public String toString() {
        return "SRemCommand{" +
            "key='" + key + '\'' +
            ", members=" + Arrays.toString(members) +
            '}';
    }
}
