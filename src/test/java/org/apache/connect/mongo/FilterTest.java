package org.apache.connect.mongo;

import org.apache.connect.mongo.replicator.Filter;
import org.apache.connect.mongo.replicator.event.OperationType;
import org.apache.connect.mongo.replicator.event.ReplicationEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FilterTest {


    private MongoReplicatorConfig config;


    @Before
    public void init() {
        config = new MongoReplicatorConfig();

    }

    @Test
    public void testSpecialDb() {
        config.setInterestDB("test,admin");
        Filter filter = new Filter(config);
        Assert.assertTrue(filter.filterDatabaseName("test"));
        Assert.assertFalse(filter.filterDatabaseName("test01"));
    }


    @Test
    public void testBlankDb() {
        Filter filter = new Filter(config);
        Assert.assertTrue(filter.filterDatabaseName("test"));
        Assert.assertTrue(filter.filterDatabaseName("test01"));
    }


    @Test
    public void testSpecialCollection() {
        config.setInterestCollection("test,admin");
        Filter filter = new Filter(config);
        Assert.assertTrue(filter.filterCollectionName("test"));
        Assert.assertFalse(filter.filterCollectionName("test01"));
    }


    @Test
    public void testBlankCollection() {
        Filter filter = new Filter(config);
        Assert.assertTrue(filter.filterCollectionName("test"));
        Assert.assertTrue(filter.filterCollectionName("test01"));
    }


    @Test
    public void testFilterEvent() {
        Filter filter = new Filter(config);
        ReplicationEvent replicationEvent = new ReplicationEvent();
        replicationEvent.setOperationType(OperationType.NOOP);
        Assert.assertFalse(filter.filterEvent(replicationEvent));
        replicationEvent.setOperationType(OperationType.DBCOMMAND);
        Assert.assertTrue(filter.filterEvent(replicationEvent));
    }


}
