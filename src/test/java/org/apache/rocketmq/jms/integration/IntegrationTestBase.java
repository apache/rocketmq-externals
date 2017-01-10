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

package org.apache.rocketmq.jms.integration;

import com.alibaba.rocketmq.broker.BrokerController;
import com.alibaba.rocketmq.common.BrokerConfig;
import com.alibaba.rocketmq.common.MixAll;
import com.alibaba.rocketmq.common.TopicConfig;
import com.alibaba.rocketmq.common.namesrv.NamesrvConfig;
import com.alibaba.rocketmq.namesrv.NamesrvController;
import com.alibaba.rocketmq.remoting.netty.NettyClientConfig;
import com.alibaba.rocketmq.remoting.netty.NettyServerConfig;
import com.alibaba.rocketmq.store.config.MessageStoreConfig;
import com.alibaba.rocketmq.tools.admin.DefaultMQAdminExt;
import java.io.File;
import java.util.Random;
import java.util.UUID;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntegrationTestBase {
    public static Logger logger = LoggerFactory.getLogger(IntegrationTestBase.class);

    protected static Random random = new Random();
    protected static final String SEP = File.separator;

    protected static String topic = "jms-test";
    protected static String messageType = "TagA";
    protected static String producerId = "PID-jms-test";
    protected static String consumerId = "CID-jms-test";
    protected static String nameServer;
    protected static String text = "English test";



    //broker
    protected static final String BROKER_NAME = "JmsTestBrokerName";
    protected static BrokerController brokerController;
    protected static BrokerConfig brokerConfig = new BrokerConfig();
    protected static NettyServerConfig nettyServerConfig = new NettyServerConfig();
    protected static NettyClientConfig nettyClientConfig = new NettyClientConfig();
    protected static MessageStoreConfig storeConfig = new MessageStoreConfig();

    protected static DefaultMQAdminExt defaultMQAdminExt = new DefaultMQAdminExt();


    //name server
    protected static NamesrvController namesrvController;

    protected static NamesrvConfig namesrvConfig  = new NamesrvConfig();
    protected static NettyServerConfig nameServerNettyServerConfig = new NettyServerConfig();
    static {
        //refactor the code
        String baseDir = System.getProperty("user.home") + SEP + "unitteststore-" + UUID.randomUUID();
        final File file = new File(baseDir);
        if (file.exists()) {
            System.out.println(String.format("[%s] has already existed, please bake up and remove it for integration tests", baseDir));
            System.exit(1);
        }


        namesrvConfig.setKvConfigPath(baseDir + SEP + "namesrv" + SEP + "kvConfig.json");
        nameServerNettyServerConfig.setListenPort(9000 + random.nextInt(1000));
        namesrvController = new NamesrvController(namesrvConfig, nameServerNettyServerConfig);
        try {
            Assert.assertTrue(namesrvController.initialize());
            logger.info("Name Server Start:{}", nameServerNettyServerConfig.getListenPort());
            namesrvController.start();
        } catch (Exception e) {
            System.out.println("Name Server start failed");
            System.exit(1);
        }
        nameServer = "127.0.0.1:" + nameServerNettyServerConfig.getListenPort();
        System.setProperty(MixAll.NAMESRV_ADDR_PROPERTY, nameServer);

        brokerConfig.setBrokerName(BROKER_NAME);
        brokerConfig.setBrokerIP1("127.0.0.1");
        brokerConfig.setNamesrvAddr(nameServer);
        storeConfig.setStorePathRootDir(baseDir);
        storeConfig.setStorePathCommitLog(baseDir + SEP + "commitlog");
        storeConfig.setHaListenPort(8000 + random.nextInt(1000));
        nettyServerConfig.setListenPort(10000 + random.nextInt(1000));
        brokerController = new BrokerController(brokerConfig, nettyServerConfig, nettyClientConfig, storeConfig);
        defaultMQAdminExt.setNamesrvAddr(nameServer);
        try {
            Assert.assertTrue(brokerController.initialize());
            logger.info("Broker Start name:{} addr:{}", brokerConfig.getBrokerName(), brokerController.getBrokerAddr());
            brokerController.start();
            defaultMQAdminExt.start();
        } catch (Exception e) {
            System.out.println("Broker start failed");
            System.exit(1);
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override public void run() {
                defaultMQAdminExt.shutdown();
                brokerController.shutdown();
                namesrvController.shutdown();
                deleteFile(file);
            }
        });

        createTopic(topic, brokerController.getBrokerAddr());
    }

    public static void deleteFile(File file) {
        if (!file.exists()) {
            return;
        }
        if (file.isFile()) {
            file.delete();
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0;i < files.length;i ++) {
                deleteFile(files[i]);
            }
            file.delete();
        }
    }
    public static void createTopic(String topic, String addr) {
        TopicConfig topicConfig = new TopicConfig();
        topicConfig.setTopicName(topic);
        topicConfig.setReadQueueNums(4);
        topicConfig.setWriteQueueNums(4);
        try {
            defaultMQAdminExt.createAndUpdateTopicConfig(addr, topicConfig);
        } catch (Exception e) {
            logger.error("Create topic:{} addr:{} failed", addr, topic);
        }
    }


}
