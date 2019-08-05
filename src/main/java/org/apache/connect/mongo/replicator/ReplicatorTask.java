package org.apache.connect.mongo.replicator;

import com.mongodb.CursorType;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.apache.connect.mongo.MongoReplicatorConfig;
import org.apache.connect.mongo.replicator.event.DocumentConvertEvent;
import org.apache.connect.mongo.replicator.event.ReplicationEvent;
import org.apache.connect.mongo.initsync.InitSync;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ReplicatorTask implements Runnable {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private MongoReplicator mongoReplicator;

    private MongoClient mongoClient;

    private MongoReplicatorConfig mongoReplicatorConfig;

    private Filter filter;

    public ReplicatorTask(MongoReplicator mongoReplicator, MongoClient mongoClient, MongoReplicatorConfig mongoReplicatorConfig, Filter filter) {
        this.mongoReplicator = mongoReplicator;
        this.mongoReplicatorConfig = mongoReplicatorConfig;
        this.mongoClient = mongoClient;
        this.filter = filter;
    }

    @Override
    public void run() {

        if (Constants.INITIAL.equals(mongoReplicatorConfig.getDataSync())) {
            InitSync initSync = new InitSync(mongoReplicatorConfig, mongoClient, filter, mongoReplicator);
            initSync.start();
        }

        MongoDatabase localDataBase = mongoClient.getDatabase(Constants.MONGO_LOCAL_DATABASE);
        FindIterable<Document> iterable;
        if (mongoReplicatorConfig.getPositionTimeStamp() > 0 && mongoReplicatorConfig.getPositionTimeStamp() < System.currentTimeMillis()) {
            iterable = localDataBase.getCollection(Constants.MONGO_OPLOG_RS).find(
                    Filters.gt("ts", mongoReplicatorConfig.getPositionTimeStamp()));
        } else {
            iterable = localDataBase.getCollection(Constants.MONGO_OPLOG_RS).find();
        }
        MongoCursor<Document> cursor = iterable.sort(new Document("$natural", 1))
                .noCursorTimeout(true)
                .cursorType(CursorType.TailableAwait)
                .batchSize(200)
                .iterator();

        while (mongoReplicator.isRuning()) {
            try {
                executorCursor(cursor);
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                logger.error("mongoReplicator shutdown.....");
                mongoReplicator.shutdown();
            }
        }
    }


    private void executorCursor(MongoCursor<Document> cursor) {
        while (cursor.hasNext() && !mongoReplicator.isPause()) {
            Document document = cursor.next();
            ReplicationEvent event = DocumentConvertEvent.convert(document);
            if (filter.filterEvent(event)) {
                mongoReplicator.publishEvent(event);
            }
        }
    }


}
