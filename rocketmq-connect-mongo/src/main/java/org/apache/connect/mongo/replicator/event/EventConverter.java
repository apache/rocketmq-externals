package org.apache.connect.mongo.replicator.event;

import org.bson.BsonTimestamp;
import org.bson.Document;

import java.util.Optional;

import static org.apache.connect.mongo.replicator.Constants.*;


public class EventConverter {


    public static ReplicationEvent convert(Document document, String replicaSetName) {

        ReplicationEvent event = new ReplicationEvent();
        event.setOperationType(OperationType.getOperationType(document.getString(OPERATIONTYPE)));
        event.setTimestamp(document.get(TIMESTAMP, BsonTimestamp.class));
        event.setH(document.getLong(HASH));
        event.setV(document.getInteger(VERSION));
        event.setNamespace(document.getString(NAMESPACE));
        event.setEventData(Optional.ofNullable(document.get(OPERATION, Document.class)));
        event.setObjectId(Optional.ofNullable(document.get(OBJECTID, Document.class)));
        event.setReplicaSetName(replicaSetName);
        event.setDocument(document);
        return event;
    }

}
