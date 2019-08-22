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

package org.apache.connect.mongo.connector.builder;

import com.alibaba.fastjson.JSONObject;
import io.openmessaging.connector.api.data.DataEntryBuilder;
import io.openmessaging.connector.api.data.Field;
import io.openmessaging.connector.api.data.FieldType;
import io.openmessaging.connector.api.data.Schema;
import io.openmessaging.connector.api.data.SourceDataEntry;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.apache.connect.mongo.replicator.Constants;
import org.apache.connect.mongo.replicator.Position;
import org.apache.connect.mongo.replicator.ReplicaSetConfig;
import org.apache.connect.mongo.replicator.event.OperationType;
import org.apache.connect.mongo.replicator.event.ReplicationEvent;
import org.bson.BsonTimestamp;

import static org.apache.connect.mongo.replicator.Constants.CREATED;
import static org.apache.connect.mongo.replicator.Constants.NAMESPACE;
import static org.apache.connect.mongo.replicator.Constants.OBJECT_ID;
import static org.apache.connect.mongo.replicator.Constants.OPERATION_TYPE;
import static org.apache.connect.mongo.replicator.Constants.PATCH;
import static org.apache.connect.mongo.replicator.Constants.TIMESTAMP;
import static org.apache.connect.mongo.replicator.Constants.VERSION;

public class MongoDataEntry {

    private static String SCHEMA_CREATED_NAME = "mongo_created";
    private static String SCHEMA_OPLOG_NAME = "mongo_oplog";

    public static SourceDataEntry createSouceDataEntry(ReplicationEvent event, ReplicaSetConfig replicaSetConfig) {

        DataEntryBuilder dataEntryBuilder;

        if (event.getOperationType().equals(OperationType.CREATED)) {
            Schema schema = createdSchema(replicaSetConfig.getReplicaSetName());
            dataEntryBuilder = new DataEntryBuilder(schema);
            dataEntryBuilder.timestamp(System.currentTimeMillis())
                .queue(event.getNamespace().replace(".", "-").replace("$", "-"))
                .entryType(event.getEntryType());

            dataEntryBuilder.putFiled(CREATED, event.getDocument().toJson());
            dataEntryBuilder.putFiled(NAMESPACE, event.getNamespace());

        } else {
            Schema schema = oplogSchema(replicaSetConfig.getReplicaSetName());
            dataEntryBuilder = new DataEntryBuilder(schema);
            dataEntryBuilder.timestamp(System.currentTimeMillis())
                .queue(event.getNamespace().replace(".", "-").replace("$", "-"))
                .entryType(event.getEntryType());
            dataEntryBuilder.putFiled(OPERATION_TYPE, event.getOperationType().name());
            dataEntryBuilder.putFiled(TIMESTAMP, event.getTimestamp().getValue());
            dataEntryBuilder.putFiled(VERSION, event.getV());
            dataEntryBuilder.putFiled(NAMESPACE, event.getNamespace());
            dataEntryBuilder.putFiled(PATCH, event.getEventData().isPresent() ? JSONObject.toJSONString(event.getEventData().get()) : "");
            dataEntryBuilder.putFiled(OBJECT_ID, event.getObjectId().isPresent() ? JSONObject.toJSONString(event.getObjectId().get()) : "");
        }

        String position = createPosition(event, replicaSetConfig);
        SourceDataEntry sourceDataEntry = dataEntryBuilder.buildSourceDataEntry(
            ByteBuffer.wrap(replicaSetConfig.getReplicaSetName().getBytes(StandardCharsets.UTF_8)),
            ByteBuffer.wrap(position.getBytes(StandardCharsets.UTF_8)));
        return sourceDataEntry;
    }

    private static String createPosition(ReplicationEvent event, ReplicaSetConfig replicaSetConfig) {
        Position position = new Position();
        BsonTimestamp timestamp = event.getTimestamp();
        position.setInc(timestamp != null ? timestamp.getInc() : 0);
        position.setTimeStamp(timestamp != null ? timestamp.getTime() : 0);
        position.setInitSync(event.getOperationType().equals(OperationType.CREATED) ? true : false);
        return JSONObject.toJSONString(position);

    }

    private static Schema createdSchema(String dataSourceName) {
        Schema schema = new Schema();
        schema.setDataSource(dataSourceName);
        schema.setName(SCHEMA_CREATED_NAME);
        schema.setFields(new ArrayList<>());
        createdField(schema);
        return schema;
    }

    private static Schema oplogSchema(String dataSourceName) {
        Schema schema = new Schema();
        schema.setDataSource(dataSourceName);
        schema.setName(SCHEMA_OPLOG_NAME);
        oplogField(schema);
        return schema;
    }

    private static void createdField(Schema schema) {
        Field namespace = new Field(0, NAMESPACE, FieldType.STRING);
        schema.getFields().add(namespace);
        Field operation = new Field(1, Constants.CREATED, FieldType.STRING);
        schema.getFields().add(operation);
    }

    private static void oplogField(Schema schema) {
        schema.setFields(new ArrayList<>());
        Field op = new Field(0, OPERATION_TYPE, FieldType.STRING);
        schema.getFields().add(op);
        Field time = new Field(1, TIMESTAMP, FieldType.INT64);
        schema.getFields().add(time);
        Field v = new Field(2, VERSION, FieldType.INT32);
        schema.getFields().add(v);
        Field namespace = new Field(3, NAMESPACE, FieldType.STRING);
        schema.getFields().add(namespace);
        Field patch = new Field(4, PATCH, FieldType.STRING);
        schema.getFields().add(patch);
        Field objectId = new Field(5, OBJECT_ID, FieldType.STRING);
        schema.getFields().add(objectId);
    }

}
