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
 * @since 2.1.1
 */
public class ZRemRangeByRankCommand implements Command {
    private String key;
    private long start;
    private long stop;

    public ZRemRangeByRankCommand() {
    }

    public ZRemRangeByRankCommand(String key, long start, long stop) {
        this.key = key;
        this.start = start;
        this.stop = stop;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getStop() {
        return stop;
    }

    public void setStop(long stop) {
        this.stop = stop;
    }

    @Override
    public String toString() {
        return "ZRemRangeByRankCommand{" +
                "key='" + key + '\'' +
                ", start=" + start +
                ", stop=" + stop +
                '}';
    }
}
