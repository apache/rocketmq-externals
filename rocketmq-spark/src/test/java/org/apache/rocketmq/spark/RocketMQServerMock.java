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
package org.apache.rocketmq.spark;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.rocketmq.broker.BrokerController;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.BrokerConfig;
import org.apache.rocketmq.common.MQVersion;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.namesrv.NamesrvConfig;
import org.apache.rocketmq.namesrv.NamesrvController;
import org.apache.rocketmq.remoting.netty.NettyClientConfig;
import org.apache.rocketmq.remoting.netty.NettyServerConfig;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.store.config.MessageStoreConfig;

import java.util.Date;
import java.util.UUID;

public class RocketMQServerMock {

    private NamesrvController nameServerController;
    private BrokerController brokerController;
    private int nameServerPort;
    private int brokerPort;

    public RocketMQServerMock(int nameServerPort, int brokerPort) {
        this.nameServerPort = nameServerPort;
        this.brokerPort = brokerPort;
    }

    public void startupServer() throws Exception{
        //start nameserver
        startNameServer();
        //start broker
        startBroker();
    }

    public void shutdownServer() {
        if (brokerController != null) {
            brokerController.shutdown();
        }

        if (nameServerController != null) {
            nameServerController.shutdown();
        }
    }

    public String getNameServerAddr() {
        return "localhost:" + nameServerPort;
    }

    private void startNameServer() throws Exception {
        NamesrvConfig namesrvConfig = new NamesrvConfig();
        NettyServerConfig nettyServerConfig = new NettyServerConfig();
        nettyServerConfig.setListenPort(nameServerPort);

        nameServerController = new NamesrvController(namesrvConfig, nettyServerConfig);
        boolean initResult = nameServerController.initialize();
        if (!initResult) {
            nameServerController.shutdown();
            throw new Exception("Namesvr init failure!");
        }
        nameServerController.start();
    }

    private void startBroker() throws Exception {
        System.setProperty(RemotingCommand.REMOTING_VERSION_KEY, Integer.toString(MQVersion.CURRENT_VERSION));

        BrokerConfig brokerConfig = new BrokerConfig();
        brokerConfig.setNamesrvAddr(getNameServerAddr());
        brokerConfig.setBrokerId(MixAll.MASTER_ID);
        NettyServerConfig nettyServerConfig = new NettyServerConfig();
        nettyServerConfig.setListenPort(brokerPort);
        NettyClientConfig nettyClientConfig = new NettyClientConfig();
        MessageStoreConfig messageStoreConfig = new MessageStoreConfig();

        brokerController = new BrokerController(brokerConfig, nettyServerConfig, nettyClientConfig, messageStoreConfig);
        boolean initResult = brokerController.initialize();
        if (!initResult) {
            brokerController.shutdown();
            throw new Exception("Broker init failure!");
        }
        brokerController.start();
    }

    public void prepareDataTo(String topic, int times) throws Exception {
        // publish test message
        DefaultMQProducer producer = new DefaultMQProducer(UUID.randomUUID().toString());
        producer.setNamesrvAddr(getNameServerAddr());

        String sendMsg = "\"Hello Rocket\"" + "," + DateFormatUtils.format(new Date(), "yyyy-MM-DD hh:mm:ss");

        try {
            producer.start();
            for (int i = 0; i < times; i++) {
                producer.send(new Message(topic, sendMsg.getBytes("UTF-8")));
            }
        } catch (Exception e) {
            throw new MQClientException("Failed to publish messages", e);
        } finally {
            producer.shutdown();
        }
    }
}
