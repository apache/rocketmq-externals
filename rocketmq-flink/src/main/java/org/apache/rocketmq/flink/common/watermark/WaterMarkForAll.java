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

public class WaterMarkForAll {

    private long maxOutOfOrderness = 5000L; // 5 seconds

    private long maxTimestamp = 0L;

    public WaterMarkForAll() {
    }

    public WaterMarkForAll(long maxOutOfOrderness) {
        this.maxOutOfOrderness = maxOutOfOrderness;
    }

    public void extractTimestamp(long timestamp) {
        maxTimestamp = Math.max(timestamp, maxTimestamp);
    }

    public Watermark getCurrentWatermark() {
        return new Watermark(maxTimestamp - maxOutOfOrderness);
    }
}
