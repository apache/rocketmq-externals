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

package org.apache.connect.mongo.replicator;

import com.mongodb.CursorType;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.apache.connect.mongo.initsync.InitSync;
import org.apache.connect.mongo.replicator.event.Document2EventConverter;
import org.apache.connect.mongo.replicator.event.ReplicationEvent;
import org.bson.BsonTimestamp;
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

        BsonTimestamp firstAvailablePosition = findFirstOplogPosition();

        Position userConfigOrRuntimePosition = replicaSetConfig.getPosition();

        boolean needDataSync = !userConfigOrRuntimePosition.isValid()
            || userConfigOrRuntimePosition.isInitSync()
            // userConfigOrRuntimePosition.position < firstAvailablePosition maybe lost some operations
            || userConfigOrRuntimePosition.converBsonTimeStamp().compareTo(firstAvailablePosition) < 0;

        if (needDataSync) {
            recordLastOplogPosition();
            InitSync initSync = new InitSync(replicaSetConfig, mongoClient, replicaSetsContext, replicaSet);
            initSync.start();

        }

        if (!replicaSet.isRuning() || replicaSetsContext.isInitSyncAbort()) {
            return;
        }

        MongoDatabase localDataBase = mongoClient.getDatabase(Constants.MONGO_LOCAL_DATABASE);
        FindIterable<Document> iterable = localDataBase.getCollection(Constants.MONGO_OPLOG_RS).find(
            Filters.gt("ts", replicaSetConfig.getPosition().converBsonTimeStamp()));

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

    private BsonTimestamp findFirstOplogPosition() {
        MongoDatabase localDataBase = mongoClient.getDatabase(Constants.MONGO_LOCAL_DATABASE);
        FindIterable<Document> iterable = localDataBase.getCollection(Constants.MONGO_OPLOG_RS).find();
        Document lastOplog = iterable.sort(new Document("$natural", 1)).limit(1).first();
        BsonTimestamp timestamp = lastOplog.get(Constants.TIMESTAMP, BsonTimestamp.class);
        return timestamp;
    }

    private void recordLastOplogPosition() {
        MongoDatabase localDataBase = mongoClient.getDatabase(Constants.MONGO_LOCAL_DATABASE);
        FindIterable<Document> iterable = localDataBase.getCollection(Constants.MONGO_OPLOG_RS).find();
        Document lastOplog = iterable.sort(new Document("$natural", -1)).limit(1).first();
        BsonTimestamp timestamp = lastOplog.get(Constants.TIMESTAMP, BsonTimestamp.class);
        replicaSetConfig.setPosition(new Position(timestamp.getTime(), timestamp.getInc(), false));
    }

    private void executorCursor(MongoCursor<Document> cursor) {
        while (cursor.hasNext() && !replicaSet.isPause()) {
            Document document = cursor.next();
            ReplicationEvent event = Document2EventConverter.convert(document, replicaSetConfig.getReplicaSetName());
            if (replicaSetsContext.filterEvent(event)) {
                replicaSetsContext.publishEvent(event, replicaSetConfig);
            }
        }
    }

}
