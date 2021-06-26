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

package org.apache.connect.mongo.initsync;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.connect.mongo.replicator.ReplicaSet;
import org.apache.connect.mongo.replicator.ReplicaSetConfig;
import org.apache.connect.mongo.replicator.ReplicaSetsContext;
import org.apache.connect.mongo.replicator.event.Document2EventConverter;
import org.apache.connect.mongo.replicator.event.OperationType;
import org.apache.connect.mongo.replicator.event.ReplicationEvent;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitSync {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private ReplicaSetConfig replicaSetConfig;
    private ExecutorService copyExecutor;
    private MongoClient mongoClient;
    private ReplicaSetsContext context;
    private int copyThreadCount;
    private Set<CollectionMeta> interestCollections;
    private CountDownLatch countDownLatch;
    private ReplicaSet replicaSet;

    public InitSync(ReplicaSetConfig replicaSetConfig, MongoClient mongoClient, ReplicaSetsContext context,
        ReplicaSet replicaSet) {
        this.replicaSetConfig = replicaSetConfig;
        this.mongoClient = mongoClient;
        this.context = context;
        this.replicaSet = replicaSet;
        init();
    }

    public void start() {
        for (CollectionMeta collectionMeta : interestCollections) {
            copyExecutor.submit(new CopyRunner(mongoClient, countDownLatch, collectionMeta, replicaSet));
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            logger.error("init sync wait countDownLatch interrupted");
        } finally {
            copyExecutor.shutdown();
        }
    }

    private void init() {
        interestCollections = getInterestCollection();
        copyThreadCount = Math.min(interestCollections.size(), context.getCopyThread());
        copyExecutor = Executors.newFixedThreadPool(copyThreadCount, new ThreadFactory() {

            AtomicInteger threads = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "copy_collection_thread_" + threads.incrementAndGet());
            }
        });
        countDownLatch = new CountDownLatch(interestCollections.size());
    }

    private Set<CollectionMeta> getInterestCollection() {
        Set<CollectionMeta> res = new HashSet<>();
        MongoIterable<String> databaseNames = mongoClient.listDatabaseNames();
        MongoCursor<String> dbIterator = databaseNames.iterator();
        while (dbIterator.hasNext()) {
            String dataBaseName = dbIterator.next();
            MongoCursor<String> collIterator = mongoClient.getDatabase(dataBaseName).listCollectionNames().iterator();
            while (collIterator.hasNext()) {
                String collectionName = collIterator.next();
                CollectionMeta collectionMeta = new CollectionMeta(dataBaseName, collectionName);
                if (context.filterMeta(collectionMeta)) {
                    res.add(collectionMeta);
                }
            }
        }

        return res;

    }

    class CopyRunner implements Runnable {

        private MongoClient mongoClient;
        private CountDownLatch countDownLatch;
        private CollectionMeta collectionMeta;
        private ReplicaSet replicaSet;

        public CopyRunner(MongoClient mongoClient, CountDownLatch countDownLatch, CollectionMeta collectionMeta,
            ReplicaSet replicaSet) {
            this.mongoClient = mongoClient;
            this.countDownLatch = countDownLatch;
            this.collectionMeta = collectionMeta;
            this.replicaSet = replicaSet;
        }

        @Override
        public void run() {
            logger.info("start copy database:{}, collection:{}", collectionMeta.getDatabaseName(), collectionMeta.getCollectionName());
            int count = 0;
            try {
                MongoCursor<Document> mongoCursor = mongoClient.getDatabase(collectionMeta.getDatabaseName())
                    .getCollection(collectionMeta.getCollectionName())
                    .find()
                    .batchSize(200)
                    .iterator();
                while (replicaSet.isRuning() && mongoCursor.hasNext()) {
                    if (context.isInitSyncAbort()) {
                        logger.info("init sync database:{}, collection:{} abort, has copy:{} document", collectionMeta.getDatabaseName(), collectionMeta.getCollectionName(), count);
                        return;
                    }
                    count++;
                    Document document = mongoCursor.next();
                    ReplicationEvent event = Document2EventConverter.convert(document, replicaSetConfig.getReplicaSetName());
                    event.setOperationType(OperationType.CREATED);
                    event.setNamespace(collectionMeta.getNameSpace());
                    context.publishEvent(event, replicaSetConfig);
                }

            } catch (Exception e) {
                context.setInitSyncError();
                replicaSet.shutdown();
                logger.error("init sync database:{}, collection:{} error", collectionMeta.getDatabaseName(), collectionMeta.getNameSpace(), e);
            } finally {
                countDownLatch.countDown();
            }
            logger.info("database:{}, collection:{}, copy {} documents, init sync done", collectionMeta.getDatabaseName(), collectionMeta.getCollectionName(), count);
        }
    }

}




