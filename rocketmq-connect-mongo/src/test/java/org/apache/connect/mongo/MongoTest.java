package org.apache.connect.mongo;

import com.alibaba.fastjson.JSONObject;
import com.mongodb.ConnectionString;
import com.mongodb.CursorType;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import io.openmessaging.connector.api.data.EntryType;
import org.apache.connect.mongo.initsync.InitSync;
import org.apache.connect.mongo.replicator.Constants;
import org.apache.connect.mongo.replicator.Filter;
import org.apache.connect.mongo.replicator.MongoReplicator;
import org.apache.connect.mongo.replicator.event.DocumentConvertEvent;
import org.apache.connect.mongo.replicator.event.OperationType;
import org.apache.connect.mongo.replicator.event.ReplicationEvent;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MongoTest {

    private MongoClient mongoClient;

    @Before
    public void before() {
        MongoClientSettings.Builder builder = MongoClientSettings.builder();
        builder.applyConnectionString(new ConnectionString("mongodb://127.0.0.1:27077"));
        mongoClient = MongoClients.create(builder.build());
    }


    @Test
    public void testConvertEvent() {
        Document oplog = new Document();
        BsonTimestamp timestamp = new BsonTimestamp(1565074665, 10);
        oplog.put(Constants.TIMESTAMP, timestamp);
        oplog.put(Constants.NAMESPACE, "test.person");
        oplog.put(Constants.HASH, 11111L);
        oplog.put(Constants.OPERATIONTYPE, "i");
        Document document = new Document();
        document.put("test", "test");
        oplog.put(Constants.OPERATION, document);
        ReplicationEvent event = DocumentConvertEvent.convert(oplog);
        Assert.assertEquals(timestamp, event.getTimestamp());
        Assert.assertEquals("test.person", event.getNamespace());
        Assert.assertTrue(11111L == event.getH());
        Assert.assertEquals(OperationType.INSERT, event.getOperationType());
        Assert.assertEquals(EntryType.CREATE, event.getEntryType());
        Assert.assertEquals(document, event.getEventData().get());


    }


    @Test
    public void testInitSyncCopy() throws NoSuchFieldException, IllegalAccessException, InterruptedException {
        MongoCollection<Document> collection = mongoClient.getDatabase("test").getCollection("person");
        collection.deleteMany(new Document());
        int count = 100;
        List<Document> documents = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Document document = new Document();
            document.put("name", "test" + i);
            document.put("age", i);
            document.put("sex", i % 2 == 0 ? "boy" : "girl");
            collection.insertOne(document);
            documents.add(document);
        }
        MongoReplicatorConfig config = new MongoReplicatorConfig();
        Map<String, List<String>> insterest = new HashMap<>();
        List<String> collections = new ArrayList<>();
        collections.add("*");
        insterest.put("test", collections);
        config.setInterestDbAndCollection(JSONObject.toJSONString(insterest));
        MongoReplicator mongoReplicator = new MongoReplicator(config);
        Field running = MongoReplicator.class.getDeclaredField("running");
        running.setAccessible(true);
        running.set(mongoReplicator, new AtomicBoolean(true));
        InitSync initSync = new InitSync(config, mongoClient, new Filter(config), mongoReplicator);
        initSync.start();
        BlockingQueue<ReplicationEvent> queue = mongoReplicator.getQueue();
        while (count > 0) {
            count--;
            ReplicationEvent event = queue.poll(100, TimeUnit.MILLISECONDS);
            Assert.assertTrue(event.getOperationType().equals(OperationType.CREATED));
            Assert.assertNotNull(event.getDocument());
            Assert.assertTrue(documents.contains(event.getDocument()));
        }



    }
}
