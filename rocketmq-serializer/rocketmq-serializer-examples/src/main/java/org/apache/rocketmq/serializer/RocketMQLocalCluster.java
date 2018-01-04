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
package org.apache.rocketmq.serializer;

import org.apache.rocketmq.broker.BrokerController;
import org.apache.rocketmq.common.BrokerConfig;
import org.apache.rocketmq.common.MQVersion;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.namesrv.NamesrvConfig;
import org.apache.rocketmq.namesrv.NamesrvController;
import org.apache.rocketmq.remoting.netty.NettyClientConfig;
import org.apache.rocketmq.remoting.netty.NettyServerConfig;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.store.config.MessageStoreConfig;

public class RocketMQLocalCluster {
    private NamesrvController namesrvController;
    private BrokerController brokerController;
    private String nameserverAddr = "localhost:9876";

    public void startupServer() throws Exception {
        //start nameserver
        startNamesrv();
        //start broker
        startBroker();
    }

    public void shutdownServer() {
        if (brokerController != null) {
            brokerController.shutdown();
        }

        if (namesrvController != null) {
            namesrvController.shutdown();
        }
    }

    public String getNameServerAddr() {
        return nameserverAddr;
    }

    private void startNamesrv() throws Exception {
        NamesrvConfig namesrvConfig = new NamesrvConfig();
        NettyServerConfig nettyServerConfig = new NettyServerConfig();
        nettyServerConfig.setListenPort(9876);

        namesrvController = new NamesrvController(namesrvConfig, nettyServerConfig);
        boolean initResult = namesrvController.initialize();
        if (!initResult) {
            namesrvController.shutdown();
            throw new Exception("Namesvr init failure!");
        }
        namesrvController.start();
    }

    private void startBroker() throws Exception {
        System.setProperty(RemotingCommand.REMOTING_VERSION_KEY, Integer.toString(MQVersion.CURRENT_VERSION));

        BrokerConfig brokerConfig = new BrokerConfig();
        brokerConfig.setNamesrvAddr(nameserverAddr);
        brokerConfig.setBrokerId(MixAll.MASTER_ID);
        NettyServerConfig nettyServerConfig = new NettyServerConfig();
        nettyServerConfig.setListenPort(10911);
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

    public static void main(String[] args) throws Exception {
        new RocketMQLocalCluster().startupServer();
    }
}
