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
public class LInsertCommand implements Command {
    private String key;
    private LInsertType lInsertType;
    private String pivot;
    private String value;

    public LInsertCommand() {
    }

    public LInsertCommand(String key, LInsertType lInsertType, String pivot, String value) {
        this.key = key;
        this.pivot = pivot;
        this.value = value;
        this.lInsertType = lInsertType;
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
