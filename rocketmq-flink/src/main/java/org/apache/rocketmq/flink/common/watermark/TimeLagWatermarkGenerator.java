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

/**
 * This generator generates watermarks that are lagging behind processing time by a certain amount. It assumes that
 * elements arrive in Flink after at most a certain time.
 */
public class TimeLagWatermarkGenerator implements AssignerWithPeriodicWatermarks<MessageExt> {
    private long maxTimeLag = 5000; // 5 seconds

    TimeLagWatermarkGenerator() {
    }

    TimeLagWatermarkGenerator(long maxTimeLag) {
        this.maxTimeLag = maxTimeLag;
    }

    @Override
    public long extractTimestamp(MessageExt element, long previousElementTimestamp) {
        return element.getBornTimestamp();
    }

    @Override
    public Watermark getCurrentWatermark() {
        // return the watermark as current time minus the maximum time lag
        return new Watermark(System.currentTimeMillis() - maxTimeLag);
    }

    @Override public String toString() {
        return "TimeLagWatermarkGenerator{" +
            "maxTimeLag=" + maxTimeLag +
            '}';
    }
}
