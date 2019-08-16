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

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.connect.runtime.common.ConnAndTaskConfigs;
import org.apache.rocketmq.connect.runtime.common.ConnectKeyValue;
import org.apache.rocketmq.connect.runtime.config.ConnectConfig;
import org.apache.rocketmq.connect.runtime.config.RuntimeConfigDefine;
import org.apache.rocketmq.connect.runtime.store.KeyValueStore;
import org.apache.rocketmq.connect.runtime.utils.Plugin;
import org.apache.rocketmq.connect.runtime.utils.TestUtils;
import org.apache.rocketmq.connect.runtime.utils.datasync.BrokerBasedLog;
import org.apache.rocketmq.connect.runtime.utils.datasync.DataSynchronizer;
import org.apache.rocketmq.connect.runtime.utils.datasync.DataSynchronizerCallback;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@RunWith(MockitoJUnitRunner.class)
public class ConfigManagementServiceImplTest {

    private KeyValueStore<String, ConnectKeyValue> connectorKeyValueStore;
    private KeyValueStore<String, List<ConnectKeyValue>> taskKeyValueStore;
    private Set<ConfigManagementService.ConnectorConfigUpdateListener> connectorConfigUpdateListener;
    private DataSynchronizer<String, ConnAndTaskConfigs> dataSynchronizer;
    private ConnectConfig connectConfig;

    @Mock
    private DefaultMQProducer producer;

    @Mock
    private DefaultMQPushConsumer consumer;

    private ConfigManagementServiceImpl configManagementService;

    private String connectorName;

    private ConnectKeyValue connectKeyValue;

    @Mock
    private Plugin plugin;

    @Before
    public void init() throws Exception {
        String consumerGroup = UUID.randomUUID().toString();
        String producerGroup = UUID.randomUUID().toString();

        connectConfig = new ConnectConfig();
        connectConfig.setHttpPort(8081);
        connectConfig.setStorePathRootDir(System.getProperty("user.home") + File.separator + "testConnectorStore");
        connectConfig.setRmqConsumerGroup("testConsumerGroup");
        connectorName = "testConnectorName";

        connectConfig.setRmqConsumerGroup(consumerGroup);
        connectConfig.setRmqProducerGroup(producerGroup);
        connectConfig.setNamesrvAddr("127.0.0.1:9876");
        connectConfig.setRmqMinConsumeThreadNums(1);
        connectConfig.setRmqMaxConsumeThreadNums(32);
        connectConfig.setRmqMessageConsumeTimeout(3 * 1000);

        connectKeyValue = new ConnectKeyValue();
        connectKeyValue.put(RuntimeConfigDefine.CONNECTOR_CLASS, "org.apache.rocketmq.connect.runtime.connectorwrapper.testimpl.TestConnector");
        connectKeyValue.put(RuntimeConfigDefine.SOURCE_RECORD_CONVERTER, "source-record-converter");

        doAnswer(new Answer() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Exception {
                final Message message = invocation.getArgument(0);
                byte[] bytes = message.getBody();

                final Field dataSynchronizerField = ConfigManagementServiceImpl.class.getDeclaredField("dataSynchronizer");
                dataSynchronizerField.setAccessible(true);
                BrokerBasedLog<String, ConnAndTaskConfigs> dataSynchronizer = (BrokerBasedLog<String, ConnAndTaskConfigs>) dataSynchronizerField.get(configManagementService);

                final Method decodeKeyValueMethod = BrokerBasedLog.class.getDeclaredMethod("decodeKeyValue", byte[].class);
                decodeKeyValueMethod.setAccessible(true);
                Map<String, ConnAndTaskConfigs> map = (Map<String, ConnAndTaskConfigs>) decodeKeyValueMethod.invoke(dataSynchronizer, bytes);

                final Field dataSynchronizerCallbackField = BrokerBasedLog.class.getDeclaredField("dataSynchronizerCallback");
                dataSynchronizerCallbackField.setAccessible(true);
                final DataSynchronizerCallback<String, ConnAndTaskConfigs> dataSynchronizerCallback = (DataSynchronizerCallback<String, ConnAndTaskConfigs>) dataSynchronizerCallbackField.get(dataSynchronizer);
                for (String key : map.keySet()) {
                    dataSynchronizerCallback.onCompletion(null, key, map.get(key));
                }
                return null;
            }
        }).when(producer).send(any(Message.class), any(SendCallback.class));

        configManagementService = new ConfigManagementServiceImpl(connectConfig, plugin);

        final Field connectorKeyValueStoreField = ConfigManagementServiceImpl.class.getDeclaredField("connectorKeyValueStore");
        connectorKeyValueStoreField.setAccessible(true);
        connectorKeyValueStore = (KeyValueStore<String, ConnectKeyValue>) connectorKeyValueStoreField.get(configManagementService);
        final Field taskKeyValueStoreField = ConfigManagementServiceImpl.class.getDeclaredField("taskKeyValueStore");
        taskKeyValueStoreField.setAccessible(true);
        taskKeyValueStore = (KeyValueStore<String, List<ConnectKeyValue>>) taskKeyValueStoreField.get(configManagementService);

        final Field dataSynchronizerField = ConfigManagementServiceImpl.class.getDeclaredField("dataSynchronizer");
        dataSynchronizerField.setAccessible(true);

        final Field producerField = BrokerBasedLog.class.getDeclaredField("producer");
        producerField.setAccessible(true);
        producerField.set((BrokerBasedLog<String, ConnAndTaskConfigs>) dataSynchronizerField.get(configManagementService), producer);

        final Field consumerField = BrokerBasedLog.class.getDeclaredField("consumer");
        consumerField.setAccessible(true);
        consumerField.set((BrokerBasedLog<String, ConnAndTaskConfigs>) dataSynchronizerField.get(configManagementService), consumer);

        configManagementService.start();
    }

    @After
    public void destory() {
        configManagementService.stop();
        TestUtils.deleteFile(new File(System.getProperty("user.home") + File.separator + "testConnectorStore"));
    }

    @Test
    public void testPutConnectorConfig() throws Exception {

        ConnectKeyValue connectKeyValue1 = connectorKeyValueStore.get(connectorName);
        List<ConnectKeyValue> connectKeyValues = taskKeyValueStore.get(connectorName);

        assertNull(connectKeyValue1);
        assertNull(connectKeyValues);

        configManagementService.putConnectorConfig(connectorName, connectKeyValue);

        connectKeyValue1 = connectorKeyValueStore.get(connectorName);
        connectKeyValues = taskKeyValueStore.get(connectorName);
        assertNotNull(connectKeyValue1);
        assertNotNull(connectKeyValues);
    }

    @Test
    public void testGetConnectorConfigs() throws Exception {
        Map<String, ConnectKeyValue> connectorConfigs = configManagementService.getConnectorConfigs();
        ConnectKeyValue connectKeyValue = connectorConfigs.get(connectorName);

        assertNull(connectKeyValue);

        configManagementService.putConnectorConfig(connectorName, this.connectKeyValue);
        connectorConfigs = configManagementService.getConnectorConfigs();
        connectKeyValue = connectorConfigs.get(connectorName);

        assertNotNull(connectKeyValue);
    }

    @Test
    public void testRemoveConnectorConfig() throws Exception {
        configManagementService.putConnectorConfig(connectorName, this.connectKeyValue);
        Map<String, ConnectKeyValue> connectorConfigs = configManagementService.getConnectorConfigs();
        ConnectKeyValue connectKeyValue = connectorConfigs.get(connectorName);

        Map<String, List<ConnectKeyValue>> taskConfigs = configManagementService.getTaskConfigs();
        List<ConnectKeyValue> connectKeyValues = taskConfigs.get(connectorName);

        assertNotNull(connectKeyValue);
        assertNotNull(connectKeyValues);

        configManagementService.removeConnectorConfig(connectorName);

        connectorConfigs = configManagementService.getConnectorConfigs();
        connectKeyValue = connectorConfigs.get(connectorName);
        taskConfigs = configManagementService.getTaskConfigs();
        connectKeyValues = taskConfigs.get(connectorName);

        assertNull(connectKeyValue);
        assertNull(connectKeyValues);
    }

    @Test
    public void testGetTaskConfigs() throws Exception {

        Map<String, List<ConnectKeyValue>> taskConfigs = configManagementService.getTaskConfigs();
        List<ConnectKeyValue> connectKeyValues = taskConfigs.get(connectorName);

        assertNull(connectKeyValues);

        configManagementService.putConnectorConfig(connectorName, this.connectKeyValue);

        taskConfigs = configManagementService.getTaskConfigs();
        connectKeyValues = taskConfigs.get(connectorName);

        assertNotNull(connectKeyValues);
    }

}

