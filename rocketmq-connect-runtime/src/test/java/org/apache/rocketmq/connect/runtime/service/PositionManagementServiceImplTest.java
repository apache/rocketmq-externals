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

import com.alibaba.fastjson.JSONObject;
import io.openmessaging.*;
import org.apache.rocketmq.connect.runtime.config.ConnectConfig;
import org.apache.rocketmq.connect.runtime.store.KeyValueStore;
import org.apache.rocketmq.connect.runtime.utils.TestUtils;
import org.apache.rocketmq.connect.runtime.utils.datasync.BrokerBasedLog;
import org.apache.rocketmq.connect.runtime.utils.datasync.DataSynchronizerCallback;
import io.openmessaging.consumer.PushConsumer;
import io.openmessaging.producer.Producer;
import io.openmessaging.producer.SendResult;
import io.openmessaging.rocketmq.domain.BytesMessageImpl;
import java.nio.ByteBuffer;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class PositionManagementServiceImplTest {

    private ConnectConfig connectConfig;

    @Mock
    private Producer producer;

    @Mock
    private PushConsumer consumer;

    @Mock
    private MessagingAccessPoint messagingAccessPoint;

    @Mock
    private Future<SendResult> future;

    private PositionManagementServiceImpl positionManagementService;

    private KeyValueStore<ByteBuffer, ByteBuffer> positionStore;

    private ByteBuffer sourcePartition;

    private ByteBuffer sourcePosition;

    private Map<ByteBuffer, ByteBuffer> positions;

    @Before
    public void init() throws Exception {
        connectConfig = new ConnectConfig();
        connectConfig.setHttpPort(8081);
        connectConfig.setOmsDriverUrl("oms:rocketmq://localhost:9876/default:default");
        connectConfig.setStorePathRootDir(System.getProperty("user.home") + File.separator + "testConnectorStore");
        connectConfig.setWorkerId("testWorkerId");
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

                final Field dataSynchronizerField = PositionManagementServiceImpl.class.getDeclaredField("dataSynchronizer");
                dataSynchronizerField.setAccessible(true);
                BrokerBasedLog<String, Map> dataSynchronizer = (BrokerBasedLog<String, Map>) dataSynchronizerField.get(positionManagementService);

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

        positionManagementService = new PositionManagementServiceImpl(connectConfig, messagingAccessPoint);

        positionManagementService.start();

        Field positionStoreField = PositionManagementServiceImpl.class.getDeclaredField("positionStore");
        positionStoreField.setAccessible(true);
        positionStore = (KeyValueStore<ByteBuffer, ByteBuffer>) positionStoreField.get(positionManagementService);

        sourcePartition = ByteBuffer.wrap("127.0.0.13306".getBytes("UTF-8"));
        JSONObject jsonObject = new JSONObject();
//        jsonObject.put(MysqlConstants.BINLOG_FILENAME, "binlogFilename");
//        jsonObject.put(MysqlConstants.NEXT_POSITION, "100");
        sourcePosition = ByteBuffer.wrap(jsonObject.toJSONString().getBytes());
        positions = new HashMap<ByteBuffer, ByteBuffer>() {
            {
                put(sourcePartition, sourcePosition);
            }
        };
    }

    @After
    public void destory() {
        positionManagementService.stop();
        TestUtils.deleteFile(new File(System.getProperty("user.home") + File.separator + "testConnectorStore"));
    }

    @Test
    public void testGetPositionTable() {
        Map<ByteBuffer, ByteBuffer> positionTable = positionManagementService.getPositionTable();
        ByteBuffer bytes = positionTable.get(sourcePartition);

        assertNull(bytes);

        positionManagementService.putPosition(positions);
        positionTable = positionManagementService.getPositionTable();
        bytes = positionTable.get(sourcePartition);

        assertNotNull(bytes);
    }

    @Test
    public void testPutPosition() throws Exception {
        ByteBuffer bytes = positionStore.get(sourcePartition);

        assertNull(bytes);

        positionManagementService.putPosition(positions);

        bytes = positionStore.get(sourcePartition);

        assertNotNull(bytes);
    }

    @Test
    public void testRemovePosition() {
        positionManagementService.putPosition(positions);
        ByteBuffer bytes = positionStore.get(sourcePartition);

        assertNotNull(bytes);

        List<ByteBuffer> sourcePartitions = new ArrayList<ByteBuffer>(8) {
            {
                add(sourcePartition);
            }
        };

        positionManagementService.removePosition(sourcePartitions);

        bytes = positionStore.get(sourcePartition);

        assertNull(bytes);
    }

}