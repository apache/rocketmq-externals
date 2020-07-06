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


package org.apache.rocketmq.connect.cassandra.source;

import com.alibaba.fastjson.JSONObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import com.datastax.oss.driver.api.querybuilder.select.SelectFrom;
import org.apache.rocketmq.connect.cassandra.config.Config;
import org.apache.rocketmq.connect.cassandra.schema.Database;
import org.apache.rocketmq.connect.cassandra.schema.Schema;
import org.apache.rocketmq.connect.cassandra.schema.Table;
import org.apache.rocketmq.connect.cassandra.schema.column.ColumnParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Querier {

    private final Logger log = LoggerFactory.getLogger(Querier.class); // use concrete subclass
    protected String topicPrefix;
    private final Queue<CqlSession> cqlSessions = new ConcurrentLinkedQueue<>();
    private Config config;
    private CqlSession cqlSession;
    private List<Table> list = new LinkedList<>();
    private String mode;
    private Schema schema;

    public Querier(){

    }

    public Querier(Config config, CqlSession cqlSession) {
        this.config = config;
        this.cqlSession = cqlSession;
        this.schema = new Schema(cqlSession);
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public List<Table> getList() {
        return list;
    }


    public void poll()  {
        try {
            LinkedList<Table> tableLinkedList = new LinkedList<>();

            for (Map.Entry<String, Database> entry : schema.getDbMap().entrySet()) {
                String dbName = entry.getKey();
                Iterator<Map.Entry<String, Table>> iterator = entry.getValue().getTableMap().entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Table> tableEntry = iterator.next();
                    String tableName = tableEntry.getKey();
                    Table table = tableEntry.getValue();
                    Map<String, String> tableFilterMap = table.getFilterMap();


                    Select selectFrom = QueryBuilder.selectFrom(dbName, tableName).all();
                    if (tableFilterMap != null && !tableFilterMap.keySet().contains("NO-FILTER")){
                        for (String key: tableFilterMap.keySet()) {
                            String value = tableFilterMap.get(key);
                            selectFrom.whereColumn(key).isEqualTo(QueryBuilder.literal(value));
                        }
                    }


                    SimpleStatement stmt;
                    boolean finishUpdate = false;
                    log.info("trying to execute sql query,{}", selectFrom.asCql());
                    ResultSet result = null;
                    while (!cqlSession.isClosed() && !finishUpdate){
                        stmt = selectFrom.build();
                        result = cqlSession.execute(stmt);
                        if (result.wasApplied()) {
                            log.info("query columns success, executed cql query {}", selectFrom.asCql());
                        }
                        finishUpdate = true;
                    }

                    List<String> colList = tableEntry.getValue().getColList();
                    List<String> dataTypeList = tableEntry.getValue().getRawDataTypeList();
                    List<ColumnParser> parserList = tableEntry.getValue().getParserList();

                    for (Row row : result) {
                        Table tableWithData = new Table(dbName, tableName);
                        tableWithData.setColList(colList);
                        tableWithData.setRawDataTypeList(dataTypeList);
                        tableWithData.setParserList(parserList);

                        for (String col : colList) {
                            tableWithData.getDataList().add(row.getObject(col));
                        }
                        tableLinkedList.add(tableWithData);
                    }
                }
            }
            list = tableLinkedList;
        } catch (Exception e) {
            log.error("fail to poll data, {}", e);
        }

    }

    public void start() throws Exception {
        String whiteDataBases = config.getWhiteDataBase();
        JSONObject whiteDataBaseObject = JSONObject.parseObject(whiteDataBases);

        if (whiteDataBaseObject != null){
            for (String whiteDataBaseName : whiteDataBaseObject.keySet()){
                JSONObject whiteTableObject = (JSONObject)whiteDataBaseObject.get(whiteDataBaseName);
                HashSet<String> whiteTableSet = new HashSet<>();
                for (String whiteTableName : whiteTableObject.keySet()){
                    Collections.addAll(whiteTableSet, whiteTableName);
                    HashMap<String, String> filterMap = new HashMap<>();
                    JSONObject tableFilterObject = JSONObject.parseObject(whiteTableObject.get(whiteTableName).toString());
                    for(String filterKey : tableFilterObject.keySet()){
                        filterMap.put(filterKey, tableFilterObject.getString(filterKey));
                    }
                    schema.tableFilterMap.put(whiteTableName, filterMap);
                }
                schema.dbTableMap.put(whiteDataBaseName, whiteTableSet);
            }
        }
        schema.load();
        log.info("load schema success");
    }
}
