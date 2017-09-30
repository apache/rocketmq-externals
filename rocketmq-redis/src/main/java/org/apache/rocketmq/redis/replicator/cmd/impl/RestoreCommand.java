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

public class RestoreCommand implements Command {

    private static final long serialVersionUID = 1L;

    private String key;
    private long ttl;
    private String serializedValue;
    private Boolean isReplace;
    private byte[] rawKey;
    private byte[] rawSerializedValue;

    public RestoreCommand() {
    }

    public RestoreCommand(String key, long ttl, String serializedValue, Boolean isReplace) {
        this(key, ttl, serializedValue, isReplace, null, null);
    }

    public RestoreCommand(String key, long ttl, String serializedValue, Boolean isReplace, byte[] rawKey,
        byte[] rawSerializedValue) {
        this.key = key;
        this.ttl = ttl;
        this.serializedValue = serializedValue;
        this.isReplace = isReplace;
        this.rawKey = rawKey;
        this.rawSerializedValue = rawSerializedValue;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public String getSerializedValue() {
        return serializedValue;
    }

    public void setSerializedValue(String serializedValue) {
        this.serializedValue = serializedValue;
    }

    public Boolean getReplace() {
        return isReplace;
    }

    public void setReplace(Boolean replace) {
        isReplace = replace;
    }

    public byte[] getRawKey() {
        return rawKey;
    }

    public void setRawKey(byte[] rawKey) {
        this.rawKey = rawKey;
    }

    public byte[] getRawSerializedValue() {
        return rawSerializedValue;
    }

    public void setRawSerializedValue(byte[] rawSerializedValue) {
        this.rawSerializedValue = rawSerializedValue;
    }

    @Override
    public String toString() {
        return "RestoreCommand{" +
            "key='" + key + '\'' +
            ", ttl=" + ttl +
            ", serializedValue='" + serializedValue + '\'' +
            ", isReplace=" + isReplace +
            '}';
    }
}
