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
package org.apache.rocketmq.sink;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.replication.ReplicationAdmin;
import org.apache.hadoop.hbase.replication.ReplicationException;
import org.apache.hadoop.hbase.replication.ReplicationPeerConfig;
import org.apache.hadoop.hbase.zookeeper.ZKConfig;
import org.apache.rocketmq.broker.BrokerController;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.consumer.PullStatus;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.BrokerConfig;
import org.apache.rocketmq.common.MQVersion;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.namesrv.NamesrvConfig;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.hbase.sink.Replicator;
import org.apache.rocketmq.hbase.sink.Transaction;
import org.apache.rocketmq.namesrv.NamesrvController;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.remoting.netty.NettyClientConfig;
import org.apache.rocketmq.remoting.netty.NettyServerConfig;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.store.config.MessageStoreConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.hadoop.hbase.util.Bytes.toBytes;
import static org.junit.Assert.assertEquals;

/**
 * This class tests the replicator by writing data from hbase to rocketmq and reading it back.
 */
public class ReplicatorTest {

    private static final String CONSUMER_GROUP_NAME = "HBASE_CONSUMER_GROUP_TEST";

    private static final String ROCKETMQ_TOPIC = "rocketmq-hbase-topic-test";

    private static final String NAMESERVER = "localhost:9876";

    private static final String TABLE_NAME_STR = "hbase-rocketmq-test";

    private static final String PEER_NAME = "rocketmq.hbase";

    private final TableName TABLE_NAME = TableName.valueOf(TABLE_NAME_STR);
    private final String ROWKEY = "rk-%s";
    private final String COLUMN_FAMILY = "d";
    private final String QUALIFIER = "q";
    private final String VALUE = "v";

    private static final Logger logger = LoggerFactory.getLogger(ReplicatorTest.class);

    private static NamesrvController namesrvController;

    private static BrokerController brokerController;

    private HBaseTestingUtility utility;

    private int numRegionServers;

    private int batchSize = 100;

    /**
     * This method starts the HBase cluster and the RocketMQ server.
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        final Configuration hbaseConf = HBaseConfiguration.create();
        hbaseConf.setInt("replication.stats.thread.period.seconds", 5);
        hbaseConf.setLong("replication.sleep.before.failover", 2000);
        hbaseConf.setInt("replication.source.maxretriesmultiplier", 10);
        hbaseConf.setBoolean(HConstants.REPLICATION_ENABLE_KEY, true);

        // Add RocketMQ properties - we prefix each property with 'rocketmq'
        addRocketMQProperties(hbaseConf);

        utility = new HBaseTestingUtility(hbaseConf);
        utility.startMiniCluster();
        utility.getHBaseCluster().getRegionServerThreads().size();

        // setup and start RocketMQ
        startMQ();
    }

    /**
     * Add RocketMQ properties to {@link Configuration}
     *
     * @param hbaseConf
     */
    private void addRocketMQProperties(Configuration hbaseConf) {
        hbaseConf.set("rocketmq.namesrv.addr", NAMESERVER);
        hbaseConf.set("rocketmq.topic", ROCKETMQ_TOPIC);
        hbaseConf.set("rocketmq.hbase.tables", TABLE_NAME_STR);
    }

    /**
     * @param configuration
     * @param peerName
     * @param tableCFs
     * @throws ReplicationException
     * @throws IOException
     */
    private void addPeer(final Configuration configuration, String peerName, Map<TableName, List<String>> tableCFs)
        throws ReplicationException, IOException {
        try (ReplicationAdmin replicationAdmin = new ReplicationAdmin(configuration)) {
            ReplicationPeerConfig peerConfig = new ReplicationPeerConfig()
                .setClusterKey(ZKConfig.getZooKeeperClusterKey(configuration))
                .setReplicationEndpointImpl(Replicator.class.getName());

            replicationAdmin.addPeer(peerName, peerConfig, tableCFs);
        }
    }

    /**
     * This method starts the RocketMQ server.
     *
     * @throws Exception
     */
    private static void startMQ() throws Exception {
        startNamesrv();
        startBroker();

        Thread.sleep(2000);
    }

    /**
     * This method starts the RocketMQ nameserver.
     *
     * @throws Exception
     */
    private static void startNamesrv() throws Exception {

        final NamesrvConfig namesrvConfig = new NamesrvConfig();
        NettyServerConfig nettyServerConfig = new NettyServerConfig();
        nettyServerConfig.setListenPort(9876);

        namesrvController = new NamesrvController(namesrvConfig, nettyServerConfig);
        final boolean initResult = namesrvController.initialize();
        if (!initResult) {
            namesrvController.shutdown();
            throw new Exception("Name server controller failed to initialize.");
        }
        namesrvController.start();
    }

    /**
     * This method starts the RocketMQ broker.
     *
     * @throws Exception
     */
    private static void startBroker() throws Exception {
        System.setProperty(RemotingCommand.REMOTING_VERSION_KEY, Integer.toString(MQVersion.CURRENT_VERSION));

        final BrokerConfig brokerConfig = new BrokerConfig();
        brokerConfig.setNamesrvAddr(NAMESERVER);
        brokerConfig.setBrokerId(MixAll.MASTER_ID);
        final NettyServerConfig nettyServerConfig = new NettyServerConfig();
        nettyServerConfig.setListenPort(10911);
        final NettyClientConfig nettyClientConfig = new NettyClientConfig();
        final MessageStoreConfig messageStoreConfig = new MessageStoreConfig();

        brokerController = new BrokerController(brokerConfig, nettyServerConfig, nettyClientConfig, messageStoreConfig);
        boolean initResult = brokerController.initialize();
        if (!initResult) {
            brokerController.shutdown();
            throw new Exception();
        }
        brokerController.start();
    }

    /**
     * This method tests the replicator by writing data from hbase to rocketmq and reading it back.
     *
     * @throws IOException
     * @throws ReplicationException
     * @throws InterruptedException
     * @throws MQClientException
     * @throws RemotingException
     * @throws MQBrokerException
     */
    @Test
    public void testCustomReplicationEndpoint() throws IOException, ReplicationException, InterruptedException,
        MQClientException, RemotingException, MQBrokerException {

        final DefaultMQPullConsumer consumer = new DefaultMQPullConsumer(CONSUMER_GROUP_NAME);
        try {
            createTestTable();

            final Map<TableName, List<String>> tableCfs = new HashMap<>();
            List<String> cfs = new ArrayList<>();
            cfs.add(COLUMN_FAMILY);
            tableCfs.put(TABLE_NAME, cfs);
            addPeer(utility.getConfiguration(), PEER_NAME, tableCfs);

            // wait for new peer to be added
            Thread.sleep(500);

            final int numberOfRecords = 10;
            final Transaction inTransaction = insertData(numberOfRecords);

            // wait for data to be replicated
            Thread.sleep(500);

            consumer.setNamesrvAddr(NAMESERVER);
            consumer.setMessageModel(MessageModel.valueOf("BROADCASTING"));
            consumer.registerMessageQueueListener(ROCKETMQ_TOPIC, null);
            consumer.start();

            int receiveNum = 0;
            String receiveMsg = null;
            Set<MessageQueue> queues = consumer.fetchSubscribeMessageQueues(ROCKETMQ_TOPIC);
            for (MessageQueue queue : queues) {
                long offset = getMessageQueueOffset(consumer, queue);
                PullResult pullResult = consumer.pull(queue, null, offset, batchSize);

                if (pullResult.getPullStatus() == PullStatus.FOUND) {
                    for (MessageExt message : pullResult.getMsgFoundList()) {
                        byte[] body = message.getBody();
                        receiveMsg = new String(body, "UTF-8");
                        // String[] receiveMsgKv = receiveMsg.split(",");
                        // msgs.remove(receiveMsgKv[1]);
                        logger.info("receive message : {}", receiveMsg);
                        receiveNum++;
                    }
                    long nextBeginOffset = pullResult.getNextBeginOffset();
                    consumer.updateConsumeOffset(queue, offset);
                }
            }
            logger.info("receive message num={}", receiveNum);

            // wait for processQueueTable init
            Thread.sleep(1000);

            assertEquals(inTransaction.toJson(), receiveMsg);
        } finally {
            removePeer();
            consumer.shutdown();
        }
    }

    /**
     * Gets the message queue offset.
     *
     * @param consumer the rocketmq consumer
     * @param queue the queue from where to consume the message
     * @return the offset
     * @throws MQClientException
     */
    private long getMessageQueueOffset(DefaultMQPullConsumer consumer, MessageQueue queue) throws MQClientException {
        long offset = consumer.fetchConsumeOffset(queue, false);
        if (offset < 0) {
            offset = 0;
        }
        return offset;
    }

    /**
     * Creates the HBase table with a scope set to global.
     *
     * @throws IOException
     */
    private void createTestTable() throws IOException {
        try (HBaseAdmin hBaseAdmin = utility.getHBaseAdmin()) {
            final HTableDescriptor hTableDescriptor = new HTableDescriptor(TABLE_NAME);
            final HColumnDescriptor hColumnDescriptor = new HColumnDescriptor(COLUMN_FAMILY);
            hColumnDescriptor.setScope(HConstants.REPLICATION_SCOPE_GLOBAL);
            hTableDescriptor.addFamily(hColumnDescriptor);
            hBaseAdmin.createTable(hTableDescriptor);
        }
        utility.waitUntilAllRegionsAssigned(TABLE_NAME);
    }

    /**
     * Adds data to the previously created HBase table.
     *
     * @throws IOException
     */
    private Transaction insertData(int numberOfRecords) throws IOException {
        final Transaction transaction = new Transaction(numberOfRecords);
        try (Table hTable = ConnectionFactory.createConnection(utility.getConfiguration()).getTable(TABLE_NAME)) {
            for (int i = 0; i < numberOfRecords; i++) {
                final byte[] rowKey = toBytes(String.format(ROWKEY, i));
                final byte[] family = toBytes(COLUMN_FAMILY);
                final Put put = new Put(rowKey);
                put.addColumn(family, toBytes(QUALIFIER), toBytes(VALUE));
                hTable.put(put);

                List<Cell> cells = put.getFamilyCellMap().get(family);
                transaction.addRow(TABLE_NAME_STR, rowKey, cells);
            }
        }
        return transaction;
    }

    /**
     * Removes the HBase peer.
     *
     * @throws IOException
     * @throws ReplicationException
     */
    private void removePeer() throws IOException, ReplicationException {
        try (ReplicationAdmin replicationAdmin = new ReplicationAdmin(utility.getConfiguration())) {
            replicationAdmin.removePeer(PEER_NAME);
        }
    }

    /**
     * Shuts down the HBase cluster and the RocketMQ server.
     *
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        if (utility != null) {
            utility.shutdownMiniCluster();
        }

        if (brokerController != null) {
            brokerController.shutdown();
        }

        if (namesrvController != null) {
            namesrvController.shutdown();
        }
    }
}
