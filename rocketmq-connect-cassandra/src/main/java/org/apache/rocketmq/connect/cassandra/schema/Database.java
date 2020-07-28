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


package org.apache.rocketmq.connect.cassandra.schema;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import com.datastax.oss.driver.api.querybuilder.select.SelectFrom;
import com.datastax.oss.driver.api.querybuilder.term.Term;
import io.openmessaging.connector.api.data.FieldType;
import org.apache.rocketmq.connect.cassandra.schema.column.ColumnParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Database {
    private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);

    private static final String CQL = "SELECT * FROM system_schema.columns \n" +
            "WHERE keyspace_name = 'keyspace_name' AND table_name = 'table_name'";

    private static final String SCHEMA_SYSTEM_SCHEMA = "system_schema";

    private static final String TABLE_COLUMNS = "columns";

    private static final String COLUMN_KEYSPACE_NAME = "keyspace_name";

    private static final String COLUMN_TABLE_NAME = "table_name";

    private static final String COLUMN_COLUMN_NAME = "column_name";

    private static final String COLUMN_TYPE = "type";

    private static final String GENERAL_CHARSET = "utf-8";

    private String name;

    private CqlSession cqlSession;

    private Map<String, Table> tableMap = new HashMap<String, Table>();

    public Set<String> tableWhiteList;

    public Map<String, Map<String, String>> tableFilterMap;

    public Database(String name, CqlSession cqlSession, Set<String> tableWhiteList, Map<String, Map<String, String>> tableFilterMap) {
        this.name = name;
        this.cqlSession = cqlSession;
        this.tableWhiteList = tableWhiteList;
        this.tableFilterMap = tableFilterMap;

    }

    public void init(){
        Select selectFrom = QueryBuilder.selectFrom(SCHEMA_SYSTEM_SCHEMA, TABLE_COLUMNS)
                .all()
                .whereColumn(COLUMN_KEYSPACE_NAME)
                .isEqualTo(QueryBuilder.literal(name));


        SimpleStatement stmt;
        boolean finishUpdate = false;
        LOGGER.info("trying to execute sql query,{}", selectFrom.asCql());
        ResultSet result = null;
        try {
            while (!cqlSession.isClosed() && !finishUpdate){
                stmt = selectFrom.build();
                result = cqlSession.execute(stmt);
                if (result.wasApplied()) {
                    LOGGER.info("query columns success, executed cql query {}", selectFrom.asCql());
                }
                finishUpdate = true;
            }

            for (Row row : result) {
                String tableName = row.getString(COLUMN_TABLE_NAME);
                String columnName = row.getString(COLUMN_COLUMN_NAME);
                String columnType = row.getString(COLUMN_TYPE);



                ColumnParser columnParser = ColumnParser.getColumnParser(columnType, columnType, GENERAL_CHARSET);

                if (!tableWhiteList.contains(tableName)){
                    continue;
                }
                if (!tableMap.containsKey(tableName)) {
                    addTable(tableName);
                }
                Table table = tableMap.get(tableName);
                table.addCol(columnName);
                table.addParser(columnParser);
                table.addRawDataType(columnType);
                table.setFilterMap(tableFilterMap.get(tableName));
            }
        } catch (Exception e) {
            LOGGER.error("init cassandra Schema failure,{}", e);
        }

    }

    private void addTable(String tableName) {

        LOGGER.info("Schema load -- DATABASE:{},\tTABLE:{}", name, tableName);

        Table table = new Table(name, tableName);
        tableMap.put(tableName, table);
    }

    public Table getTable(String tableName) {

        return tableMap.get(tableName);
    }

    public Map<String, Table> getTableMap() {
        return tableMap;
    }
}
