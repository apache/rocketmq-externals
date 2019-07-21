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

package org.apache.rocketmq.connect.rabbitmq.connector;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;

import org.apache.rocketmq.connect.jms.Replicator;
import org.apache.rocketmq.connect.rabbitmq.RabbitmqConfig;
import org.apache.rocketmq.connect.rabbitmq.pattern.RabbitMQPatternProcessor;
import org.junit.Assert;
import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.rabbitmq.jms.admin.RMQConnectionFactory;
import com.rabbitmq.jms.client.message.RMQBytesMessage;
import com.rabbitmq.jms.client.message.RMQMapMessage;
import com.rabbitmq.jms.client.message.RMQObjectMessage;
import com.rabbitmq.jms.client.message.RMQStreamMessage;
import com.rabbitmq.jms.client.message.RMQTextMessage;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.data.SourceDataEntry;
import io.openmessaging.internal.DefaultKeyValue;

public class RabbitmqSourceTaskTest {

	//@Before
	public void befores() throws JMSException, InterruptedException {
		RMQConnectionFactory connectionFactory = new RMQConnectionFactory();
		connectionFactory.setUri("amqp://112.74.48.251:5672");
		Connection connection = connectionFactory.createConnection("admin", "admin");

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
		kv.put("rabbitmqUrl", "amqp://112.74.48.251:5672");
		kv.put("rabbitmqUsername", "admin");
		kv.put("rabbitmqPassword", "admin");
		kv.put("destinationType", "queue");
		kv.put("destinationName", "test-queue");
		RabbitmqSourceTask task = new RabbitmqSourceTask();
		task.start(kv);
		for (int i = 0; i < 20;) {
			Collection<SourceDataEntry> sourceDataEntry = task.poll();
			i = i + sourceDataEntry.size();
			System.out.println(sourceDataEntry);
		}
		Thread.sleep(20000);
	}

	@Test
	public void getMessageConnentTest() throws JMSException {
		String value = "hello rocketmq";
		RabbitmqSourceTask task = new RabbitmqSourceTask();
		RMQTextMessage textMessage = new RMQTextMessage();
		textMessage.setText(value);
		ByteBuffer buffer = task.getMessageContent(textMessage);
		Assert.assertEquals(new String(buffer.array()), textMessage.getText());

		ObjectMessage objectMessage = new RMQObjectMessage();
		objectMessage.setObject(value);
		buffer = task.getMessageContent(objectMessage);
		Assert.assertEquals(new String(buffer.array()), "\"" + objectMessage.getObject().toString() + "\"");

		BytesMessage bytes = new RMQBytesMessage();
		bytes.writeBytes(value.getBytes());
		bytes.reset();
		buffer = task.getMessageContent(bytes);
		Assert.assertEquals(new String(buffer.array()), value);

		MapMessage mapMessage = new RMQMapMessage();
		mapMessage.setString("hello", "rocketmq");
		buffer = task.getMessageContent(mapMessage);
		Map<String, String> map = JSON.parseObject(buffer.array(), Map.class);
		Assert.assertEquals(map.get("hello"), "rocketmq");
		Assert.assertEquals(map.size(), 1);

		StreamMessage streamMessage = new RMQStreamMessage();
		String valueTwo = null;
		for (int i = 0; i < 200; i++) {
			valueTwo = valueTwo + value;
		}
		streamMessage.writeBytes(valueTwo.getBytes());
		streamMessage.reset();
		//buffer = task.getMessageContent(streamMessage);
		//Assert.assertEquals(new String(buffer.array()), valueTwo);

	}
	
	@Test(expected=Exception.class)
	public void getMessageConnentException() throws JMSException {
		RabbitmqSourceTask task = new RabbitmqSourceTask();
		task.getMessageContent(null);
		
	}

	public void getPatternProcessor(Replicator replicator) {
		KeyValue kv = new DefaultKeyValue();
		kv.put("rabbitmqUrl", "amqp://112.74.48.251:5672");
		kv.put("rabbitmqUsername", "admin");
		kv.put("rabbitmqPassword", "admin");
		kv.put("destinationType", "queue");
		kv.put("destinationName", "test-queue");
        RabbitmqConfig config = new RabbitmqConfig();
        config.load(kv);
        replicator = new Replicator(config,null);
        RabbitmqSourceTask task = new RabbitmqSourceTask();
		assertEquals(RabbitMQPatternProcessor.class, task.getPatternProcessor(replicator).getClass());
	}

	@Test
	public void getConfig() {
		RabbitmqSourceTask task = new RabbitmqSourceTask();
		assertEquals(task.getConfig().getClass() , RabbitmqConfig.class);
	}
}
