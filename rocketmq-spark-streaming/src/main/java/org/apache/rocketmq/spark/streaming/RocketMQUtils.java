/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.spark.streaming;

import org.apache.rocketmq.common.message.Message;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import java.util.Properties;

public final class RocketMQUtils {

    public static int getInteger(Properties props, String key, int defaultValue) {
        return Integer.parseInt(props.getProperty(key, String.valueOf(defaultValue)));
    }

    public static boolean getBoolean(Properties props, String key, boolean defaultValue) {
        return Boolean.parseBoolean(props.getProperty(key, String.valueOf(defaultValue)));
    }

    public static JavaInputDStream createInputDStream(JavaStreamingContext jssc, Properties properties, StorageLevel level) {
        return createInputDStream(jssc, properties, level, false);
    }

    public static JavaInputDStream createReliableInputDStream(JavaStreamingContext jssc, Properties properties, StorageLevel level) {
        return createInputDStream(jssc, properties, level, true);
    }

    public static JavaInputDStream createInputDStream(JavaStreamingContext jssc, Properties properties, StorageLevel level, boolean reliable) {
        if (jssc == null || properties == null || level == null) {
            return null;
        }
        RocketMQReceiver receiver = reliable ? new ReliableRocketMQReceiver(properties, level) : new RocketMQReceiver(properties, level);
        JavaReceiverInputDStream<Message> ds = jssc.receiverStream(receiver);
        return ds;
    }
}
