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

package org.apache.rocketmq.console.testbase;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.rocketmq.broker.BrokerController;
import org.apache.rocketmq.common.BrokerConfig;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.namesrv.NamesrvConfig;
import org.apache.rocketmq.namesrv.NamesrvController;
import org.apache.rocketmq.remoting.netty.NettyClientConfig;
import org.apache.rocketmq.remoting.netty.NettyServerConfig;
import org.apache.rocketmq.store.config.MessageStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static java.io.File.separator;
import static org.apache.rocketmq.console.testbase.TestConstant.TEST_BROKER_NAME;
import static org.apache.rocketmq.console.testbase.TestConstant.TEST_CLUSTER_NAME;
import static org.apache.rocketmq.console.testbase.TestConstant.TEST_FILE_ROOT_DIR;

@Service
public class TestRocketMQServer {
    public static Logger log = LoggerFactory.getLogger(TestRocketMQServer.class);
    private final SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");

    private String serverDir;
    private volatile boolean started = false;

    //name server
    private NamesrvConfig namesrvConfig = new NamesrvConfig();
    private NettyServerConfig nameServerNettyServerConfig = new NettyServerConfig();
    private NamesrvController namesrvController;

    //broker
    private BrokerController brokerController;
    private BrokerConfig brokerConfig = new BrokerConfig();
    private NettyServerConfig nettyServerConfig = new NettyServerConfig();
    private NettyClientConfig nettyClientConfig = new NettyClientConfig();
    private MessageStoreConfig storeConfig = new MessageStoreConfig();

    public TestRocketMQServer() {
        this.storeConfig.setDiskMaxUsedSpaceRatio(95);
    }

    @PostConstruct
    public void start() {
        if (started) {
            return;
        }

        createServerDir();

        startNameServer();

        startBroker();

        started = true;

        log.info("Start RocketServer Successfully");
    }

    private void createServerDir() {
        for (int i = 0; i < 5; i++) {
            serverDir = TEST_FILE_ROOT_DIR + separator + sf.format(new Date());
            final File file = new File(serverDir);
            if (!file.exists()) {
                return;
            }
        }
        log.error("Has retry 5 times to register base dir,but still failed.");
        System.exit(1);
    }

    private void startNameServer() {
        namesrvConfig.setKvConfigPath(serverDir + separator + "namesrv" + separator + "kvConfig.json");
        nameServerNettyServerConfig.setListenPort(TestConstant.NAME_SERVER_PORT);
        namesrvController = new NamesrvController(namesrvConfig, nameServerNettyServerConfig);
        try {
            namesrvController.initialize();
            log.info("Success to start Name Server:{}", TestConstant.NAME_SERVER_ADDRESS);
            namesrvController.start();
        }
        catch (Exception e) {
            log.error("Failed to start Name Server", e);
            System.exit(1);
        }
        System.setProperty(MixAll.NAMESRV_ADDR_PROPERTY, TestConstant.NAME_SERVER_ADDRESS);
    }

    private void startBroker() {
        brokerConfig.setBrokerName(TEST_BROKER_NAME);
        brokerConfig.setBrokerClusterName(TEST_CLUSTER_NAME);
        brokerConfig.setBrokerIP1(TestConstant.BROKER_IP);
        brokerConfig.setNamesrvAddr(TestConstant.NAME_SERVER_ADDRESS);
        storeConfig.setStorePathRootDir(serverDir);
        storeConfig.setStorePathCommitLog(serverDir + separator + "commitlog");
        storeConfig.setHaListenPort(TestConstant.BROKER_HA_PORT);
        nettyServerConfig.setListenPort(TestConstant.BROKER_PORT);
        brokerController = new BrokerController(brokerConfig, nettyServerConfig, nettyClientConfig, storeConfig);

        try {
            brokerController.initialize();
            log.info("Broker Start name:{} address:{}", brokerConfig.getBrokerName(), brokerController.getBrokerAddr());
            brokerController.start();

        }
        catch (Exception e) {
            log.error("Failed to start Broker", e);
            System.exit(1);
        }
    }

    @PreDestroy
    private void shutdown() {
        brokerController.shutdown();
        namesrvController.shutdown();
        deleteFile(new File(TEST_FILE_ROOT_DIR));
    }

    private void deleteFile(File file) {
        if (!file.exists()) {
            return;
        }
        if (file.isFile()) {
            file.delete();
        }
        else if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File file1 : files) {
                deleteFile(file1);
            }
            file.delete();
        }
    }

}
