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

package org.apache.connect.mongo.replicator.event;

import io.openmessaging.connector.api.data.EntryType;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.bson.BsonTimestamp;
import org.bson.Document;

public class ReplicationEvent {

    private Document document;
    private OperationType operationType;
    private Integer v;
    private Long h;
    private BsonTimestamp timestamp;
    private String databaseName;
    private String collectionName;
    private String namespace;
    private Optional<Document> eventData;
    private Optional<Document> objectId;
    private String replicaSetName;

    public ReplicationEvent() {

    }

    public ReplicationEvent(OperationType operationType, BsonTimestamp timestamp, Integer v, Long h, String namespace,
        Optional<Document> eventData, Optional<Document> objectId, Document document) {
        this.operationType = operationType;
        this.v = v;
        this.h = h;
        this.timestamp = timestamp;
        this.namespace = namespace;
        this.eventData = eventData;
        this.objectId = objectId;
        String[] split = StringUtils.split(namespace, ".", 2);
        this.databaseName = split != null && split.length == 2 ? split[0] : "";
        this.collectionName = split != null && split.length == 2 ? split[1] : "";
        this.document = document;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public Integer getV() {
        return v;
    }

    public Long getH() {
        return h;
    }

    public BsonTimestamp getTimestamp() {
        return timestamp;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public String getNamespace() {
        return namespace;
    }

    public Optional<Document> getEventData() {
        return eventData;
    }

    public Optional<Document> getObjectId() {
        return objectId;
    }

    public EntryType getEntryType() {
        switch (operationType) {
            case UPDATE:
                return EntryType.UPDATE;
            case DELETE:
                return EntryType.DELETE;
            case INSERT:
                return EntryType.CREATE;
            default:
                return EntryType.CREATE;
        }
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public Document getDocument() {
        return document;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public void setV(Integer v) {
        this.v = v;
    }

    public void setH(Long h) {
        this.h = h;
    }

    public void setTimestamp(BsonTimestamp timestamp) {
        this.timestamp = timestamp;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public void setEventData(Optional<Document> eventData) {
        this.eventData = eventData;
    }

    public void setObjectId(Optional<Document> objectId) {
        this.objectId = objectId;
    }

    public void setReplicaSetName(String replicaSetName) {
        this.replicaSetName = replicaSetName;
    }

    public String getReplicaSetName() {
        return replicaSetName;
    }

    @Override
    public String toString() {
        return "ReplicationEvent{" +
            "document=" + document +
            ", operationType=" + operationType +
            ", v=" + v +
            ", h=" + h +
            ", timestamp=" + timestamp +
            ", databaseName='" + databaseName + '\'' +
            ", collectionName='" + collectionName + '\'' +
            ", namespace='" + namespace + '\'' +
            ", eventData=" + eventData +
            ", objectId=" + objectId +
            ", replicaSetName='" + replicaSetName + '\'' +
            '}';
    }
}
