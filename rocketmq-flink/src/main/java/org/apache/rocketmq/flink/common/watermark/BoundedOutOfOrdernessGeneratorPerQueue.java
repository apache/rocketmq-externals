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

import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 取每条队列中的最大eventTime的最小值作为当前source的watermark
 */
public class BoundedOutOfOrdernessGeneratorPerQueue implements AssignerWithPeriodicWatermarks<MessageExt> {

    private Map<String, Long> maxEventTimeTable;
    private long maxOutOfOrderness = 5000L; // 5 seconds

    public BoundedOutOfOrdernessGeneratorPerQueue() {
    }

    public BoundedOutOfOrdernessGeneratorPerQueue(long maxOutOfOrderness) {
        this.maxOutOfOrderness = maxOutOfOrderness;
        maxEventTimeTable = new ConcurrentHashMap<>();
    }

    @Override
    public long extractTimestamp(MessageExt element, long previousElementTimestamp) {
        String key = element.getBrokerName() + "_" + element.getQueueId();
        Long maxEventTime = maxEventTimeTable.getOrDefault(key, maxOutOfOrderness);
        long timestamp = element.getBornTimestamp();
        maxEventTimeTable.put(key, Math.max(maxEventTime, timestamp));
        return timestamp;
    }

    @Override
    public Watermark getCurrentWatermark() {
        // return the watermark as current highest timestamp minus the out-of-orderness bound
        long minTimestamp = 0L;
        for (Map.Entry<String, Long> entry : maxEventTimeTable.entrySet()) {
            minTimestamp = Math.min(minTimestamp, entry.getValue());
        }
        return new Watermark(minTimestamp - maxOutOfOrderness);
    }

    @Override
    public String toString() {
        return "BoundedOutOfOrdernessGeneratorPerQueue{" +
                "maxEventTimeTable=" + maxEventTimeTable +
                ", maxOutOfOrderness=" + maxOutOfOrderness +
                '}';
    }
}
