package org.apache.rocketmq.connector.config;

public enum TaskConfigEnum {

    TASK_ID("taskId"),
    TASK_SOURCE_GROUP("sourceGroup"),
    TASK_SOURCE_ROCKETMQ("sourceRocketmq"),
    TASK_SOURCE_TOPIC("sourceTopic"),
    TASK_STORE_ROCKETMQ("storeTopic"),
    TASK_DATA_TYPE("dataType"),
    TASK_BROKER_NAME("brokerName"),
    TASK_QUEUE_ID("queueId"),
    TASK_NEXT_POSITION("nextPosition"),
    TASK_TOPIC_INFO("taskTopicList");

    private String key;

    TaskConfigEnum(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
