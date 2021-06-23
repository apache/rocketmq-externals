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


package org.apache.rocketmq.connect.cassandra.sink;


import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.DataTypes;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.delete.Delete;
import com.datastax.oss.driver.api.querybuilder.delete.DeleteSelection;
import com.datastax.oss.driver.api.querybuilder.insert.InsertInto;
import com.datastax.oss.driver.api.querybuilder.insert.RegularInsert;
import com.datastax.oss.driver.api.querybuilder.term.Term;
import io.openmessaging.connector.api.data.EntryType;
import io.openmessaging.connector.api.data.Field;
import io.openmessaging.connector.api.data.FieldType;
import org.apache.rocketmq.connect.cassandra.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Updater {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Queue<CqlSession> connections = new ConcurrentLinkedQueue<>();
    private Config config;
    private CqlSession cqlSession;

    private static final int BEFORE_UPDATE = 0;
    private static final int AFTER_UPDATE = 1;

    public Updater(Config config, CqlSession cqlSession) {
        this.config = config;
        this.cqlSession = cqlSession;
    }

    /**
     * We cannot know the primary key of each table, so we have to put every field in the where clause
     * @param dbName
     * @param tableName
     * @param fieldMap
     * @param entryType
     * @return
     */
    public boolean push(String dbName, String tableName, Map<Field, Object[]> fieldMap, EntryType entryType) {
        log.info("Updater Trying to push data");
        Boolean isSuccess = false;
        boolean afterUpdateExist;
        boolean beforeUpdateExist;
        switch (entryType) {
            case CREATE:
                isSuccess = updateRow(dbName, tableName, fieldMap);
                break;
            case UPDATE:
                isSuccess = updateRow(dbName, tableName, fieldMap);
                break;
            case DELETE:
                isSuccess = deleteRow(dbName, tableName, fieldMap);
                break;
            default:
                log.error("entryType {} is illegal.", entryType.toString());
        }
        return isSuccess;
    }

    public void start() throws Exception {
        log.info("schema load success");
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }


    /** Since we have no way of getting the id of a record, and we cannot get the primary key list of a table,
     * even we can it is not extensible. So we the result sql sentense would be like
     * UPDATE dbName.tableName SET afterUpdateValues WHERE beforeUpdateValues.
     *
     */
    private Boolean updateRow(String dbName, String tableName, Map<Field, Object[]> fieldMap) {
        log.info("Updater.updateRow() get called ");
        int count = 0;
        InsertInto insert = QueryBuilder.insertInto(dbName, tableName);
        RegularInsert regularInsert = null;
        for (Map.Entry<Field, Object[]> entry : fieldMap.entrySet()) {
            count++;
            String fieldName = entry.getKey().getName();
            FieldType fieldType = entry.getKey().getType();
            Object fieldValue = entry.getValue()[1];
            if (count == 1) {
                regularInsert = insert.value(fieldName, buildTerm(fieldType, fieldValue));
            }
            else {
                regularInsert = regularInsert.value(fieldName, buildTerm(fieldType, fieldValue));
            }
        }


        SimpleStatement stmt;
        boolean finishUpdate = false;
        log.info("trying to execute sql query,{}", regularInsert.asCql());
        try {
            while (!cqlSession.isClosed() && !finishUpdate){
                stmt = regularInsert.build();
                ResultSet result = cqlSession.execute(stmt);
                if (result.wasApplied()) {
                    log.info("update table success, executed cql query {}", regularInsert.asCql());
                    return true;
                }
                finishUpdate = true;
            }
        } catch (Exception e) {
            log.error("update table error,{}", e);
        }
        return false;
    }


    private boolean deleteRow(String dbName, String tableName, Map<Field, Object[]> fieldMap) {
        DeleteSelection deleteSelection = QueryBuilder.deleteFrom(dbName, tableName);
        Delete delete = null;
        int count = 0;
        for (Map.Entry<Field, Object[]> entry : fieldMap.entrySet()) {
            count++;
            String fieldName = entry.getKey().getName();
            FieldType fieldType = entry.getKey().getType();
            Object fieldValue = entry.getValue()[1];
            if (count == 1) {
                delete = deleteSelection.whereColumn(fieldName)
                    .isEqualTo(buildTerm(fieldType, fieldValue));
            }
            else {
                delete = delete.whereColumn(fieldName)
                    .isEqualTo(buildTerm(fieldType, fieldValue));
            }
        }

        boolean finishDelete = false;
        SimpleStatement stmt = delete.build();
        try {
            while (!cqlSession.isClosed() && !finishDelete){
                ResultSet result = cqlSession.execute(stmt);
                if (result.wasApplied()) {
                    log.info("delete from table success, executed query {}", delete);
                    return true;
                }
                finishDelete = true;
            }
        } catch (Exception e) {
            log.error("delete from table error,{}", e);
        }
        return false;
    }


    /**
     * Cassandra datastax driver automatically
     * infer type from literal value, or we can use "typeHints" to
     * tell datastax driver what type should this  literal be. We will add
     * type hints utils once we found it is necessary to do so. For now literal
     * inference should be enough.
     *
     * @param fieldType
     * @param fieldValue
     * @return
     */
    private Term buildTerm(FieldType fieldType, Object fieldValue) {
        return QueryBuilder.literal(fieldValue);
    }
    private DataType typeParser(FieldType fieldType) {
        DataType dataType = null;
        switch (fieldType) {
            case STRING:
                break;
            case DATETIME:
                break;
            case INT32:
            case INT64:
            case FLOAT32:
            case FLOAT64:
            case BIG_INTEGER:
                dataType = DataTypes.BIGINT;
                break;
            default:
                log.error("fieldType {} is illegal.", fieldType.toString());
        }
        return dataType;
    }

}
