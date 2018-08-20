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
package org.apache.rocketmq.hbase.source;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.hadoop.hbase.util.Bytes.toBytes;

/**
 * This class represents the HBase client that effectively writes the messages to the corresponding HBase tables.
 */
public class HBaseClient {

    public static final byte[] COLUMN_FAMILY = toBytes("message");

    private static final Logger logger = LoggerFactory.getLogger(HBaseClient.class);

    private String zookeeperAddress;

    private int zookeeperPort;

    private Connection connection;

    /**
     * Constructor.
     *
     * @param config the configuration
     */
    public HBaseClient(Config config) {
        this.zookeeperAddress = config.getZookeeperAddress();
        this.zookeeperPort = config.getZookeeperPort();
    }

    /**
     * Starts the HBase client by opening a connection to the HBase server.
     *
     * @throws IOException
     */
    public void start() throws IOException {
        final Configuration hbaseConfig = HBaseConfiguration.create();
        hbaseConfig.set("hbase.zookeeper.quorum", zookeeperAddress);
        hbaseConfig.set("hbase.zookeeper.property.clientPort", Integer.toString(zookeeperPort));
        connection = ConnectionFactory.createConnection(hbaseConfig);
        logger.info("HBase client started.");
    }

    /**
     * Writes messages into a specified HBase table.
     *
     * @param tableName
     * @param messages
     * @throws IOException
     */
    public void put(String tableName, List<MessageExt> messages) throws IOException {

        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            final List<Put> puts = new ArrayList<>();

            for (MessageExt msg : messages) {
                final Put put = new Put(toBytes(msg.getMsgId()));
                put.addColumn(COLUMN_FAMILY, null, msg.getBody());
                puts.add(put);
            }

            table.put(puts);
        }
    }

    /**
     * Stops the HBase client.
     *
     * @throws IOException
     */
    public void stop() throws IOException {
        connection.close();
        logger.info("HBase client stopped.");
    }
}
