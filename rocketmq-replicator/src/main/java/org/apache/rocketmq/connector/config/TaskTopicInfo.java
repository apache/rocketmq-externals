package org.apache.rocketmq.connector.config;

public class TaskTopicInfo {

    private String sourceTopic;
    private String brokerName;
    private String queueId;

    public TaskTopicInfo(String sourceTopic, String brokerName, String queueId) {
        this.sourceTopic = sourceTopic;
        this.brokerName = brokerName;
        this.queueId = queueId;
    }

    public String getSourceTopic() {
        return sourceTopic;
    }

    public void setSourceTopic(String sourceTopic) {
        this.sourceTopic = sourceTopic;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public String getQueueId() {
        return queueId;
    }

    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }
}
