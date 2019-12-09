package org.apache.rocketmq.mqtt.service;

import org.apache.rocketmq.mqtt.client.MQTTSession;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

public interface BridgeService {
    /**
     * Notify Remote Mqtt Bridge to close client's connection and update session
     * @param currentCleanSession cleanSession which in new CONNECT packet
     * @param mqttSession client which should be closed
     * @return RemotingCommand
     */
    RemotingCommand closeClientConnection(final boolean currentCleanSession,final MQTTSession mqttSession);
}
