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

public class SetTypeOffsetValue implements Statement {

    private static final long serialVersionUID = 1L;

    private String type;
    private String offset;
    private long value;
    private byte[] rawType;
    private byte[] rawOffset;

    public SetTypeOffsetValue() {
    }

    public SetTypeOffsetValue(String type, String offset, long value) {
        this(type, offset, value, null, null);
    }

    public SetTypeOffsetValue(String type, String offset, long value, byte[] rawType, byte[] rawOffset) {
        this.type = type;
        this.offset = offset;
        this.value = value;
        this.rawType = rawType;
        this.rawOffset = rawOffset;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public byte[] getRawType() {
        return rawType;
    }

    public void setRawType(byte[] rawType) {
        this.rawType = rawType;
    }

    public byte[] getRawOffset() {
        return rawOffset;
    }

    public void setRawOffset(byte[] rawOffset) {
        this.rawOffset = rawOffset;
    }

    @Override
    public String toString() {
        return "SetTypeOffsetValue{" +
            "type='" + type + '\'' +
            ", offset='" + offset + '\'' +
            ", value=" + value +
            '}';
    }
}