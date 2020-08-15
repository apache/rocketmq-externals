package org.apache.rocketmq.redis.test.pojo;

import com.moilioncircle.redis.replicator.rdb.datatype.KeyStringValueString;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyValuePair;
import org.apache.rocketmq.connect.redis.pojo.RedisEvent;
import org.junit.Assert;
import org.junit.Test;

public class RedisEventTest {
    @Test
    public void test(){
        RedisEvent redisEvent = new RedisEvent();
        redisEvent.setEvent(getKeyValuePair());
        redisEvent.setReplOffset(3926872L);
        redisEvent.setReplId("c18cece63c7b16851a6f387f52dbbb9eee07e46f");
        redisEvent.setStreamDB(0);

        Assert.assertEquals("c18cece63c7b16851a6f387f52dbbb9eee07e46f", redisEvent.getReplId());
        Assert.assertTrue(3926872L == redisEvent.getReplOffset());
        Assert.assertTrue(0 == redisEvent.getStreamDB());
        Assert.assertNotNull(redisEvent.getEvent());
        Assert.assertEquals(KeyStringValueString.class, redisEvent.getEvent().getClass());
        Assert.assertEquals("key", new String(((KeyStringValueString)redisEvent.getEvent()).getKey()));
        Assert.assertEquals("value", new String(((KeyStringValueString)redisEvent.getEvent()).getValue()));
    }

    private KeyValuePair getKeyValuePair(){
        KeyValuePair pair = new KeyStringValueString();
        pair.setKey("key".getBytes());
        pair.setValue("value".getBytes());
        return pair;
    }
}
