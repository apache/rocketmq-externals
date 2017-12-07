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
import org.apache.rocketmq.redis.replicator.rdb.datatype.ZSetEntry;

public class ZAddCommand implements Command {

    private static final long serialVersionUID = 1L;

    private String key;
    private ExistType existType;
    private Boolean isCh;
    private Boolean isIncr;
    private ZSetEntry[] zSetEntries;
    private byte[] rawKey;

    public ZAddCommand() {
    }

    public ZAddCommand(String key, ExistType existType, Boolean isCh, Boolean isIncr, ZSetEntry[] zSetEntries) {
        this(key, existType, isCh, isIncr, zSetEntries, null);
    }

    public ZAddCommand(String key, ExistType existType, Boolean isCh, Boolean isIncr, ZSetEntry[] zSetEntries,
        byte[] rawKey) {
        this.key = key;
        this.existType = existType;
        this.isCh = isCh;
        this.isIncr = isIncr;
        this.zSetEntries = zSetEntries;
        this.rawKey = rawKey;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public ExistType getExistType() {
        return existType;
    }

    public void setExistType(ExistType existType) {
        this.existType = existType;
    }

    public Boolean getCh() {
        return isCh;
    }

    public void setCh(Boolean ch) {
        isCh = ch;
    }

    public Boolean getIncr() {
        return isIncr;
    }

    public void setIncr(Boolean incr) {
        isIncr = incr;
    }

    public ZSetEntry[] getZSetEntries() {
        return zSetEntries;
    }

    public ZSetEntry[] getzSetEntries() {
        return zSetEntries;
    }

    public void setzSetEntries(ZSetEntry[] zSetEntries) {
        this.zSetEntries = zSetEntries;
    }

    public byte[] getRawKey() {
        return rawKey;
    }

    public void setRawKey(byte[] rawKey) {
        this.rawKey = rawKey;
    }

    @Override
    public String toString() {
        return "ZAddCommand{" +
            "key='" + key + '\'' +
            ", existType=" + existType +
            ", isCh=" + isCh +
            ", isIncr=" + isIncr +
            ", zSetEntries=" + Arrays.toString(zSetEntries) +
            '}';
    }

}
