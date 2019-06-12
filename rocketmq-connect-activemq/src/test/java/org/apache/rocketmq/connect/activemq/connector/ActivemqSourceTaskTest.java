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

package org.apache.rocketmq.connect.activemq.connector;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQBytesMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQObjectMessage;
import org.apache.activemq.command.ActiveMQStreamMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.rocketmq.connect.activemq.Config;
import org.apache.rocketmq.connect.activemq.Replicator;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.alibaba.fastjson.JSON;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.data.SourceDataEntry;
import io.openmessaging.internal.DefaultKeyValue;

public class ActivemqSourceTaskTest {

    public void befores() throws JMSException, InterruptedException {
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://112.74.48.251:6166");
        Connection connection = connectionFactory.createConnection();

        connection.start();
        Session session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createQueue("test-queue");

        MessageProducer producer = session.createProducer(destination);

        producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        for (int i = 0; i < 20; i++) {
            TextMessage message = session.createTextMessage("hello 我是消息：" + i);
            producer.send(message);
        }

        session.commit();
        session.close();
        connection.close();
    }

    //@Test
    public void test() throws InterruptedException {
        KeyValue kv = new DefaultKeyValue();
        kv.put("activemqUrl", "tcp://112.74.48.251:6166");
        kv.put("destinationType", "queue");
        kv.put("destinationName", "test-queue");
        ActivemqSourceTask task = new ActivemqSourceTask();
        task.start(kv);
        for (int i = 0; i < 20; ) {
            Collection<SourceDataEntry> sourceDataEntry = task.poll();
            i = i + sourceDataEntry.size();
            System.out.println(sourceDataEntry);
        }
        Thread.sleep(20000);
    }

    @Test
    public void pollTest() throws Exception {
        ActivemqSourceTask task = new ActivemqSourceTask();
        TextMessage textMessage = new ActiveMQTextMessage();
        textMessage.setText("hello rocketmq");

        Replicator replicatorObject = Mockito.mock(Replicator.class);
        BlockingQueue<Message> queue = new LinkedBlockingQueue<>();
        Mockito.when(replicatorObject.getQueue()).thenReturn(queue);

        Field replicator = ActivemqSourceTask.class.getDeclaredField("replicator");
        replicator.setAccessible(true);
        replicator.set(task, replicatorObject);

        Field config = ActivemqSourceTask.class.getDeclaredField("config");
        config.setAccessible(true);
        config.set(task, new Config());

        queue.put(textMessage);
        Collection<SourceDataEntry> list = task.poll();
        Assert.assertEquals(list.size(), 1);

        list = task.poll();
        Assert.assertEquals(list.size(), 0);

    }

    @Test(expected = RuntimeException.class)
    public void getMessageConnentTest() throws JMSException {
        String value = "hello rocketmq";
        ActivemqSourceTask task = new ActivemqSourceTask();
        TextMessage textMessage = new ActiveMQTextMessage();
        textMessage.setText(value);
        ByteBuffer buffer = task.getMessageConnent(textMessage);
        Assert.assertEquals(new String(buffer.array()), textMessage.getText());

        ObjectMessage objectMessage = new ActiveMQObjectMessage();
        objectMessage.setObject(value);
        buffer = task.getMessageConnent(objectMessage);
        Assert.assertEquals(new String(buffer.array()), "\"" + objectMessage.getObject().toString() + "\"");

        BytesMessage bytes = new ActiveMQBytesMessage();
        bytes.writeBytes(value.getBytes());
        bytes.reset();
        buffer = task.getMessageConnent(bytes);
        Assert.assertEquals(new String(buffer.array()), value);

        MapMessage mapMessage = new ActiveMQMapMessage();
        mapMessage.setString("hello", "rocketmq");
        buffer = task.getMessageConnent(mapMessage);
        Map<String, String> map = JSON.parseObject(buffer.array(), Map.class);
        Assert.assertEquals(map.get("hello"), "rocketmq");
        Assert.assertEquals(map.size(), 1);

        StreamMessage streamMessage = new ActiveMQStreamMessage();
        String valueTwo = null;
        for (int i = 0; i < 200; i++) {
            valueTwo = valueTwo + value;
        }
        streamMessage.writeBytes(valueTwo.getBytes());
        streamMessage.reset();
        buffer = task.getMessageConnent(streamMessage);
        Assert.assertEquals(new String(buffer.array()), valueTwo);

        task.getMessageConnent(null);
    }
}
