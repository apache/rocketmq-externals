package org.apache.connect.mongo;

import org.apache.connect.mongo.replicator.MongoReplicator;
import org.junit.Before;
import org.junit.Test;

public class ReplicatorTest {

    private MongoReplicatorConfig config;

    @Before
    public void before() {
        config = new MongoReplicatorConfig();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoPort() {
        config.setMongoAddr("127.0.0.1");
        MongoReplicator mongoReplicator = new MongoReplicator(config);
        mongoReplicator.start();
    }


    @Test
    public void testNoPort1() {
        config.setMongoAddr("127.0.0.1");
        config.setMongoPort(27012);
        MongoReplicator mongoReplicator = new MongoReplicator(config);
        mongoReplicator.start();
    }


}
