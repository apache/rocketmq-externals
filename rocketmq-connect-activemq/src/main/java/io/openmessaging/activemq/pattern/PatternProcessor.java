package io.openmessaging.activemq.pattern;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.lang3.StringUtils;

import io.openmessaging.activemq.Config;
import io.openmessaging.activemq.Replicator;

public class PatternProcessor {

	private Replicator replicator;
	
	private Config config;
	
	Connection connection;
	
	Session session;
	
	MessageConsumer consumer;
	
	public PatternProcessor(Replicator replicator) {
		this.replicator = replicator;
		this.config = replicator.getConfig();
	}
	
	public void start() {
		try {
		   ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(config.getActivemqUrl());
		   
	        //2、使用连接工厂创建一个连接对象
		   if(StringUtils.isNotBlank(config.getActivemqUsername()) && StringUtils.isNotBlank(config.getActivemqPassword()) ) {
	           connection = connectionFactory.createConnection(config.getActivemqUsername() , config.getActivemqPassword());
		   }else {
			   connection = connectionFactory.createConnection();
		   }
	        //3、开启连接
	        connection.start();
	        //4、使用连接对象创建会话（session）对象
	        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	        //5、使用会话对象创建目标对象，包含queue和topic（一对一和一对多）
	        Destination destination = null;
	        if(StringUtils.equals("topic", config.getDestinationType())) {
	        	destination = session.createTopic(config.getDestinationName());
	        }else if(StringUtils.equals("queue", config.getDestinationType())){
	        	destination = session.createQueue(config.getDestinationName());
	        }else {
	        	throw new RuntimeException("");
	        }
	        consumer = session.createConsumer(destination);
	        //6、使用会话对象创建生产者对象
	        //7、向consumer对象中设置一个messageListener对象，用来接收消息
	        consumer.setMessageListener(new MessageListener() {
	            @Override
	            public void onMessage(Message message) {
	            	replicator.commit(message, true);
	            }
	        });
		}catch(Exception e) {
			
		}
	}
	
	public void stop() {
        try {
        	consumer.close();
        	session.close();
			connection.close();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
 
}
