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

public class HIncrByCommand implements Command {

    private static final long serialVersionUID = 1L;

    private String key;
    private String field;
    private long increment;
    private byte[] rawKey;
    private byte[] rawField;

    public HIncrByCommand() {
    }

    public HIncrByCommand(String key, String field, long increment) {
        this(key, field, increment, null, null);
    }

    public HIncrByCommand(String key, String field, long increment, byte[] rawKey, byte[] rawField) {
        this.key = key;
        this.field = field;
        this.increment = increment;
        this.rawKey = rawKey;
        this.rawField = rawField;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public long getIncrement() {
        return increment;
    }

    public void setIncrement(long increment) {
        this.increment = increment;
    }

    public byte[] getRawKey() {
        return rawKey;
    }

    public void setRawKey(byte[] rawKey) {
        this.rawKey = rawKey;
    }

    public byte[] getRawField() {
        return rawField;
    }

    public void setRawField(byte[] rawField) {
        this.rawField = rawField;
    }

    @Override
    public String toString() {
        return "HIncrByCommand{" +
            "key='" + key + '\'' +
            ", field='" + field + '\'' +
            ", increment='" + increment + '\'' +
            '}';
    }
}
