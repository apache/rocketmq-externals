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
import java.io.UnsupportedEncodingException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.rocketmq.broker.BrokerController;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.BrokerConfig;
import org.apache.rocketmq.common.MQVersion;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.namesrv.NamesrvConfig;
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

import static org.apache.hadoop.hbase.util.Bytes.toBytes;
import static org.junit.Assert.assertEquals;

/**
 * This class tests the RocketMQ source by writing data from RocketMQ to HBase and reading it back.
 */
public class RocketMQSourceTest {

    private static final String NAMESERVER = "localhost:9876";

    private static final String PRODUCER_GROUP_NAME = "HBASE_PRODUCER_GROUP_TEST";

    private static final String ROCKETMQ_TOPIC = "rocketmq-hbase-topic-test";

    private static final TableName TABLE_NAME = TableName.valueOf(ROCKETMQ_TOPIC);

    private static final Logger logger = LoggerFactory.getLogger(RocketMQSourceTest.class);

    private static NamesrvController namesrvController;

    private static BrokerController brokerController;

    private HBaseTestingUtility utility;

    private Configuration hbaseConf;

    @Before
    public void setUp() throws Exception {
        // start hbase server
        final Configuration config = HBaseConfiguration.create();
        utility = new HBaseTestingUtility(config);
        utility.startMiniCluster();
        hbaseConf = utility.getConfiguration();

        // create HBase table
        createTable();

        // start rocketmq server
        startMQ();
    }

    @Test
    public void testRocketMQSource() throws Exception {
        // write data to rocketmq
        final String inMsg = "test-rocketmq-hbase-" + System.currentTimeMillis();
        final String msgId;
        try {
            msgId = writeData(inMsg);
            logger.info("Message written to rocketmq (msgID: " + msgId + ").");
        } catch (Exception e) {
            logger.error("Error while writing data.", e);
            throw new Exception("Error while writing data.", e);
        }

        // write data from rocketmq to hbase
        final Config config = new Config();
        config.setNameserver(NAMESERVER);
        config.setTopics(ROCKETMQ_TOPIC);
        config.setZookeeperPort(utility.getZkCluster().getClientPort());
        config.setPullInterval(0);

        final MessageProcessor messageProcessor = new MessageProcessor(config);
        try {
            messageProcessor.start();
        } catch (Exception e) {
            logger.error("Error while starting message processor.", e);
            throw new Exception("Error while starting message processor.", e);
        }
        Thread.sleep(1000);
        messageProcessor.stop();
        logger.info("Message processor completed successfully.");

        // read data from hbase
        final String readMsg;
        try {
            readMsg = readData(msgId);
            logger.info("Message read from HBase (msg: " + readMsg + ").");
        } catch (Exception e) {
            logger.error("Error while reading data.", e);
            throw new Exception("Error while reading data.", e);
        }

        assertEquals(inMsg, readMsg);
    }

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

    /**
     * Creates the HBase table.
     *
     * @throws IOException
     */
    private void createTable() throws IOException {
        try (HBaseAdmin hBaseAdmin = utility.getHBaseAdmin()) {
            final HTableDescriptor hTableDescriptor = new HTableDescriptor(TABLE_NAME);
            final HColumnDescriptor hColumnDescriptor = new HColumnDescriptor(HBaseClient.COLUMN_FAMILY);
            hTableDescriptor.addFamily(hColumnDescriptor);
            hBaseAdmin.createTable(hTableDescriptor);
        }
        utility.waitUntilAllRegionsAssigned(TABLE_NAME);
    }

    /**
     * @param inMsg
     * @return
     * @throws MQClientException
     * @throws UnsupportedEncodingException
     * @throws RemotingException
     * @throws InterruptedException
     * @throws MQBrokerException
     */
    private String writeData(String inMsg) throws MQClientException, UnsupportedEncodingException, RemotingException,
        InterruptedException, MQBrokerException {

        final DefaultMQProducer producer = new DefaultMQProducer(PRODUCER_GROUP_NAME);
        producer.setNamesrvAddr(NAMESERVER);

        try {
            producer.start();

            final Message msg = new Message(ROCKETMQ_TOPIC, null, inMsg.getBytes("UTF-8"));
            final SendResult sendResult = producer.send(msg);
            logger.info("publish message : {}, sendResult:{}", msg, sendResult);
            return sendResult.getMsgId();
        } finally {
            producer.shutdown();
        }
    }

    /**
     * @param row
     * @return
     * @throws IOException
     */
    private String readData(String row) throws IOException {
        try (Table hTable = ConnectionFactory.createConnection(hbaseConf).getTable(TABLE_NAME)) {
            final Get get = new Get(toBytes(row));
            get.addFamily(HBaseClient.COLUMN_FAMILY);
            final Result result = hTable.get(get);
            final String resultStr = Bytes.toString(result.getValue(HBaseClient.COLUMN_FAMILY, null));
            return resultStr;
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

        NamesrvConfig namesrvConfig = new NamesrvConfig();
        NettyServerConfig nettyServerConfig = new NettyServerConfig();
        nettyServerConfig.setListenPort(9876);

        namesrvController = new NamesrvController(namesrvConfig, nettyServerConfig);
        boolean initResult = namesrvController.initialize();
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

        BrokerConfig brokerConfig = new BrokerConfig();
        brokerConfig.setNamesrvAddr(NAMESERVER);
        brokerConfig.setBrokerId(MixAll.MASTER_ID);
        NettyServerConfig nettyServerConfig = new NettyServerConfig();
        nettyServerConfig.setListenPort(10911);
        NettyClientConfig nettyClientConfig = new NettyClientConfig();
        MessageStoreConfig messageStoreConfig = new MessageStoreConfig();

        brokerController = new BrokerController(brokerConfig, nettyServerConfig, nettyClientConfig, messageStoreConfig);
        boolean initResult = brokerController.initialize();
        if (!initResult) {
            brokerController.shutdown();
            throw new Exception();
        }
        brokerController.start();
    }

}
