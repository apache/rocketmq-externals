package org.apache.rocketmq.connect.jdbc.config;

import com.alibaba.fastjson.JSONObject;
import io.openmessaging.KeyValue;
import org.apache.rocketmq.connect.jdbc.strategy.DivideStrategyEnum;
import org.apache.rocketmq.connect.jdbc.strategy.DivideTaskByQueue;
import org.apache.rocketmq.connect.jdbc.strategy.DivideTaskByTopic;

import java.util.HashMap;
import java.util.Map;

public class SourceDbConnectorConfig extends DbConnectorConfig{

    private Map<String, String> whiteMap;

    public SourceDbConnectorConfig(){
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

        buildWhiteMap(config);

        this.converter = config.getString(Config.CONN_SOURCE_RECORD_CONVERTER);
        this.dbUrl = config.getString(Config.CONN_DB_IP);
        this.dbPort = config.getString(Config.CONN_DB_PORT);
        this.dbUserName = config.getString(Config.CONN_DB_USERNAME);
        this.dbPassword = config.getString(Config.CONN_DB_PASSWORD);
        this.mode = config.getString(Config.CONN_DB_MODE, "bulk");

    }

    private void buildWhiteMap(KeyValue config) {
        this.whiteMap = new HashMap<>(16);
        String whiteListStr = config.getString(Config.CONN_WHITE_LIST, "");
        JSONObject whiteDataBaseObject = JSONObject.parseObject(whiteListStr);
        if(whiteDataBaseObject.keySet().size() <= 0){
            throw new IllegalArgumentException("white data base must be not empty.");
        }else {
            this.whiteMap.clear();
            for (String dbName : whiteDataBaseObject.keySet()){
                JSONObject whiteTableObject = (JSONObject) whiteDataBaseObject.get(dbName);
                for (String tableName : whiteTableObject.keySet()){
                    String dbTableKey = dbName + "-" + tableName;
                    this.whiteMap.put(dbTableKey, whiteTableObject.getString(tableName));
                }
            }
        }
    }


    public Map<String, String> getWhiteMap() {
        return whiteMap;
    }

    public void setWhiteMap(Map<String, String> whiteMap) {
        this.whiteMap = whiteMap;
    }

    @Override
    public Map<String, String> getWhiteTopics() {
        return getWhiteMap();
    }

}
