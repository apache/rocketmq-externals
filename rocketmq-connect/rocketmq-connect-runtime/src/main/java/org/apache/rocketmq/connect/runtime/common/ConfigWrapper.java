package org.apache.rocketmq.connect.runtime.common;

public class ConfigWrapper{

    private String leader;

    private ConnAndTaskConfigs connAndTaskConfigs;

    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public ConnAndTaskConfigs getConnAndTaskConfigs() {
        return connAndTaskConfigs;
    }

    public void setConnAndTaskConfigs(ConnAndTaskConfigs connAndTaskConfigs) {
        this.connAndTaskConfigs = connAndTaskConfigs;
    }

    @Override
    public String toString() {
        return "ConnAndTaskConfigs{" +
                "leader={" + this.getLeader() + "}" +
                "connectorConfigs=" + this.connAndTaskConfigs.getConnectorConfigs() +
                ", taskConfigs=" + this.connAndTaskConfigs.getTaskConfigs() +
                '}';
    }
}
