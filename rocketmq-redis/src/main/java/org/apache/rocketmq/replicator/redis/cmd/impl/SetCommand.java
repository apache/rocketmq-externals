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
public class SetCommand implements Command {
    private String key;
    private String value;
    private Integer ex;
    private Long px;
    private ExistType existType;

    public SetCommand() {
    }

    public SetCommand(String key, String value, Integer ex, Long px, ExistType existType) {
        this.key = key;
        this.value = value;
        this.ex = ex;
        this.px = px;
        this.existType = existType;
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
