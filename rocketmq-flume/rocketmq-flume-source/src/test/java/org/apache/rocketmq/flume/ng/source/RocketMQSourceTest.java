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

package org.apache.rocketmq.flume.ng.source;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.flume.Channel;
import org.apache.flume.ChannelSelector;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.PollableSource;
import org.apache.flume.Transaction;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.channel.MemoryChannel;
import org.apache.flume.channel.ReplicatingChannelSelector;
import org.apache.flume.conf.Configurables;
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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.rocketmq.flume.ng.source.RocketMQSourceConstants.NAME_SERVER_CONFIG;
import static org.apache.rocketmq.flume.ng.source.RocketMQSourceConstants.TAG_CONFIG;
import static org.apache.rocketmq.flume.ng.source.RocketMQSourceConstants.TAG_DEFAULT;
import static org.apache.rocketmq.flume.ng.source.RocketMQSourceConstants.TOPIC_DEFAULT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 *
 */
public class RocketMQSourceTest {

    private static final Logger log = LoggerFactory.getLogger(RocketMQSourceTest.class);

    private static String nameServer = "localhost:9876";

    private static NamesrvController namesrvController;
    private static BrokerController brokerController;

    private String tag = TAG_DEFAULT + "_SOURCE_TEST_" + new Random().nextInt(99);
    private String producerGroup = "PRODUCER_GROUP_SOURCE_TEST";

    @BeforeClass
    public static void startMQ() throws Exception {

        /*
        start nameserver
         */
        startNamesrv();

        /*
        start broker
         */
        startBroker();

        Thread.sleep(2000);
    }

    private static void startNamesrv() throws Exception {

        NamesrvConfig namesrvConfig = new NamesrvConfig();
        NettyServerConfig nettyServerConfig = new NettyServerConfig();
        nettyServerConfig.setListenPort(9876);

        namesrvController = new NamesrvController(namesrvConfig, nettyServerConfig);
        boolean initResult = namesrvController.initialize();
        if (!initResult) {
            namesrvController.shutdown();
            throw new Exception();
        }
        namesrvController.start();
    }

    private static void startBroker() throws Exception {

        System.setProperty(RemotingCommand.REMOTING_VERSION_KEY, Integer.toString(MQVersion.CURRENT_VERSION));

        BrokerConfig brokerConfig = new BrokerConfig();
        brokerConfig.setNamesrvAddr(nameServer);
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

    @Test
    public void testEvent() throws EventDeliveryException, MQBrokerException, MQClientException, InterruptedException, UnsupportedEncodingException, RemotingException {
        // publish  message before flume-source start
        DefaultMQProducer producer = new DefaultMQProducer(producerGroup);
        producer.setNamesrvAddr(nameServer);
        String sendMsg = "\"Hello Flume\"" + "," + DateFormatUtils.format(new Date(), "yyyy-MM-dd hh:mm:ss");
        producer.start();
        Message msg = new Message(TOPIC_DEFAULT, tag, sendMsg.getBytes("UTF-8"));
        SendResult sendResult = producer.send(msg);

        // start source
        Context context = new Context();
        context.put(NAME_SERVER_CONFIG, nameServer);
        context.put(TAG_CONFIG, tag);
        Channel channel = new MemoryChannel();
        Configurables.configure(channel, context);
        List<Channel> channels = new ArrayList<>();
        channels.add(channel);
        ChannelSelector channelSelector = new ReplicatingChannelSelector();
        channelSelector.setChannels(channels);
        ChannelProcessor channelProcessor = new ChannelProcessor(channelSelector);

        RocketMQSource source = new RocketMQSource();
        source.setChannelProcessor(channelProcessor);
        Configurables.configure(source, context);

        source.start();

        // wait for rebalanceImpl start
        Thread.sleep(2000);

        sendMsg = "\"Hello Flume\"" + "," + DateFormatUtils.format(new Date(), "yyyy-MM-dd hh:mm:ss");
        msg = new Message(TOPIC_DEFAULT, tag, sendMsg.getBytes("UTF-8"));
        sendResult = producer.send(msg);
        log.info("publish message : {}, sendResult:{}", sendMsg, sendResult);

        PollableSource.Status status = source.process();
        if (status == PollableSource.Status.BACKOFF) {
            fail("Error");
        }
        /*
        wait for processQueueTable init
         */
        Thread.sleep(1000);

        producer.shutdown();
        source.stop();


        /*
        mock flume sink
         */
        Transaction transaction = channel.getTransaction();
        transaction.begin();
        Event event = channel.take();
        if (event == null) {
            transaction.commit();
            fail("Error");
        }
        byte[] body = event.getBody();
        String receiveMsg = new String(body, "UTF-8");
        log.info("receive message : {}", receiveMsg);

        assertEquals(sendMsg, receiveMsg);
    }

    @AfterClass
    public static void stop() {

        if (brokerController != null) {
            brokerController.shutdown();
        }

        if (namesrvController != null) {
            namesrvController.shutdown();
        }
    }
}
