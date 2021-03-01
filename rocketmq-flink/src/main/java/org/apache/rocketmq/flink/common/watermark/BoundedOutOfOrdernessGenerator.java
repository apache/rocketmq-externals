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

public class BoundedOutOfOrdernessGenerator implements AssignerWithPeriodicWatermarks<MessageExt> {

    private long maxOutOfOrderness = 5000; // 5 seconds

    private long currentMaxTimestamp;

    public BoundedOutOfOrdernessGenerator() {
    }

    public BoundedOutOfOrdernessGenerator(long maxOutOfOrderness) {
        this.maxOutOfOrderness = maxOutOfOrderness;
    }

    @Override
    public long extractTimestamp(MessageExt element, long previousElementTimestamp) {
        long timestamp = element.getBornTimestamp();
        currentMaxTimestamp = Math.max(timestamp, currentMaxTimestamp);
        return timestamp;
    }

    @Override
    public Watermark getCurrentWatermark() {
        // return the watermark as current highest timestamp minus the out-of-orderness bound
        return new Watermark(currentMaxTimestamp - maxOutOfOrderness);
    }

    @Override
    public String toString() {
        return "BoundedOutOfOrdernessGenerator{" +
            "maxOutOfOrderness=" + maxOutOfOrderness +
            ", currentMaxTimestamp=" + currentMaxTimestamp +
            '}';
    }
}
