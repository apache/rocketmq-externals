package org.apache.connect.mongo.replicator;

import com.mongodb.CursorType;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.apache.connect.mongo.initsync.InitSync;
import org.apache.connect.mongo.replicator.event.EventConverter;
import org.apache.connect.mongo.replicator.event.ReplicationEvent;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplicatorTask implements Runnable {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private ReplicaSet replicaSet;

    private MongoClient mongoClient;

    private ReplicaSetConfig replicaSetConfig;

    private ReplicaSetsContext replicaSetsContext;

    public ReplicatorTask(ReplicaSet replicaSet, MongoClient mongoClient, ReplicaSetConfig replicaSetConfig,
        ReplicaSetsContext replicaSetsContext) {
        this.replicaSet = replicaSet;
        this.replicaSetConfig = replicaSetConfig;
        this.mongoClient = mongoClient;
        this.replicaSetsContext = replicaSetsContext;
    }

    @Override
    public void run() {

        if (replicaSetConfig.getPosition() == null || replicaSetConfig.getPosition().isInitSync()) {
            InitSync initSync = new InitSync(replicaSetConfig, mongoClient, replicaSetsContext, replicaSet);
            initSync.start();
        }

        MongoDatabase localDataBase = mongoClient.getDatabase(Constants.MONGO_LOCAL_DATABASE);
        FindIterable<Document> iterable;
        if (replicaSetConfig.getPosition().isValid()) {
            iterable = localDataBase.getCollection(Constants.MONGO_OPLOG_RS).find(
                Filters.gt("ts", replicaSetConfig.getPosition().converBsonTimeStamp()));
        } else {
            iterable = localDataBase.getCollection(Constants.MONGO_OPLOG_RS).find();
        }
        MongoCursor<Document> cursor = iterable.sort(new Document("$natural", 1))
            .noCursorTimeout(true)
            .cursorType(CursorType.TailableAwait)
            .batchSize(200)
            .iterator();

        while (replicaSet.isRuning()) {
            try {
                executorCursor(cursor);
            } catch (Exception e) {
                logger.error("replicaSet:{} shutdown.....", replicaSetConfig, e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                replicaSet.shutdown();
            }
        }
        logger.info("replicaSet:{}, already shutdown, replicaTask end of life cycle", replicaSetConfig);
    }

    private void executorCursor(MongoCursor<Document> cursor) {
        while (cursor.hasNext() && !replicaSet.isPause()) {
            Document document = cursor.next();
            ReplicationEvent event = EventConverter.convert(document, replicaSetConfig.getReplicaSetName());
            if (replicaSetsContext.filterEvent(event)) {
                replicaSetsContext.publishEvent(event, replicaSetConfig);
            }
        }
    }

}
