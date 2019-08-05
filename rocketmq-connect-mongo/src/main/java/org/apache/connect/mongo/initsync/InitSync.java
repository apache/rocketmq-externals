package org.apache.connect.mongo.initsync;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import org.apache.connect.mongo.replicator.event.DocumentConvertEvent;
import org.apache.connect.mongo.replicator.event.OperationType;
import org.apache.connect.mongo.replicator.event.ReplicationEvent;
import org.apache.connect.mongo.replicator.Filter;
import org.apache.connect.mongo.replicator.MongoReplicator;
import org.apache.connect.mongo.MongoReplicatorConfig;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class InitSync {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private MongoReplicatorConfig mongoReplicatorConfig;
    private ExecutorService copyExecutor;
    private MongoClient mongoClient;
    private Filter filter;
    private int copyThreadCount;
    private Set<String> interestDataBases;
    private Set<CollectionMeta> interestCollections;
    private CountDownLatch countDownLatch;
    private MongoReplicator mongoReplicator;

    public InitSync(MongoReplicatorConfig mongoReplicatorConfig, MongoClient mongoClient, Filter filter, MongoReplicator mongoReplicator) {
        this.mongoReplicatorConfig = mongoReplicatorConfig;
        this.mongoClient = mongoClient;
        this.filter = filter;
        this.mongoReplicator = mongoReplicator;
        init();
    }

    public void start() {
        for (CollectionMeta collectionMeta : interestCollections) {
            copyExecutor.submit(new CopyRunner(mongoClient, countDownLatch, collectionMeta, mongoReplicator));
        }
        try {
            countDownLatch.await();
        } catch (Exception e) {
        } finally {
            copyExecutor.shutdown();
        }
    }

    private void init() {
        interestDataBases = getInterestDataBase();
        interestCollections = getInterestCollection(interestDataBases);
        copyThreadCount = Math.min(interestCollections.size(), mongoReplicatorConfig.getCopyThread());
        copyExecutor = Executors.newFixedThreadPool(copyThreadCount, new ThreadFactory() {

            AtomicInteger threads = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "copy_collection_thread_" + threads.incrementAndGet());
            }
        });
        countDownLatch = new CountDownLatch(interestCollections.size());
    }

    private Set<CollectionMeta> getInterestCollection(Set<String> interestDataBases) {
        Set<CollectionMeta> res = new HashSet<>();
        for (String interestDataBase : interestDataBases) {
            MongoIterable<String> collectionNames = mongoClient.getDatabase(interestDataBase).listCollectionNames();
            MongoCursor<String> iterator = collectionNames.iterator();
            while (iterator.hasNext()) {
                String collectionName = iterator.next();
                if (filter.filterCollectionName(collectionName)) {
                    CollectionMeta collectionMeta = new CollectionMeta(interestDataBase, collectionName);
                    res.add(collectionMeta);
                }
            }
        }

        return res;

    }

    private Set<String> getInterestDataBase() {
        Set<String> res = new HashSet<>();
        MongoIterable<String> databaseNames = mongoClient.listDatabaseNames();
        MongoCursor<String> iterator = databaseNames.iterator();
        while (iterator.hasNext()) {
            String dataBaseName = iterator.next();
            if (filter.filterDatabaseName(dataBaseName)) {
                res.add(dataBaseName);
            }
        }

        return res;
    }

    class CopyRunner implements Runnable {

        private MongoClient mongoClient;
        private CountDownLatch countDownLatch;
        private CollectionMeta collectionMeta;
        private MongoReplicator mongoReplicator;

        public CopyRunner(MongoClient mongoClient, CountDownLatch countDownLatch, CollectionMeta collectionMeta, MongoReplicator mongoReplicator) {
            this.mongoClient = mongoClient;
            this.countDownLatch = countDownLatch;
            this.collectionMeta = collectionMeta;
            this.mongoReplicator = mongoReplicator;
        }

        @Override
        public void run() {

            try {

                MongoCursor<Document> mongoCursor = mongoClient.getDatabase(collectionMeta.getDatabaseName())
                        .getCollection(collectionMeta.getCollectionName())
                        .find()
                        .batchSize(200)
                        .iterator();

                while (mongoReplicator.isRuning() && mongoCursor.hasNext()) {
                    Document document = mongoCursor.next();
                    ReplicationEvent event = DocumentConvertEvent.convert(document);
                    event.setOperationType(OperationType.CREATED);
                    event.setNamespace(collectionMeta.getNameSpace());
                    mongoReplicator.publishEvent(event);
                }
            } finally {
                countDownLatch.countDown();
            }
            logger.info("database:{}, collection:{}, init sync done", collectionMeta.getDatabaseName(), collectionMeta.getCollectionName());
        }
    }

}




