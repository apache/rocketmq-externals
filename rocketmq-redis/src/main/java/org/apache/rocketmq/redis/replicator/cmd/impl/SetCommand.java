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

public class SetCommand implements Command {

    private static final long serialVersionUID = 1L;

    private String key;
    private String value;
    private Integer ex;
    private Long px;
    private ExistType existType;
    private byte[] rawKey;
    private byte[] rawValue;

    public SetCommand() {
    }

    public SetCommand(String key, String value, Integer ex, Long px, ExistType existType) {
        this(key, value, ex, px, existType, null, null);
    }

    public SetCommand(String key, String value, Integer ex, Long px, ExistType existType, byte[] rawKey,
        byte[] rawValue) {
        this.key = key;
        this.value = value;
        this.ex = ex;
        this.px = px;
        this.existType = existType;
        this.rawKey = rawKey;
        this.rawValue = rawValue;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getEx() {
        return ex;
    }

    public void setEx(Integer ex) {
        this.ex = ex;
    }

    public Long getPx() {
        return px;
    }

    public void setPx(Long px) {
        this.px = px;
    }

    public ExistType getExistType() {
        return existType;
    }

    public void setExistType(ExistType existType) {
        this.existType = existType;
    }

    public byte[] getRawKey() {
        return rawKey;
    }

    public void setRawKey(byte[] rawKey) {
        this.rawKey = rawKey;
    }

    public byte[] getRawValue() {
        return rawValue;
    }

    public void setRawValue(byte[] rawValue) {
        this.rawValue = rawValue;
    }

    @Override
    public String toString() {
        return "SetCommand{" +
            "name='" + key + '\'' +
            ", value='" + value + '\'' +
            ", ex=" + ex +
            ", px=" + px +
            ", existType=" + existType +
            '}';
    }
}
