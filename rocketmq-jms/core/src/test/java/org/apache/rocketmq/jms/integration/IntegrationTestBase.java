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

import org.apache.rocketmq.broker.BrokerController;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.BrokerConfig;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.namesrv.NamesrvConfig;
import org.apache.rocketmq.jms.domain.CommonConstant;
import org.apache.rocketmq.namesrv.NamesrvController;
import org.apache.rocketmq.remoting.netty.NettyClientConfig;
import org.apache.rocketmq.remoting.netty.NettyServerConfig;
import org.apache.rocketmq.store.config.MessageStoreConfig;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntegrationTestBase {
    public static Logger logger = LoggerFactory.getLogger(IntegrationTestBase.class);

    protected static Random random = new Random();
    protected static final String SEP = File.separator;


    protected static String topic = "jms-test";
    protected static String topic2 = "jms-test-2";
    protected static String messageType = "TagA";
    protected static String producerId = "PID-jms-test";
    protected static String consumerId = "CID-jms-test";
    protected static String consumerId2 = "CID-jms-test-2";
    protected static String nameServer;
    protected static String text = "English test";
    protected static int consumeThreadNums = 16;




    protected static final String BROKER_NAME_PREFIX = "TestBrokerName_";
    protected static final AtomicInteger BROKER_INDEX = new AtomicInteger(0);
    protected static final List<File> TMPE_FILES = new ArrayList<File>();
    protected static final List<BrokerController> BROKER_CONTROLLERS =  new ArrayList<BrokerController>();
    protected static final List<NamesrvController> NAMESRV_CONTROLLERS = new ArrayList<NamesrvController>();


    private static String createBaseDir() {
        String baseDir = System.getProperty("user.home") + SEP + "unitteststore-" + UUID.randomUUID();
        final File file = new File(baseDir);
        if (file.exists()) {
            System.out.println(String.format("[%s] has already existed, please bake up and remove it for integration tests", baseDir));
            System.exit(1);
        }
        TMPE_FILES.add(file);
        return baseDir;
    }

    public static NamesrvController createAndStartNamesrv() {
        String baseDir = createBaseDir();
        NamesrvConfig namesrvConfig = new NamesrvConfig();
        NettyServerConfig nameServerNettyServerConfig = new NettyServerConfig();
        namesrvConfig.setKvConfigPath(baseDir + SEP + "namesrv" + SEP + "kvConfig.json");

        nameServerNettyServerConfig.setListenPort(9000 + random.nextInt(1000));
        NamesrvController namesrvController = new NamesrvController(namesrvConfig, nameServerNettyServerConfig);
        try {
            Assert.assertTrue(namesrvController.initialize());
            logger.info("Name Server Start:{}", nameServerNettyServerConfig.getListenPort());
            namesrvController.start();
        } catch (Exception e) {
            System.out.println("Name Server start failed");
            System.exit(1);
        }
        NAMESRV_CONTROLLERS.add(namesrvController);
        return namesrvController;

    }


    public static BrokerController createAndStartBroker(String nsAddr) {
        String baseDir = createBaseDir();
        BrokerConfig brokerConfig = new BrokerConfig();
        NettyServerConfig nettyServerConfig = new NettyServerConfig();
        NettyClientConfig nettyClientConfig = new NettyClientConfig();
        MessageStoreConfig storeConfig = new MessageStoreConfig();
        brokerConfig.setBrokerName(BROKER_NAME_PREFIX + BROKER_INDEX.getAndIncrement());
        brokerConfig.setBrokerIP1("127.0.0.1");
        brokerConfig.setNamesrvAddr(nsAddr);
        storeConfig.setStorePathRootDir(baseDir);
        storeConfig.setStorePathCommitLog(baseDir + SEP + "commitlog");
        storeConfig.setHaListenPort(8000 + random.nextInt(1000));
        nettyServerConfig.setListenPort(10000 + random.nextInt(1000));
        BrokerController brokerController = new BrokerController(brokerConfig, nettyServerConfig, nettyClientConfig, storeConfig);
        try {
            Assert.assertTrue(brokerController.initialize());
            logger.info("Broker Start name:{} addr:{}", brokerConfig.getBrokerName(), brokerController.getBrokerAddr());
            brokerController.start();
        } catch (Exception e) {
            System.out.println("Broker start failed");
            System.exit(1);
        }
        BROKER_CONTROLLERS.add(brokerController);
        return brokerController;
    }



    protected static DefaultMQAdminExt defaultMQAdminExt;

    static {
        //clear the environment
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override public void run() {
                if (defaultMQAdminExt != null) {
                    defaultMQAdminExt.shutdown();
                }
                for (NamesrvController namesrvController: NAMESRV_CONTROLLERS) {
                    if (namesrvController != null) {
                        namesrvController.shutdown();
                    }
                }
                for (BrokerController brokerController: BROKER_CONTROLLERS) {
                    if (brokerController != null) {
                        brokerController.shutdown();
                    }
                }
                for (File file : TMPE_FILES) {
                    deleteFile(file);
                }
            }
        });


        NamesrvController namesrvController = IntegrationTestBase.createAndStartNamesrv();
        nameServer = "127.0.0.1:" + namesrvController.getNettyServerConfig().getListenPort();
        BrokerController brokerController = createAndStartBroker(nameServer);

        defaultMQAdminExt = new DefaultMQAdminExt();
        defaultMQAdminExt.setNamesrvAddr(nameServer);
        try {
            defaultMQAdminExt.start();
        } catch (MQClientException e) {
            System.out.println("DefaultMQAdminExt start failed");
            System.exit(1);
        }

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
