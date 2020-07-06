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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import com.datastax.oss.driver.api.querybuilder.select.SelectFrom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.oss.driver.api.querybuilder.QueryBuilder;

public class Schema {
    private static final Logger LOGGER = LoggerFactory.getLogger(Schema.class);

    private static final String CQL = "SELECT * FROM system_schema.keyspaces";

    private static final String SCHEMA_SYSTEM_SCHEMA = "system_schema";

    private static final String TABLE_KEYSPACES = "keyspaces";

    private static final String COLUMN_KEYSPACE_NAME = "keyspace_name";

    private static final List<String> IGNORED_DATABASES = new ArrayList<>(
        Arrays.asList(new String[] {"system_schema", "system", "system_auth", "system_distributed", "system_traces"})
    );

    private CqlSession cqlSession;

    private Map<String, Database> dbMap;

    public Map<String, Set<String>> dbTableMap;

    public Map<String, Map<String, String>> tableFilterMap;

    public Schema(CqlSession cqlSession) {
        this.cqlSession = cqlSession;
        this.dbTableMap = new HashMap<>();
        this.tableFilterMap = new HashMap<>();
    }

    public void load() throws Exception {

        dbMap = new HashMap<>();

        Select selectFrom = QueryBuilder.selectFrom(SCHEMA_SYSTEM_SCHEMA, TABLE_KEYSPACES)
                .column(COLUMN_KEYSPACE_NAME);


        SimpleStatement stmt;
        boolean finishUpdate = false;
        LOGGER.info("trying to execute sql query,{}", selectFrom.asCql());
        ResultSet result = null;
        try {
            while (!cqlSession.isClosed() && !finishUpdate){
                stmt = selectFrom.build();
                result = cqlSession.execute(stmt);
                if (result.wasApplied()) {
                    LOGGER.info("update table success, executed cql query {}", selectFrom.asCql());
                }
                finishUpdate = true;
            }

            for (Row row : result) {
                String dbName = row.getString(COLUMN_KEYSPACE_NAME);
                if (!IGNORED_DATABASES.contains(dbName) && dbTableMap.keySet().contains(dbName)) {
                    Database database = new Database(dbName, cqlSession, dbTableMap.get(dbName), tableFilterMap);
                    dbMap.put(dbName, database);
                }
            }
        } catch (Exception e) {
            LOGGER.error("init cassandra Schema failure,{}", e);
        }

        for (Database db : dbMap.values()) {
            db.init();
        }

    }

//    public Table getTable(String dbName, String tableName) {
//
//        if (dbMap == null) {
//            reload();
//        }
//
//        Database database = dbMap.get(dbName);
//        if (database == null) {
//            return null;
//        }
//
//        Table table = database.getTable(tableName);
//        if (table == null) {
//            return null;
//        }
//
//        return table;
//    }

    private void reload() {

        while (true) {
            try {
                load();
                break;
            } catch (Exception e) {
                LOGGER.error("Cassandra Source Connector reload schema error.", e);
            }
        }
    }

    public void reset() {
        dbMap = null;
    }

    public Map<String, Database> getDbMap() {
        return dbMap;
    }
}

