package org.apache.connect.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import org.apache.commons.lang3.StringUtils;
import org.apache.connect.mongo.replicator.ReplicaSetConfig;
import org.apache.connect.mongo.replicator.ReplicaSetsContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ReplicaContextTest {

    private ReplicaSetsContext context;

    @Before
    public void before() {
        SourceTaskConfig sourceTaskConfig = new SourceTaskConfig();
        context = new ReplicaSetsContext(sourceTaskConfig);
    }

    @Test
    public void testCreateMongoClient() {
        MongoClient mongoClient = context.createMongoClient(new ReplicaSetConfig("shardName1", "", "127.0.0.1:27027"));
        MongoIterable<String> collectionNames = mongoClient.getDatabase("local").listCollectionNames();
        MongoCursor<String> iterator = collectionNames.iterator();
        while (iterator.hasNext()) {
            Assert.assertTrue(StringUtils.isNoneBlank(iterator.next()));
        }
    }

}
