package org.apache.rocketmq.mqtt.protocol.header;

import org.apache.rocketmq.remoting.CommandCustomHeader;
import org.apache.rocketmq.remoting.annotation.CFNotNull;
import org.apache.rocketmq.remoting.exception.RemotingCommandException;

public class GetMqttBridgeAddrByClientIdResponseHeader implements CommandCustomHeader {

    @CFNotNull
    private String mqttBridgeAddr;

    public String getMqttBridgeAddr() {
        return mqttBridgeAddr;
    }

    public void setMqttBridgeAddr(String mqttBridgeAddr) {
        this.mqttBridgeAddr = mqttBridgeAddr;
    }

    @Override
    public void checkFields() throws RemotingCommandException {

    }
}
