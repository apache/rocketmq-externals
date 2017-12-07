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

public class RenameCommand implements Command {

    private static final long serialVersionUID = 1L;

    private String key;
    private String newKey;
    private byte[] rawKey;
    private byte[] rawNewKey;

    public RenameCommand() {
    }

    public RenameCommand(String key, String newKey) {
        this(key, newKey, null, null);
    }

    public RenameCommand(String key, String newKey, byte[] rawKey, byte[] rawNewKey) {
        this.key = key;
        this.newKey = newKey;
        this.rawKey = rawKey;
        this.rawNewKey = rawNewKey;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getNewKey() {
        return newKey;
    }

    public void setNewKey(String newKey) {
        this.newKey = newKey;
    }

    public byte[] getRawKey() {
        return rawKey;
    }

    public void setRawKey(byte[] rawKey) {
        this.rawKey = rawKey;
    }

    public byte[] getRawNewKey() {
        return rawNewKey;
    }

    public void setRawNewKey(byte[] rawNewKey) {
        this.rawNewKey = rawNewKey;
    }

    @Override
    public String toString() {
        return "RenameCommand{" +
            "key='" + key + '\'' +
            ", newKey=" + newKey +
            '}';
    }
}
