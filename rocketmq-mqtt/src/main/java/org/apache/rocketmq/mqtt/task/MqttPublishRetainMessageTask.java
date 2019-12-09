package org.apache.rocketmq.mqtt.task;

import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.rocketmq.mqtt.MqttBridgeController;
import org.apache.rocketmq.mqtt.client.MQTTSession;
import org.apache.rocketmq.mqtt.exception.MqttRuntimeException;
import org.apache.rocketmq.mqtt.utils.MqttUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttPublishRetainMessageTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(MqttPublishRetainMessageTask.class);
    private MqttSubscribeMessage mqttSubscribeMessage;
    private MQTTSession client;
    private MqttBridgeController mqttBridgeController;

    public MqttPublishRetainMessageTask(MqttSubscribeMessage mqttSubscribeMessage, MQTTSession client,
        MqttBridgeController mqttBridgeController) {
        this.client = client;
        this.mqttSubscribeMessage = mqttSubscribeMessage;
        this.mqttBridgeController = mqttBridgeController;
    }

    @Override
    public void run() {
        List<MqttTopicSubscription> mqttTopicSubscriptions = mqttSubscribeMessage.payload().topicSubscriptions();
        Set<String> rootTopics = new HashSet<>();
        for (MqttTopicSubscription mqttTopicSubscription : mqttTopicSubscriptions) {
            rootTopics.add(MqttUtil.getRootTopic(mqttTopicSubscription.topicName()));
        }
        for (String rootTopic : rootTopics) {
            Set<String> retainTopics = mqttBridgeController.getPersistService().getTopicsByRootTopic(rootTopic);
            if (retainTopics == null) {
                return;
            }
            for (String retainTopic : retainTopics) {
                int maxRequestedQos = 0;
                boolean match = false;
                for (MqttTopicSubscription mqttTopicSubscription : mqttTopicSubscriptions) {
                    if (MqttUtil.isMatch(mqttTopicSubscription.topicName(), retainTopic)) {
                        maxRequestedQos = mqttTopicSubscription.qualityOfService().value() > maxRequestedQos ? mqttTopicSubscription.qualityOfService().value() : maxRequestedQos;
                        match = true;
                    }
                }
                if (match) {
                    byte[] retainMessage = this.mqttBridgeController.getPersistService().getRetainMessageByTopic(retainTopic);
                    if (retainMessage != null) {
                        publishRetainMessageToNewSub(client, MqttQoS.valueOf(maxRequestedQos), retainMessage, retainTopic);
                    } else {
                        log.info("no retainMessage");
                        return;
                    }

                }
            }
        }
    }

    private void publishRetainMessageToNewSub(MQTTSession client, MqttQoS qos, byte[] retainMessage, String topicName) {
        MqttFixedHeader mqttFixedHeader;
        switch (qos) {
            case AT_MOST_ONCE:
                mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, true, 0);
                client.pushRetainAndWillMessage(mqttFixedHeader, topicName, retainMessage);
                break;
            case AT_LEAST_ONCE:
                mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_LEAST_ONCE, true, 0);
                client.pushRetainAndWillMessage(mqttFixedHeader, topicName, retainMessage);
                break;
            default:
                throw new MqttRuntimeException("Qos = 2 messages are not supported yet.");
        }
    }
}
