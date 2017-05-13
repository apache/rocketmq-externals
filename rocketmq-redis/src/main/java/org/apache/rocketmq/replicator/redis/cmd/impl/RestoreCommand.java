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

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class RestoreCommand implements Command {
    private String key;
    private int ttl;
    private String serializedValue;
    private Boolean isReplace;

    public RestoreCommand() {
    }

    public RestoreCommand(String key, int ttl, String serializedValue, Boolean isReplace) {
        this.key = key;
        this.ttl = ttl;
        this.serializedValue = serializedValue;
        this.isReplace = isReplace;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
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
