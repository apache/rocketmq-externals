/*
 *
 *   Copyright 2016 leon chen
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  modified:
 *    1. rename package from com.moilioncircle.redis.replicator to
 *        org.apache.rocketmq.replicator.redis
 *
 */

package org.apache.rocketmq.replicator.redis.cmd.impl;

import org.apache.rocketmq.replicator.redis.cmd.Command;
import org.apache.rocketmq.replicator.redis.rdb.datatype.ZSetEntry;

import java.util.Arrays;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class ZAddCommand implements Command {
    private String key;
    private ExistType existType;
    private Boolean isCh;
    private Boolean isIncr;
    private ZSetEntry[] zSetEntries;

    public ZAddCommand() {
    }

    public ZAddCommand(String key, ExistType existType, Boolean isCh, Boolean isIncr, ZSetEntry[] zSetEntries) {
        this.key = key;
        this.existType = existType;
        this.isCh = isCh;
        this.isIncr = isIncr;
        this.zSetEntries = zSetEntries;
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
