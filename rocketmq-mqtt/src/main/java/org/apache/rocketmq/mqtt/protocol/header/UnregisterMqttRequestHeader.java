package org.apache.rocketmq.mqtt.protocol.header;

import org.apache.rocketmq.remoting.CommandCustomHeader;
import org.apache.rocketmq.remoting.annotation.CFNotNull;
import org.apache.rocketmq.remoting.exception.RemotingCommandException;

public class UnregisterMqttRequestHeader implements CommandCustomHeader {
    @CFNotNull
    private String mqttBridgeName;

    @Override public void checkFields() throws RemotingCommandException {

    }

    public String getMqttBridgeName() {
        return mqttBridgeName;
    }

    public void setMqttBridgeName(String mqttBridgeName) {
        this.mqttBridgeName = mqttBridgeName;
    }
}
