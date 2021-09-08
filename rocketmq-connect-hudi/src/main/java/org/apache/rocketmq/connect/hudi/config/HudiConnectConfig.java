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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.rocketmq.connect.hudi.config;


import org.apache.avro.Schema;

import java.util.HashSet;
import java.util.Set;


public class HudiConnectConfig {

    protected String tableType = "COPY_ON_WRITE";

    protected String tablePath;

    protected String tableName;

    protected int insertShuffleParallelism = 2;

    protected int upsertShuffleParallelism = 2;

    protected int deleteParallelism = 2;

    protected String srcRecordConverter;

    protected String topicNames;

    protected String indexType = "INMEMORY";

    protected String schemaPath;

    public Schema schema;

    public static final String CONN_TASK_PARALLELISM = "task-parallelism";
    public static final String CONN_TASK_DIVIDE_STRATEGY = "task-divide-strategy";
    public static final String CONN_WHITE_LIST = "whiteDataBase";
    public static final String CONN_SOURCE_RECORD_CONVERTER = "source-record-converter";

    public static final String CONN_HUDI_TABLE_TYPE = "tableType";
    public static final String CONN_HUDI_TABLE_PATH = "tablePath";
    public static final String CONN_HUDI_TABLE_NAME = "tableName";
    public static final String CONN_HUDI_INSERT_SHUFFLE_PARALLELISM = "insertShuffleParallelism";
    public static final String CONN_HUDI_UPSERT_SHUFFLE_PARALLELISM = "upsertShuffleParallelism";
    public static final String CONN_HUDI_DELETE_PARALLELISM = "deleteParallelism";

    public static final String CONN_TOPIC_NAMES = "topicNames";
    public static final String CONN_TOPIC_QUEUES = "topicQueues";
    public static final String CONN_SCHEMA_PATH = "schemaPath";

    public static final String CONN_TOPIC_ROUTE_INFO = "topicRouterInfo";

    public static final String CONN_SOURCE_RMQ = "source-rocketmq";
    public static final String CONN_SOURCE_CLUSTER = "source-cluster";
    public static final String REFRESH_INTERVAL = "refresh.interval";

    public static final Set<String> REQUEST_CONFIG = new HashSet<String>() {
        {
            add(CONN_HUDI_TABLE_PATH);
            add(CONN_HUDI_TABLE_NAME);
            add(CONN_HUDI_INSERT_SHUFFLE_PARALLELISM);
            add(CONN_HUDI_UPSERT_SHUFFLE_PARALLELISM);
            add(CONN_HUDI_DELETE_PARALLELISM);
            add(CONN_SOURCE_RECORD_CONVERTER);
            add(CONN_TOPIC_NAMES);
            add(CONN_SCHEMA_PATH);
        }
    };

    public String getTableType() {
        return tableType;
    }

    public void setTableType(String tableType) {
        this.tableType = tableType;
    }

    public String getTablePath() {
        return tablePath;
    }

    public void setTablePath(String tablePath) {
        this.tablePath = tablePath;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public int getInsertShuffleParallelism() {
        return insertShuffleParallelism;
    }

    public void setInsertShuffleParallelism(int insertShuffleParallelism) {
        this.insertShuffleParallelism = insertShuffleParallelism;
    }

    public int getUpsertShuffleParallelism() {
        return upsertShuffleParallelism;
    }

    public void setUpsertShuffleParallelism(int upsertShuffleParallelism) {
        this.upsertShuffleParallelism = upsertShuffleParallelism;
    }

    public int getDeleteParallelism() {
        return deleteParallelism;
    }

    public void setDeleteParallelism(int deleteParallelism) {
        this.deleteParallelism = deleteParallelism;
    }

    public String getSrcRecordConverter() {
        return srcRecordConverter;
    }

    public void setSrcRecordConverter(String srcRecordConverter) {
        this.srcRecordConverter = srcRecordConverter;
    }

    public String getTopicNames() {
        return topicNames;
    }

    public void setTopicNames(String topicNames) {
        this.topicNames = topicNames;
    }

    public String getIndexType() {
        return indexType;
    }

    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }

    public String getSchemaPath() {
        return schemaPath;
    }

    public void setSchemaPath(String schemaPath) {
        this.schemaPath = schemaPath;
    }

    public Schema getSchema() {
        return schema;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }
}