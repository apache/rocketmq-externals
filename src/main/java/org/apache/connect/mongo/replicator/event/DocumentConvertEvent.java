package org.apache.connect.mongo.replicator.event;

import org.bson.BsonTimestamp;
import org.bson.Document;

import java.util.Optional;

import static org.apache.connect.mongo.replicator.Constants.*;


public class DocumentConvertEvent {


    public static ReplicationEvent convert(Document document) {

        OperationType operationType = OperationType.getOperationType(document.getString(OPERATIONTYPE));
        BsonTimestamp timestamp = (BsonTimestamp) document.get(TIMESTAMP);
//                Long t = document.getLong("t");
        Long h = document.getLong(HASH);
        Integer v = document.getInteger(VERSION);
        String nameSpace = document.getString(NAMESPACE);
//                String uuid = document.getString("uuid");
//                Date wall = document.getDate("wall");
        Document operation = document.get(OPERATION, Document.class);
        Document objectID = document.get(OBJECTID, Document.class);
        return new ReplicationEvent(operationType, timestamp, v, h, nameSpace, Optional.ofNullable(operation), Optional.ofNullable(objectID), document);
    }

}
