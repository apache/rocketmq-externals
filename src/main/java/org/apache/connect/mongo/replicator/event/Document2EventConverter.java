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

import java.util.Optional;
import org.bson.BsonTimestamp;
import org.bson.Document;

import static org.apache.connect.mongo.replicator.Constants.HASH;
import static org.apache.connect.mongo.replicator.Constants.NAMESPACE;
import static org.apache.connect.mongo.replicator.Constants.OBJECT_ID;
import static org.apache.connect.mongo.replicator.Constants.OPERATION;
import static org.apache.connect.mongo.replicator.Constants.OPERATION_TYPE;
import static org.apache.connect.mongo.replicator.Constants.TIMESTAMP;
import static org.apache.connect.mongo.replicator.Constants.VERSION;

public class Document2EventConverter {

    public static ReplicationEvent convert(Document document, String replicaSetName) {

        ReplicationEvent event = new ReplicationEvent();
        event.setOperationType(OperationType.getOperationType(document.getString(OPERATION_TYPE)));
        event.setTimestamp(document.get(TIMESTAMP, BsonTimestamp.class));
        event.setH(document.getLong(HASH));
        event.setV(document.getInteger(VERSION));
        event.setNamespace(document.getString(NAMESPACE));
        event.setEventData(Optional.ofNullable(document.get(OPERATION, Document.class)));
        event.setObjectId(Optional.ofNullable(document.get(OBJECT_ID, Document.class)));
        event.setReplicaSetName(replicaSetName);
        event.setDocument(document);
        return event;
    }

}
