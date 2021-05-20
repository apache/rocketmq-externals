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

public class ProducerTask implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(ProducerTask.class);

    private String broker;
    private String password;
    private String clientId;
    private String topic;
    private long interval;
    private String msg;
    private boolean isTimestamp = false;
    private int qos = 0;

    public ProducerTask(String broker, String password, String clientId, String topic,
        long interval, boolean isTimestamp) {
        this(broker, password, clientId, topic, interval, "", isTimestamp);
    }

    public ProducerTask(String broker, String password, String clientId, String topic, long interval,
        String msg) {
        this(broker, password, clientId, topic, interval, msg, false);
    }

    public ProducerTask(String broker, String password, String clientId, String topic, long interval,
        String msg, boolean isTimestamp) {
        this.broker = broker;
        this.password = password;
        this.clientId = clientId;
        this.topic = topic;
        this.interval = interval;
        this.msg = msg;
        this.isTimestamp = isTimestamp;
    }

    @Override public void run() {
        String taskName = broker + "#" + clientId + "#" + topic;
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);
            connectOptions.setUserName("mqtt");
            connectOptions.setPassword(password.toCharArray());
            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
            sampleClient.connect(connectOptions);

            sampleClient.setCallback(new MqttCallback() {
                @Override public void connectionLost(Throwable throwable) {
                    logger.warn("publisher client connection lost, the connection will be tried again; task:"
                        + taskName, throwable);
                    try {
                        if (sampleClient != null && !sampleClient.isConnected()) {
                            sampleClient.connect(connectOptions);
                        }
                    } catch (MqttException e) {
                        logger.error("publish mqtt client try connect again failed.");
                    }
                }

                @Override public void messageArrived(String topic, MqttMessage message) throws Exception {
                }

                @Override public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });
            logger.info("start publish task: " + taskName);

            while (true) {
                MqttMessage message = new MqttMessage();
                if (isTimestamp) {
                    long producer_timestamp = System.nanoTime();
                    message.setPayload(String.valueOf(producer_timestamp).getBytes());
                } else {
                    message.setPayload(msg.getBytes());
                }
                message.setQos(qos);
                sampleClient.publish(topic, message);
                Thread.sleep(interval);
            }
        } catch (Exception e) {
            logger.error("start producer task failed:" + taskName, e);
        }
    }
}
