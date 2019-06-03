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

package org.apache.rocketmq.connect.runtime.utils.datasync;

import io.openmessaging.*;
import io.openmessaging.connector.api.data.Converter;
import io.openmessaging.consumer.MessageListener;
import io.openmessaging.consumer.PushConsumer;
import io.openmessaging.producer.Producer;
import io.openmessaging.producer.SendResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BrokerBasedLogTest {

    @Mock
    private Producer producer;

    @Mock
    private PushConsumer consumer;

    private String queueName;

    private String consumerId;

    private BrokerBasedLog brokerBasedLog;

    @Mock
    private MessagingAccessPoint messagingAccessPoint;

    @Mock
    private DataSynchronizerCallback dataSynchronizerCallback;

    @Mock
    private BytesMessage bytesMessage;

    @Mock
    private Future<SendResult> future;

    @Mock
    private Converter converter;


    @Before
    public void init() {
        queueName = "testQueueName";
        consumerId = "testConsumerId";
        doReturn(producer).when(messagingAccessPoint).createProducer();
        doReturn(consumer).when(messagingAccessPoint).createPushConsumer(any(KeyValue.class));
        doReturn(bytesMessage).when(producer).createBytesMessage(anyString(), any(byte[].class));
        doReturn(future).when(producer).sendAsync(any(Message.class));
        doReturn(new byte[0]).when(converter).objectToByte(any(Object.class));
        brokerBasedLog = new BrokerBasedLog(messagingAccessPoint, queueName, consumerId, dataSynchronizerCallback, converter, converter);
    }

    @Test
    public void testStart() {
        brokerBasedLog.start();
        verify(producer, times(1)).startup();
        verify(consumer, times(1)).attachQueue(anyString(), any(MessageListener.class));
        verify(consumer, times(1)).startup();
    }

    @Test
    public void testStop() {
        brokerBasedLog.stop();
        verify(producer, times(1)).shutdown();
        verify(consumer, times(1)).shutdown();
    }

    @Test
    public void testSend() {
        brokerBasedLog.send(new Object(), new Object());
        verify(producer, times(1)).sendAsync(any(Message.class));
    }

}