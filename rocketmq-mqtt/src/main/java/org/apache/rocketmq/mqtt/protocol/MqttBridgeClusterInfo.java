package org.apache.rocketmq.mqtt.protocol;

import org.apache.rocketmq.remoting.protocol.RemotingSerializable;

import java.util.HashMap;
import java.util.Set;

public class MqttBridgeClusterInfo extends RemotingSerializable {
    private HashMap<String/* mqttBridgeName*/, MqttBridgeData> mqttBridgeTable;
    private HashMap<String/* clusterName*/, Set<String/*mqttBridgeName*/>> mqttCluster;

    public HashMap<String, MqttBridgeData> getMqttBridgeTable() {
        return mqttBridgeTable;
    }

    public void setMqttBridgeTable(
        HashMap<String, MqttBridgeData> mqttBridgeTable) {
        this.mqttBridgeTable = mqttBridgeTable;
    }

    public HashMap<String, Set<String>> getMqttCluster() {
        return mqttCluster;
    }

    public void setMqttCluster(HashMap<String, Set<String>> mqttCluster) {
        this.mqttCluster = mqttCluster;
    }
}
