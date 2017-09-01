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

public class LInsertCommand implements Command {

    private static final long serialVersionUID = 1L;

    private String key;
    private LInsertType lInsertType;
    private String pivot;
    private String value;
    private byte[] rawKey;
    private byte[] rawPivot;
    private byte[] rawValue;

    public LInsertCommand() {
    }

    public LInsertCommand(String key, LInsertType lInsertType, String pivot, String value) {
        this(key, lInsertType, pivot, value, null, null, null);
    }

    public LInsertCommand(String key, LInsertType lInsertType, String pivot, String value, byte[] rawKey,
        byte[] rawPivot, byte[] rawValue) {
        this.key = key;
        this.lInsertType = lInsertType;
        this.pivot = pivot;
        this.value = value;
        this.rawKey = rawKey;
        this.rawPivot = rawPivot;
        this.rawValue = rawValue;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public LInsertType getlInsertType() {
        return lInsertType;
    }

    public void setlInsertType(LInsertType lInsertType) {
        this.lInsertType = lInsertType;
    }

    public String getPivot() {
        return pivot;
    }

    public void setPivot(String pivot) {
        this.pivot = pivot;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public byte[] getRawKey() {
        return rawKey;
    }

    public void setRawKey(byte[] rawKey) {
        this.rawKey = rawKey;
    }

    public byte[] getRawPivot() {
        return rawPivot;
    }

    public void setRawPivot(byte[] rawPivot) {
        this.rawPivot = rawPivot;
    }

    public byte[] getRawValue() {
        return rawValue;
    }

    public void setRawValue(byte[] rawValue) {
        this.rawValue = rawValue;
    }

    @Override
    public String toString() {
        return "LInsertCommand{" +
            "key='" + key + '\'' +
            ", lInsertType=" + lInsertType +
            ", pivot='" + pivot + '\'' +
            ", value='" + value + '\'' +
            '}';
    }
}
