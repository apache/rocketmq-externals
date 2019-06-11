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

package org.apache.rocketmq.connect.runtime.service;

import io.openmessaging.Future;
import io.openmessaging.producer.SendResult;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.connect.runtime.config.ConnectConfig;
import org.apache.rocketmq.connect.runtime.utils.datasync.BrokerBasedLog;
import org.apache.rocketmq.connect.runtime.utils.datasync.DataSynchronizerCallback;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ClusterManagementServiceImplTest {

    private ConnectConfig connectConfig;

    @Mock
    private DefaultMQProducer producer;

    @Mock
    private DefaultMQPushConsumer consumer;

    @Mock
    private Future<SendResult> future;

    private ClusterManagementServiceImpl clusterManagementService;

    @Before
    public void init() throws RemotingException, MQClientException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        String consumerGroup = UUID.randomUUID().toString();
        String producerGroup = UUID.randomUUID().toString();

        connectConfig = new ConnectConfig();
        connectConfig.setHttpPort(8081);
        connectConfig.setStorePathRootDir(System.getProperty("user.home") + File.separator + "testConnectorStore");
        connectConfig.setWorkerId("testWorkerId");
        connectConfig.setRmqConsumerGroup(consumerGroup);
        connectConfig.setRmqProducerGroup(producerGroup);
        connectConfig.setNamesrvAddr("127.0.0.1:9876");
        connectConfig.setRmqMinConsumeThreadNums(1);
        connectConfig.setRmqMaxConsumeThreadNums(32);
        connectConfig.setRmqMessageConsumeTimeout(3 * 1000);
        doAnswer(new Answer() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Exception {
                final Message message = invocation.getArgument(0);
                byte[] bytes = message.getBody();

                final Field dataSynchronizerField = ClusterManagementServiceImpl.class.getDeclaredField("dataSynchronizer");
                dataSynchronizerField.setAccessible(true);
                BrokerBasedLog<String, Map> dataSynchronizer = (BrokerBasedLog<String, Map>) dataSynchronizerField.get(clusterManagementService);

                final Method decodeKeyValueMethod = BrokerBasedLog.class.getDeclaredMethod("decodeKeyValue", byte[].class);
                decodeKeyValueMethod.setAccessible(true);
                Map<String, Map> map = (Map<String, Map>) decodeKeyValueMethod.invoke(dataSynchronizer, bytes);

                final Field dataSynchronizerCallbackField = BrokerBasedLog.class.getDeclaredField("dataSynchronizerCallback");
                dataSynchronizerCallbackField.setAccessible(true);
                final DataSynchronizerCallback<String, Map> dataSynchronizerCallback = (DataSynchronizerCallback<String, Map>) dataSynchronizerCallbackField.get(dataSynchronizer);
                for (String key : map.keySet()) {
                    dataSynchronizerCallback.onCompletion(null, key, map.get(key));
                }
                return null;
            }
        }).when(producer).send(any(Message.class), any(SendCallback.class));

        clusterManagementService = new ClusterManagementServiceImpl(connectConfig);
        final Field dataSynchronizerField = ClusterManagementServiceImpl.class.getDeclaredField("dataSynchronizer");
        dataSynchronizerField.setAccessible(true);

        final Field producerField = BrokerBasedLog.class.getDeclaredField("producer");
        producerField.setAccessible(true);
        producerField.set((BrokerBasedLog<String, Map>) dataSynchronizerField.get(clusterManagementService), producer);

        final Field consumerField = BrokerBasedLog.class.getDeclaredField("consumer");
        consumerField.setAccessible(true);
        consumerField.set((BrokerBasedLog<String, Map>) dataSynchronizerField.get(clusterManagementService), consumer);
    }

    @Test
    public void testSendHeartBeat() throws Exception {
        Map<String, Long> aliveWorker = new HashMap<String, Long>() {
            {
                put("testWorkerId2", System.currentTimeMillis());
            }
        };
        Field aliveWorkerField = ClusterManagementServiceImpl.class.getDeclaredField("aliveWorker");
        aliveWorkerField.setAccessible(true);
        aliveWorkerField.set(clusterManagementService, aliveWorker);

        clusterManagementService.sendAliveHeartBeat();

        verify(producer, times(1)).send(any(Message.class), any(SendCallback.class));

        Map<String, Long> allAliveWorkers = clusterManagementService.getAllAliveWorkers();
        Set<String> keys = allAliveWorkers.keySet();
        assertEquals(2, keys.size());
        aliveWorker.remove("testWorkerId2");
        for (String s : keys) {
            Assert.assertTrue(s.equals(connectConfig.getWorkerId()));
        }

        aliveWorker.clear();
        clusterManagementService.sendOnlineFinishHeartBeat();

        verify(producer, times(2)).send(any(Message.class), any(SendCallback.class));

        allAliveWorkers = clusterManagementService.getAllAliveWorkers();
        keys = allAliveWorkers.keySet();
        assertEquals(1, keys.size());
        for (String s : keys) {
            Assert.assertTrue(s.equals(connectConfig.getWorkerId()));
        }

        aliveWorker.clear();
        clusterManagementService.sendOnlineHeartBeat();

        verify(producer, times(4)).send(any(Message.class), any(SendCallback.class));

        allAliveWorkers = clusterManagementService.getAllAliveWorkers();
        keys = allAliveWorkers.keySet();
        assertEquals(1, keys.size());
        for (String s : keys) {
            Assert.assertTrue(s.equals(connectConfig.getWorkerId()));
        }

        aliveWorker.clear();
        aliveWorker.put("testWorkerId", System.currentTimeMillis());
        assertEquals(1, aliveWorker.size());
        clusterManagementService.sendOffLineHeartBeat();

        verify(producer, times(5)).send(any(Message.class), any(SendCallback.class));

        allAliveWorkers = clusterManagementService.getAllAliveWorkers();
        keys = allAliveWorkers.keySet();
        assertEquals(0, keys.size());
    }

}