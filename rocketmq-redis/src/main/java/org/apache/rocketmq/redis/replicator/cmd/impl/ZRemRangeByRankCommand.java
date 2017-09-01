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

public class ZRemRangeByRankCommand implements Command {

    private static final long serialVersionUID = 1L;

    private String key;
    private long start;
    private long stop;
    private byte[] rawKey;

    public ZRemRangeByRankCommand() {
    }

    public ZRemRangeByRankCommand(String key, long start, long stop) {
        this(key, start, stop, null);
    }

    public ZRemRangeByRankCommand(String key, long start, long stop, byte[] rawKey) {
        this.key = key;
        this.start = start;
        this.stop = stop;
        this.rawKey = rawKey;
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

    public byte[] getRawKey() {
        return rawKey;
    }

    public void setRawKey(byte[] rawKey) {
        this.rawKey = rawKey;
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
