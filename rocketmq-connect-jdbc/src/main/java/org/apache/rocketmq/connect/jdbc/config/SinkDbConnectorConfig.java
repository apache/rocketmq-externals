package org.apache.rocketmq.connect.jdbc.config;

import io.openmessaging.KeyValue;
import org.apache.rocketmq.connect.jdbc.strategy.DivideStrategyEnum;
import org.apache.rocketmq.connect.jdbc.strategy.DivideTaskByQueue;
import org.apache.rocketmq.connect.jdbc.strategy.DivideTaskByTopic;

import java.util.HashSet;
import java.util.Set;

public class SinkDbConnectorConfig extends DbConnectorConfig{

    private Set<String> whiteList;

    public SinkDbConnectorConfig(){
    }

    @Override
    public void validate(KeyValue config) {
        this.taskParallelism = config.getInt(Config.CONN_TASK_PARALLELISM, 0);

        int strategy = config.getInt(Config.CONN_TASK_DIVIDE_STRATEGY, DivideStrategyEnum.BY_QUEUE.ordinal());
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

    }

    private void buildWhiteList(KeyValue config) {
        this.whiteList = new HashSet<>();
        String whiteListStr = config.getString(Config.CONN_WHITE_LIST, "");
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

    @Override
    public Set<String> getWhiteTopics() {
        return getWhiteList();
    }

}
