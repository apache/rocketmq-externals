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

import org.apache.rocketmq.spark.RocketMQConfig;
import org.apache.rocketmq.spark.RocketMQServerMock;
import org.apache.rocketmq.spark.RocketMqUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;

public class ReliableRocketMQReceiverTest {
    private static RocketMQServerMock mockServer = new RocketMQServerMock(9878, 10003);

    private static final String NAMESERVER_ADDR = mockServer.getNameServerAddr();
    private static final String CONSUMER_GROUP = "wordcount2";
    private static final String CONSUMER_TOPIC = "wordcountsource";

    @BeforeClass
    public static void start() throws Exception {
        mockServer.startupServer();

        Thread.sleep(2000);

        //prepare data
        mockServer.prepareDataTo(CONSUMER_TOPIC, 5);
    }

    @AfterClass
    public static void stop() {
        mockServer.shutdownServer();
    }

    @Test
    public void testReliableRocketMQReceiver() {
        SparkConf conf = new SparkConf().setMaster("local[2]").setAppName("NetworkWordCount");
        JavaStreamingContext jssc = new JavaStreamingContext(conf, Durations.seconds(5));
        Properties properties = new Properties();
        properties.setProperty(RocketMQConfig.NAME_SERVER_ADDR, NAMESERVER_ADDR);
        properties.setProperty(RocketMQConfig.CONSUMER_GROUP, CONSUMER_GROUP);
        properties.setProperty(RocketMQConfig.CONSUMER_TOPIC, CONSUMER_TOPIC);
        JavaInputDStream ds = RocketMqUtils.createJavaReliableMQPushStream(jssc, properties, StorageLevel.MEMORY_ONLY());
        ds.print();
        jssc.start();
        try {
            jssc.awaitTerminationOrTimeout(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        jssc.stop();
    }
}
