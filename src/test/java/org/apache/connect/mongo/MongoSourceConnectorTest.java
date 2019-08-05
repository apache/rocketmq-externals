package org.apache.connect.mongo;

import io.openmessaging.internal.DefaultKeyValue;
import org.apache.commons.lang3.StringUtils;
import org.apache.connect.mongo.connector.MongoSourceConnector;
import org.apache.connect.mongo.connector.MongoSourceTask;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MongoSourceConnectorTest {

    private MongoSourceConnector mongoSourceConnector;
    private DefaultKeyValue keyValue;

    @Before
    public void before() {
        mongoSourceConnector = new MongoSourceConnector();
        keyValue = new DefaultKeyValue();
    }

    @Test
    public void takeClass() {
        Assert.assertEquals(mongoSourceConnector.taskClass(), MongoSourceTask.class);
    }


    @Test
    public void verifyConfig() {
        keyValue.put("mongoAddr", "127.0.0.1");
        String s = mongoSourceConnector.verifyAndSetConfig(keyValue);
        Assert.assertTrue(s.contains("Request config key:"));
        keyValue.put("mongoPort", 27017);
        s = mongoSourceConnector.verifyAndSetConfig(keyValue);
        Assert.assertTrue(StringUtils.isBlank(s));
    }








}
