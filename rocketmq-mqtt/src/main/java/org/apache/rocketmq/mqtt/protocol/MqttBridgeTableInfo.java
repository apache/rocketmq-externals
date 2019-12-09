package org.apache.rocketmq.mqtt.protocol;

import org.apache.rocketmq.remoting.protocol.RemotingSerializable;

import java.util.HashMap;

public class MqttBridgeTableInfo extends RemotingSerializable {

    private HashMap<String/* mqttBridgeName*/, MqttBridgeData> mqttBridgeTable;

    public HashMap<String, MqttBridgeData> getMqttBridgeTable() {
        return mqttBridgeTable;
    }

    public void setMqttBridgeTable(HashMap<String, MqttBridgeData> mqttBridgeTable) {
        this.mqttBridgeTable = mqttBridgeTable;
    }

    @Override
    public String toString() {
        return "MqttBridgeTableInfo{" +
                "mqttBridgeTable=" + mqttBridgeTable +
                '}';
    }
}
