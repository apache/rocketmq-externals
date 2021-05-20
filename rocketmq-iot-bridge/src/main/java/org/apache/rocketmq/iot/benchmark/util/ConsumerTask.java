package org.apache.rocketmq.iot.benchmark.util;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerTask implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(ConsumerTask.class);

    private String broker;
    private String password;
    private String clientId;
    private String topic;
    private int qos = 0;
    private boolean isTimestamp = false;

    public ConsumerTask(String broker, String password, String clientId, String topic) {
        this(broker, password, clientId, topic, false);
    }

    public ConsumerTask(String broker, String password, String clientId, String topic, boolean isTimestamp) {
        this.broker = broker;
        this.password = password;
        this.clientId = clientId;
        this.topic = topic;
        this.isTimestamp = isTimestamp;
    }

    @Override public void run() {
        String taskName = broker + "#" + clientId + "#" + topic;
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);

            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);
            connectOptions.setUserName("mqtt");
            connectOptions.setPassword(password.toCharArray());

            sampleClient.connect(connectOptions);
            sampleClient.subscribe(topic, qos);

            logger.info("start subscribe task: " + taskName);

            sampleClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {
                    logger.warn("subscriber client connection lost, the connection will be tried again.", throwable);
                    try {
                        if (sampleClient != null && !sampleClient.isConnected()) {
                            sampleClient.connect(connectOptions);
                            sampleClient.subscribe(topic, qos);
                        }
                    } catch (MqttException e) {
                        logger.error("subscriber client try connect again failed.");
                    }
                }

                @Override
                public void messageArrived(String s, MqttMessage message) throws Exception {
                    long consumer_timestamp = System.nanoTime();
                    if (isTimestamp) {
                        long producer_timestamp = Long.parseLong(message.toString());
                        long diff = consumer_timestamp - producer_timestamp;
                        logger.info("diff(ms): " + (double) diff / 1000 / 1000);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });
        } catch (Exception e) {
            logger.error("start subscribe task failed:" + taskName, e);
        }
    }
}
