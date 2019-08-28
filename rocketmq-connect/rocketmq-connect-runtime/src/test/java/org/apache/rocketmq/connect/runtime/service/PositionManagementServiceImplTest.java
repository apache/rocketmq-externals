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
import io.openmessaging.Future;
import io.openmessaging.producer.SendResult;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.connect.runtime.common.ConnAndTaskConfigs;
import org.apache.rocketmq.connect.runtime.config.ConnectConfig;
import org.apache.rocketmq.connect.runtime.store.KeyValueStore;
import org.apache.rocketmq.connect.runtime.utils.TestUtils;
import org.apache.rocketmq.connect.runtime.utils.datasync.BrokerBasedLog;
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
public class PositionManagementServiceImplTest {

    private ConnectConfig connectConfig;

    @Mock
    private DefaultMQProducer producer;

    @Mock
    private DefaultMQPushConsumer consumer;

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
        connectConfig.setNamesrvAddr("localhost:9876");
        connectConfig.setStorePathRootDir(System.getProperty("user.home") + File.separator + "testConnectorStore");
        connectConfig.setRmqConsumerGroup("testConsumerGroup");
        doAnswer(new Answer() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Exception {
                final Message message = invocation.getArgument(0);
                byte[] bytes = message.getBody();

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
                return null;
            }
        }).when(producer).send(any(Message.class), any(SendCallback.class));

        positionManagementService = new PositionManagementServiceImpl(connectConfig);

        final Field dataSynchronizerField = PositionManagementServiceImpl.class.getDeclaredField("dataSynchronizer");
        dataSynchronizerField.setAccessible(true);

        final Field producerField = BrokerBasedLog.class.getDeclaredField("producer");
        producerField.setAccessible(true);
        producerField.set((BrokerBasedLog<String, ConnAndTaskConfigs>) dataSynchronizerField.get(positionManagementService), producer);

        final Field consumerField = BrokerBasedLog.class.getDeclaredField("consumer");
        consumerField.setAccessible(true);
        consumerField.set((BrokerBasedLog<String, ConnAndTaskConfigs>) dataSynchronizerField.get(positionManagementService), consumer);

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