package org.apache.rocketmq.redis.test.connector;

import java.util.List;

import io.openmessaging.KeyValue;
import io.openmessaging.internal.DefaultKeyValue;
import org.apache.rocketmq.connect.redis.connector.RedisSourceConnector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RedisSourceConnectorTest {
    private KeyValue keyValue;

    @Before
    public void initKeyValue(){
        this.keyValue = new DefaultKeyValue();
        this.keyValue.put("redisAddr", "127.0.0.1");
        this.keyValue.put("redisPort", "6379");
        this.keyValue.put("redisPassword", "");
    }
    @Test
    public void testConnector(){
        RedisSourceConnector connector = new RedisSourceConnector();
        connector.verifyAndSetConfig(this.keyValue);
        Class cl = connector.taskClass();
        Assert.assertNotNull(cl);
        List<KeyValue> keyValues = connector.taskConfigs();
        Assert.assertNotNull(keyValues);
        connector.start();
        connector.pause();
        connector.resume();
        connector.stop();
    }
}
