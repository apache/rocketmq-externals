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

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spark.ConsumerStrategy;
import org.apache.rocketmq.spark.HasOffsetRanges;
import org.apache.rocketmq.spark.LocationStrategy;
import org.apache.rocketmq.spark.OffsetRange;
import org.apache.rocketmq.spark.RocketMQConfig;
import org.apache.rocketmq.spark.RocketMQServerMock;
import org.apache.rocketmq.spark.RocketMqUtils;
import org.apache.rocketmq.spark.TopicQueueId;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class RocketMqUtilsTest {

    private static RocketMQServerMock mockServer = new RocketMQServerMock(9876, 10001);

    private static String NAME_SERVER = mockServer.getNameServerAddr();

    private static String TOPIC_DEFAULT = UUID.randomUUID().toString();

    private static int MESSAGE_NUM = 100;

    @BeforeClass
    public static void start() throws Exception {
        mockServer.startupServer();

        Thread.sleep(2000);

        //producer message
        mockServer.prepareDataTo(TOPIC_DEFAULT, MESSAGE_NUM);
    }

    @AfterClass
    public static void stop() {
        mockServer.shutdownServer();
    }

    @Test
    public void testConsumer() throws MQBrokerException, MQClientException, InterruptedException, UnsupportedEncodingException {

        // start up spark
        Map<String, String> optionParams = new HashMap<>();
        optionParams.put(RocketMQConfig.NAME_SERVER_ADDR, NAME_SERVER);
        SparkConf sparkConf = new SparkConf().setAppName("JavaCustomReceiver").setMaster("local[*]");
        JavaStreamingContext sc = new JavaStreamingContext(sparkConf, new Duration(1000));
        List<String> topics = new ArrayList<>();
        topics.add(TOPIC_DEFAULT);

        LocationStrategy locationStrategy = LocationStrategy.PreferConsistent();

        JavaInputDStream<MessageExt> stream = RocketMqUtils.createJavaMQPullStream(sc, UUID.randomUUID().toString(),
                topics, ConsumerStrategy.earliest(), false, false, false, locationStrategy, optionParams);


        final Set<MessageExt> result = Collections.synchronizedSet(new HashSet<MessageExt>());

        stream.foreachRDD(new VoidFunction<JavaRDD<MessageExt>>() {
            @Override
            public void call(JavaRDD<MessageExt> messageExtJavaRDD) throws Exception {
                result.addAll(messageExtJavaRDD.collect());
            }
        });

        sc.start();

        long startTime = System.currentTimeMillis();
        boolean matches = false;
        while (!matches && System.currentTimeMillis() - startTime < 20000) {
            matches = MESSAGE_NUM == result.size();
            Thread.sleep(50);
        }
        sc.stop();
    }

    @Test
    public void testGetOffsets() throws MQBrokerException, MQClientException, InterruptedException, UnsupportedEncodingException {

        Map<String, String> optionParams = new HashMap<>();
        optionParams.put(RocketMQConfig.NAME_SERVER_ADDR, NAME_SERVER);
        SparkConf sparkConf = new SparkConf().setAppName("JavaCustomReceiver").setMaster("local[*]");
        JavaStreamingContext sc = new JavaStreamingContext(sparkConf, new Duration(1000));
        List<String> topics = new ArrayList<>();
        topics.add(TOPIC_DEFAULT);

        LocationStrategy locationStrategy = LocationStrategy.PreferConsistent();

        JavaInputDStream<MessageExt> dStream = RocketMqUtils.createJavaMQPullStream(sc, UUID.randomUUID().toString(),
                topics, ConsumerStrategy.earliest(), false, false, false, locationStrategy, optionParams);

        // hold a reference to the current offset ranges, so it can be used downstream
        final AtomicReference<Map<TopicQueueId, OffsetRange[]>> offsetRanges = new AtomicReference<>();

        final Set<MessageExt> result = Collections.synchronizedSet(new HashSet<MessageExt>());

        dStream.transform(new Function<JavaRDD<MessageExt>, JavaRDD<MessageExt>>() {
            @Override
            public JavaRDD<MessageExt> call(JavaRDD<MessageExt> v1) throws Exception {
                Map<TopicQueueId, OffsetRange []> offsets = ((HasOffsetRanges) v1.rdd()).offsetRanges();
                offsetRanges.set(offsets);
                return v1;
            }
        }).foreachRDD(new VoidFunction<JavaRDD<MessageExt>>() {
            @Override
            public void call(JavaRDD<MessageExt> messageExtJavaRDD) throws Exception {
                result.addAll(messageExtJavaRDD.collect());
            }
        });

        sc.start();

        long startTime = System.currentTimeMillis();
        boolean matches = false;
        while (!matches && System.currentTimeMillis() - startTime < 10000) {
            matches = MESSAGE_NUM == result.size();
            Thread.sleep(50);
        }
        sc.stop();


    }
}
