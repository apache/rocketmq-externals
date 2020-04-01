package org.apache.rocketmq.connect.jdbc.config;

import io.openmessaging.KeyValue;
import org.apache.rocketmq.connect.jdbc.strategy.DivideStrategyEnum;
import org.apache.rocketmq.connect.jdbc.strategy.DivideTaskByQueue;
import org.apache.rocketmq.connect.jdbc.strategy.DivideTaskByTopic;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SinkDbConnectorConfig extends DbConnectorConfig{

    private Set<String> whiteList;
    private String srcNamesrvs;
    private String srcCluster;
    private long refreshInterval;
    private Map<String, Set<TaskTopicInfo>> topicRouteMap;

    public SinkDbConnectorConfig(){
    }

    @Override
    public void validate(KeyValue config) {
        this.taskParallelism = config.getInt(Config.CONN_TASK_PARALLELISM, 1);

        int strategy = config.getInt(Config.CONN_TASK_DIVIDE_STRATEGY, DivideStrategyEnum.BY_TOPIC.ordinal());
        if (strategy == DivideStrategyEnum.BY_QUEUE.ordinal()) {
            this.taskDivideStrategy = new DivideTaskByQueue();
        } else {
            this.taskDivideStrategy = new DivideTaskByTopic();
        }

        buildWhiteList(config);

        this.converter = config.getString(Config.CONN_SOURCE_RECORD_CONVERTER);
        this.dbUrl = config.getString(Config.CONN_DB_IP);
        this.dbPort = config.getString(Config.CONN_DB_PORT);
        this.dbUserName = config.getString(Config.CONN_DB_USERNAME);
        this.dbPassword = config.getString(Config.CONN_DB_PASSWORD);

        this.srcNamesrvs = config.getString(Config.CONN_SOURCE_RMQ);
        this.srcCluster = config.getString(Config.CONN_SOURCE_CLUSTER);
        this.refreshInterval = config.getLong(Config.REFRESH_INTERVAL, 3);
        this.mode = config.getString(Config.CONN_DB_MODE, "bulk");

    }

    private void buildWhiteList(KeyValue config) {
        this.whiteList = new HashSet<>();
        String whiteListStr = config.getString(Config.CONN_TOPIC_NAMES, "");
        String[] wl = whiteListStr.trim().split(",");
        if (wl.length <= 0)
            throw new IllegalArgumentException("White list must be not empty.");
        else {
            this.whiteList.clear();
            for (String t : wl) {
                this.whiteList.add(t.trim());
            }
        }
    }


    public Set<String> getWhiteList() {
        return whiteList;
    }

    public void setWhiteList(Set<String> whiteList) {
        this.whiteList = whiteList;
    }

    public String getSrcNamesrvs() {
        return this.srcNamesrvs;
    }

    public String getSrcCluster() {
        return this.srcCluster;
    }

    public long getRefreshInterval() {
        return this.refreshInterval;
    }

    public Map<String, Set<TaskTopicInfo>> getTopicRouteMap() {
        return topicRouteMap;
    }

    public void setTopicRouteMap(Map<String, Set<TaskTopicInfo>> topicRouteMap) {
        this.topicRouteMap = topicRouteMap;
    }

    @Override
    public Set<String> getWhiteTopics() {
        return getWhiteList();
    }

}
