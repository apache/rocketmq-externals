package org.apache.rocketmq.connect.activemq.pattern;

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
import org.apache.rocketmq.connect.activemq.Config;
import org.apache.rocketmq.connect.activemq.Replicator;

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
        if (!StringUtils.equals("topic", config.getDestinationType()) && !StringUtils.equals("queue", config.getDestinationType())) {
            throw new RuntimeException("destination type is incorrectness");
        }

        try {
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(config.getActivemqUrl());

            if (StringUtils.isNotBlank(config.getActivemqUsername()) && StringUtils.isNotBlank(config.getActivemqPassword())) {
                connection = connectionFactory.createConnection(config.getActivemqUsername(), config.getActivemqPassword());
            } else {
                connection = connectionFactory.createConnection();
            }
            connection.start();
            Session session = connection.createSession(config.getSessionTransacted(), config.getSessionAcknowledgeMode());
            Destination destination = null;
            if (StringUtils.equals("topic", config.getDestinationType())) {
                destination = session.createTopic(config.getDestinationName());
            } else if (StringUtils.equals("queue", config.getDestinationType())) {
                destination = session.createQueue(config.getDestinationName());
            }
            consumer = session.createConsumer(destination, config.getMessageSelector());
            consumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    replicator.commit(message, true);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
            consumer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

}
