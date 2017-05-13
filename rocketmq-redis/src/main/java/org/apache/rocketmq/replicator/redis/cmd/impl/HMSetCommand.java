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

import java.util.Map;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class HMSetCommand implements Command {
    private String key;
    private Map<String, String> fields;

    public HMSetCommand() {
    }

    public HMSetCommand(String key, Map<String, String> fields) {
        this.key = key;
        this.fields = fields;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }

    @Override
    public String toString() {
        return "HMSetCommand{" +
                "key='" + key + '\'' +
                ", fields=" + fields +
                '}';
    }
}
