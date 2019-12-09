package org.apache.rocketmq.mqtt.service.impl;

import org.apache.rocketmq.common.protocol.RequestCode;
import org.apache.rocketmq.common.protocol.header.mqtt.CloseClientConnectionRequestHeader;
import org.apache.rocketmq.mqtt.MqttBridgeController;
import org.apache.rocketmq.mqtt.client.MQTTSession;
import org.apache.rocketmq.mqtt.constant.MqttConstant;
import org.apache.rocketmq.mqtt.protocol.header.CloseClientConnectionRequestHeader;
import org.apache.rocketmq.mqtt.service.BridgeService;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BridgeServiceImpl implements BridgeService {
    private static final Logger log = LoggerFactory.getLogger(BridgeServiceImpl.class);
    private MqttBridgeController mqttBridgeController;

    public BridgeServiceImpl(MqttBridgeController mqttBridgeController) {
        this.mqttBridgeController = mqttBridgeController;
    }

    @Override
    public RemotingCommand closeClientConnection(boolean currentCleanSession, MQTTSession iotClient) {
        MQTTSession client = iotClient;
        String remoteBridgeAddr = client.getMqttBridgeAddr() + ":" + this.mqttBridgeController.getMultiNettyServerConfig().getListenPort();
        CloseClientConnectionRequestHeader requestHeader = new CloseClientConnectionRequestHeader();
        requestHeader.setCurrentCleanSession(currentCleanSession);
        requestHeader.setClientId(client.getClientId());
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.CLOSE_MQTTCLIENT_CONNECTION, requestHeader);
        log.info("Request Close Connection to Bridge：[{}], New cleanSession is [{}], and old client is [{}] ", remoteBridgeAddr,requestHeader.isCurrentCleanSession(),client.toString());
        try {
            RemotingCommand response = this.mqttBridgeController.getMqttRemotingClient().invokeSync(remoteBridgeAddr, request, MqttConstant.DEFAULT_TIMEOUT_MILLS);
            return response;
        } catch (Exception e) {
            log.warn("Ask remote mqtt bridge [{}] to close mqtt client [{}] failed. ", remoteBridgeAddr, client.toString());
        }
        return null;
    }
}
