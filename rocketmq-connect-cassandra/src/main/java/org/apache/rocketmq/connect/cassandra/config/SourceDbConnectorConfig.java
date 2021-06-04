/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.connect.cassandra.config;

import com.alibaba.fastjson.JSONObject;
import io.openmessaging.KeyValue;
import org.apache.rocketmq.connect.cassandra.strategy.DivideStrategyEnum;
import org.apache.rocketmq.connect.cassandra.strategy.DivideTaskByTopic;

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

        this.taskDivideStrategy = new DivideTaskByTopic();

        buildWhiteMap(config);

        this.converter = config.getString(Config.CONN_SOURCE_RECORD_CONVERTER);
        this.dbUrl = config.getString(Config.CONN_DB_IP);
        this.dbPort = config.getString(Config.CONN_DB_PORT);
        this.dbUserName = config.getString(Config.CONN_DB_USERNAME);
        this.dbPassword = config.getString(Config.CONN_DB_PASSWORD);
        this.localDataCenter = config.getString(Config.CONN_DB_DATACENTER);
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
