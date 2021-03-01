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

import org.apache.flink.streaming.api.functions.AssignerWithPunctuatedWatermarks;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.flink.RocketMQConfig;

/**
 * With Punctuated Watermarks
 * To generate watermarks whenever a certain event indicates that a new watermark might be generated, use
 * AssignerWithPunctuatedWatermarks. For this class Flink will first call the extractTimestamp(...) method to assign the
 * element a timestamp, and then immediately call the checkAndGetNextWatermark(...) method on that element.
 *
 * The checkAndGetNextWatermark(...) method is passed the timestamp that was assigned in the extractTimestamp(...)
 * method, and can decide whether it wants to generate a watermark. Whenever the checkAndGetNextWatermark(...) method
 * returns a non-null watermark, and that watermark is larger than the latest previous watermark, that new watermark
 * will be emitted.
 */
public class PunctuatedAssigner implements AssignerWithPunctuatedWatermarks<MessageExt> {
    @Override
    public long extractTimestamp(MessageExt element, long previousElementTimestamp) {
        return element.getBornTimestamp();
    }

    @Override
    public Watermark checkAndGetNextWatermark(MessageExt lastElement, long extractedTimestamp) {
        String lastValue = lastElement.getProperty(RocketMQConfig.WATERMARK);
        return lastValue != null ? new Watermark(extractedTimestamp) : null;
    }
}
