/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.spark.sql;

import org.apache.rocketmq.spark.RocketMQConfig;
import org.apache.rocketmq.spark.RocketMQServerMock;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.rocketmq.RocketMQSourceProvider;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class RocketMQDataSourceTest {
    private static RocketMQServerMock mockServer = new RocketMQServerMock(9878, 10003);

    private static final String NAMESERVER_ADDR = mockServer.getNameServerAddr();
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
    public void testStructuredStreaming() {
        String className = RocketMQSourceProvider.class.getCanonicalName();

        SparkSession spark = SparkSession
            .builder()
            .master("local[2]")
            .appName("NetworkWordCount")
            .getOrCreate();

        Dataset<Row> dfInput = spark
            .readStream()
            .format(className)
            .option(RocketMQConfig.NAME_SERVER_ADDR, NAMESERVER_ADDR)
            .option(RocketMQConfig.CONSUMER_TOPIC, CONSUMER_TOPIC) // required
            .load();

        Dataset<Row> dfOutput = dfInput.select("*");

        StreamingQuery query = dfOutput.writeStream()
            .outputMode("append")
            .format("console")
            .start();

        try {
            query.awaitTermination(10000);
        } catch (StreamingQueryException e) {
            e.printStackTrace();
        }
    }
}
