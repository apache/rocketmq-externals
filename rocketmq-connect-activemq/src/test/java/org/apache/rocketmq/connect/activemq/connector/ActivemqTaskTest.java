package org.apache.rocketmq.connect.activemq.connector;

import java.util.Collection;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.rocketmq.connect.activemq.Config;
import org.junit.Before;
import org.junit.Test;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.data.SourceDataEntry;
import io.openmessaging.internal.DefaultKeyValue;

public class ActivemqTaskTest {

	@Before
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

	@Test
	public void nullTest() {
		
	}
	
	@Test
	public void test() throws InterruptedException {
		KeyValue kv = new DefaultKeyValue();
		kv.put("activemqUrl", "tcp://112.74.48.251:6166");
		kv.put("destinationType", "queue");
		kv.put("destinationName", "test-queue");
		ActivemqTask task = new ActivemqTask();
		task.start(kv);
		for(int i = 0 ; i < 20;) {
			Collection<SourceDataEntry> sourceDataEntry = task.poll();
			i = i+sourceDataEntry.size();
			System.out.println(sourceDataEntry);
		}
		Thread.sleep(20000);
		
	}
}
