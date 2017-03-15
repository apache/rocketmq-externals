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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.rocketmq.jms;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.jms.domain.JmsBaseMessageConsumer;
import org.apache.rocketmq.jms.domain.JmsBaseMessageProducer;
import org.apache.rocketmq.jms.domain.RMQPushConsumerExt;
import org.junit.Assert;

public class JmsTestUtil {
    public static MQProducer getMQProducer(String producerId) throws Exception {
        Assert.assertNotNull(producerId);
        Field field = JmsBaseMessageProducer.class.getDeclaredField("producerMap");
        field.setAccessible(true);
        ConcurrentMap<String, MQProducer> producerMap = (ConcurrentMap<String, MQProducer>) field.get(null);
        return  producerMap.get(producerId);
    }
    public static RMQPushConsumerExt getRMQPushConsumerExt(String consumerId) throws Exception {
        Assert.assertNotNull(consumerId);
        Field field = JmsBaseMessageConsumer.class.getDeclaredField("consumerMap");
        field.setAccessible(true);
        ConcurrentMap<String, RMQPushConsumerExt> consumerMap = (ConcurrentMap<String, RMQPushConsumerExt>) field.get(null);
        return  consumerMap.get(consumerId);
    }
    public static void checkConsumerState(String consumerId, boolean isNull, boolean isStarted) throws Exception {
        RMQPushConsumerExt rmqPushConsumerExt = getRMQPushConsumerExt(consumerId);
        if (isNull) {
            Assert.assertNull(rmqPushConsumerExt);
        } else  {
            Assert.assertNotNull(rmqPushConsumerExt);
            Assert.assertEquals(isStarted, rmqPushConsumerExt.isStarted());
        }
    }


}
