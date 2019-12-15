package org.apache.rocketmq.connect.jdbc.config;

import io.openmessaging.KeyValue;
import org.apache.rocketmq.connect.jdbc.strategy.TaskDivideStrategy;

public abstract class DbConnectorConfig {

    public TaskDivideStrategy taskDivideStrategy;
    public String dbUrl;
    public String dbPort;
    public String dbUserName;
    public String dbPassword;
    public String converter;
    public int taskParallelism;
    public String mode;

    public abstract void validate(KeyValue config);

    public abstract <T> T getWhiteTopics();

    public TaskDivideStrategy getTaskDivideStrategy() {
        return taskDivideStrategy;
    }

    public void setTaskDivideStrategy(TaskDivideStrategy taskDivideStrategy) {
        this.taskDivideStrategy = taskDivideStrategy;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public String getDbPort() {
        return dbPort;
    }

    public void setDbPort(String dbPort) {
        this.dbPort = dbPort;
    }

    public String getDbUserName() {
        return dbUserName;
    }

    public void setDbUserName(String dbUserName) {
        this.dbUserName = dbUserName;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public String getConverter() {
        return converter;
    }

    public void setConverter(String converter) {
        this.converter = converter;
    }

    public int getTaskParallelism() {
        return taskParallelism;
    }

    public void setTaskParallelism(int taskParallelism) {
        this.taskParallelism = taskParallelism;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
