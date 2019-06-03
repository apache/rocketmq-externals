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

import io.openmessaging.*;
import org.apache.rocketmq.connect.runtime.common.ConnAndTaskConfigs;
import org.apache.rocketmq.connect.runtime.common.ConnectKeyValue;
import org.apache.rocketmq.connect.runtime.config.ConnectConfig;
import org.apache.rocketmq.connect.runtime.config.RuntimeConfigDefine;
import org.apache.rocketmq.connect.runtime.store.KeyValueStore;
import org.apache.rocketmq.connect.runtime.utils.TestUtils;
import org.apache.rocketmq.connect.runtime.utils.datasync.BrokerBasedLog;
import org.apache.rocketmq.connect.runtime.utils.datasync.DataSynchronizer;
import org.apache.rocketmq.connect.runtime.utils.datasync.DataSynchronizerCallback;
import io.openmessaging.connector.api.Connector;
import io.openmessaging.connector.api.Task;
import io.openmessaging.consumer.PushConsumer;
import io.openmessaging.producer.Producer;
import io.openmessaging.producer.SendResult;
import io.openmessaging.rocketmq.domain.BytesMessageImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class ConfigManagementServiceImplTest {

    private KeyValueStore<String, ConnectKeyValue> connectorKeyValueStore;
    private KeyValueStore<String, List<ConnectKeyValue>> taskKeyValueStore;
    private Set<ConfigManagementService.ConnectorConfigUpdateListener> connectorConfigUpdateListener;
    private DataSynchronizer<String, ConnAndTaskConfigs> dataSynchronizer;
    private ConnectConfig connectConfig;

    @Mock
    private Producer producer;

    @Mock
    private PushConsumer consumer;

    @Mock
    private MessagingAccessPoint messagingAccessPoint;

    @Mock
    private Future<SendResult> future;

    private ConfigManagementServiceImpl configManagementService;

    private String connectorName;

    private ConnectKeyValue connectKeyValue;

    @Before
    public void init() throws Exception {
        connectConfig = new ConnectConfig();
        connectConfig.setHttpPort(8081);
        connectConfig.setOmsDriverUrl("oms:rocketmq://localhost:9876/default:default");
        connectConfig.setStorePathRootDir(System.getProperty("user.home") + File.separator + "testConnectorStore");
        connectConfig.setWorkerId("testWorkerId");

        connectorName = "testConnectorName";
        connectKeyValue = new ConnectKeyValue();
        connectKeyValue.put(RuntimeConfigDefine.CONNECTOR_CLASS, "org.apache.rocketmq.connect.runtime.service.TestConnector");
        connectKeyValue.put(RuntimeConfigDefine.OMS_DRIVER_URL, "oms:rocketmq://localhost:9876/default:default");
        connectKeyValue.put(RuntimeConfigDefine.SOURCE_RECORD_CONVERTER, "source-record-converter");


        doReturn(producer).when(messagingAccessPoint).createProducer();
        doReturn(consumer).when(messagingAccessPoint).createPushConsumer(any(KeyValue.class));
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                final String queue = invocationOnMock.getArgument(0);
                final byte[] body = invocationOnMock.getArgument(1);
                BytesMessage message = new BytesMessageImpl();
                message.setBody(body);
                message.sysHeaders().put("DESTINATION", queue);
                return message;
            }
        }).when(producer).createBytesMessage(anyString(), any(byte[].class));
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Exception {
                final Message message = invocation.getArgument(0);
                byte[] bytes = message.getBody(byte[].class);

                final Field dataSynchronizerField = ConfigManagementServiceImpl.class.getDeclaredField("dataSynchronizer");
                dataSynchronizerField.setAccessible(true);
                BrokerBasedLog<String, Map> dataSynchronizer = (BrokerBasedLog<String, Map>) dataSynchronizerField.get(configManagementService);

                final Method decodeKeyValueMethod = BrokerBasedLog.class.getDeclaredMethod("decodeKeyValue", byte[].class);
                decodeKeyValueMethod.setAccessible(true);
                Map<String, Map> map = (Map<String, Map>) decodeKeyValueMethod.invoke(dataSynchronizer, bytes);

                final Field dataSynchronizerCallbackField = BrokerBasedLog.class.getDeclaredField("dataSynchronizerCallback");
                dataSynchronizerCallbackField.setAccessible(true);
                final DataSynchronizerCallback<String, Map> dataSynchronizerCallback = (DataSynchronizerCallback<String, Map>) dataSynchronizerCallbackField.get(dataSynchronizer);
                for (String key : map.keySet()) {
                    dataSynchronizerCallback.onCompletion(null, key, map.get(key));
                }
                return future;
            }
        }).when(producer).sendAsync(any(Message.class));

        configManagementService = new ConfigManagementServiceImpl(connectConfig, messagingAccessPoint);

        configManagementService.start();

        final Field connectorKeyValueStoreField = ConfigManagementServiceImpl.class.getDeclaredField("connectorKeyValueStore");
        connectorKeyValueStoreField.setAccessible(true);
        connectorKeyValueStore = (KeyValueStore<String, ConnectKeyValue>) connectorKeyValueStoreField.get(configManagementService);
        final Field taskKeyValueStoreField = ConfigManagementServiceImpl.class.getDeclaredField("taskKeyValueStore");
        taskKeyValueStoreField.setAccessible(true);
        taskKeyValueStore = (KeyValueStore<String, List<ConnectKeyValue>>) taskKeyValueStoreField.get(configManagementService);

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

class TestConnector implements Connector {

    private KeyValue config;

    @Override
    public String verifyAndSetConfig(KeyValue config) {
        this.config = config;
        return "";
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public Class<? extends Task> taskClass() {
        return TestTask.class;
    }

    @Override
    public List<KeyValue> taskConfigs() {
        List<KeyValue> configs = new ArrayList<>();
        configs.add(this.config);
        return configs;
    }
}

class TestTask implements Task {

    @Override
    public void start(KeyValue config) {

    }

    @Override
    public void stop() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }
}

