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

package org.apache.rocketmq.jms.integration;

import com.alibaba.rocketmq.broker.BrokerController;
import com.alibaba.rocketmq.common.BrokerConfig;
import com.alibaba.rocketmq.common.MixAll;
import com.alibaba.rocketmq.common.namesrv.NamesrvConfig;
import com.alibaba.rocketmq.namesrv.NamesrvController;
import com.alibaba.rocketmq.remoting.netty.NettyClientConfig;
import com.alibaba.rocketmq.remoting.netty.NettyServerConfig;
import com.alibaba.rocketmq.store.config.MessageStoreConfig;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static java.io.File.separator;
import static org.apache.rocketmq.jms.integration.Constant.BROKER_HA_PORT;
import static org.apache.rocketmq.jms.integration.Constant.BROKER_PORT;
import static org.apache.rocketmq.jms.integration.Constant.NAME_SERVER_ADDRESS;

@Service
public class RocketMQServer {
    public static Logger log = LoggerFactory.getLogger(RocketMQServer.class);
    private final SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
    private final String rootDir = System.getProperty("user.home") + separator + "rocketmq-jms" + separator;
    // fixed location of config files which is updated after RMQ3.2.6
    private final String configDir = System.getProperty("user.home") + separator + "store/config";

    private String serverDir;
    private volatile boolean started = false;

    //name server
    private NamesrvConfig namesrvConfig = new NamesrvConfig();
    private NettyServerConfig nameServerNettyServerConfig = new NettyServerConfig();
    private NamesrvController namesrvController;

    //broker
    private final String brokerName = "JmsTestBrokerName";
    private BrokerController brokerController;
    private BrokerConfig brokerConfig = new BrokerConfig();
    private NettyServerConfig nettyServerConfig = new NettyServerConfig();
    private NettyClientConfig nettyClientConfig = new NettyClientConfig();
    private MessageStoreConfig storeConfig = new MessageStoreConfig();

    public RocketMQServer() {
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
            serverDir = rootDir + sf.format(new Date());
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
        nameServerNettyServerConfig.setListenPort(Constant.NAME_SERVER_PORT);
        namesrvController = new NamesrvController(namesrvConfig, nameServerNettyServerConfig);
        try {
            namesrvController.initialize();
            log.info("Success to start Name Server:{}", NAME_SERVER_ADDRESS);
            namesrvController.start();
        }
        catch (Exception e) {
            log.error("Failed to start Name Server", e);
            System.exit(1);
        }
        System.setProperty(MixAll.NAMESRV_ADDR_PROPERTY, NAME_SERVER_ADDRESS);
    }

    private void startBroker() {
        brokerConfig.setBrokerName(brokerName);
        brokerConfig.setBrokerIP1(Constant.BROKER_IP);
        brokerConfig.setNamesrvAddr(NAME_SERVER_ADDRESS);
        storeConfig.setStorePathRootDir(serverDir);
        storeConfig.setStorePathCommitLog(serverDir + separator + "commitlog");
        storeConfig.setHaListenPort(BROKER_HA_PORT);
        nettyServerConfig.setListenPort(BROKER_PORT);
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
        deleteFile(new File(rootDir));
        deleteFile(new File(configDir));
    }

    public void deleteFile(File file) {
        if (!file.exists()) {
            return;
        }
        if (file.isFile()) {
            file.delete();
        }
        else if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                deleteFile(files[i]);
            }
            file.delete();
        }
    }

}
