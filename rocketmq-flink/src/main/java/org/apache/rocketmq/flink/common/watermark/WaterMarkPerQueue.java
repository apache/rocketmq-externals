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

package org.apache.rocketmq.flink.common.watermark;

import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.rocketmq.common.message.MessageQueue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class WaterMarkPerQueue {

    private ConcurrentMap<MessageQueue, Long> maxEventTimeTable;

    private long maxOutOfOrderness = 5000L; // 5 seconds

    public WaterMarkPerQueue() {
    }

    public WaterMarkPerQueue(long maxOutOfOrderness) {
        this.maxOutOfOrderness = maxOutOfOrderness;
        maxEventTimeTable = new ConcurrentHashMap<>();
    }

    public void extractTimestamp(MessageQueue mq, long timestamp) {
        long maxEventTime = maxEventTimeTable.getOrDefault(mq, maxOutOfOrderness);
        maxEventTimeTable.put(mq, Math.max(maxEventTime, timestamp));
    }

    public Watermark getCurrentWatermark() {
        // return the watermark as current highest timestamp minus the out-of-orderness bound
        long minTimestamp = maxOutOfOrderness;
        for (Map.Entry<MessageQueue, Long> entry : maxEventTimeTable.entrySet()) {
            minTimestamp = Math.min(minTimestamp, entry.getValue());
        }
        return new Watermark(minTimestamp - maxOutOfOrderness);
    }

    @Override
    public String toString() {
        return "WaterMarkPerQueue{" +
                "maxEventTimeTable=" + maxEventTimeTable +
                ", maxOutOfOrderness=" + maxOutOfOrderness +
                '}';
    }
}
