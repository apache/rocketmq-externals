package org.apache.connect.mongo.replicator.event;

import java.util.Optional;
import org.bson.BsonTimestamp;
import org.bson.Document;

import static org.apache.connect.mongo.replicator.Constants.HASH;
import static org.apache.connect.mongo.replicator.Constants.NAMESPACE;
import static org.apache.connect.mongo.replicator.Constants.OBJECTID;
import static org.apache.connect.mongo.replicator.Constants.OPERATION;
import static org.apache.connect.mongo.replicator.Constants.OPERATIONTYPE;
import static org.apache.connect.mongo.replicator.Constants.TIMESTAMP;
import static org.apache.connect.mongo.replicator.Constants.VERSION;

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
