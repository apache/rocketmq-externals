package org.apache.rocketmq.redis.test.common;

import java.nio.ByteBuffer;
import java.util.Set;

import com.moilioncircle.redis.replicator.RedisURI;
import io.openmessaging.KeyValue;
import io.openmessaging.internal.DefaultKeyValue;
import org.apache.rocketmq.connect.redis.common.Config;
import org.apache.rocketmq.connect.redis.common.SyncMod;
import org.junit.Assert;
import org.junit.Test;

public class ConfigTest {

    @Test
    public void testNull() {
        Config config = new Config();
        RedisURI redisURI = config.getRedisUri();
        ByteBuffer po = config.getPositionPartitionKey();
        Assert.assertNull(redisURI);
        Assert.assertNull(po);

        config.setRedisAddr("127.0.0.1");
        redisURI = config.getRedisUri();
        po = config.getPositionPartitionKey();
        Assert.assertNull(redisURI);
        Assert.assertNull(po);

        config.setRedisPassword("123456");
        redisURI = config.getRedisUri();
        po = config.getPositionPartitionKey();
        Assert.assertNull(redisURI);
        Assert.assertNull(po);

        config.setRedisPort(6379);
        redisURI = config.getRedisUri();
        po = config.getPositionPartitionKey();
        Assert.assertNotNull(redisURI);
        Assert.assertNotNull(po);
    }

    @Test
    public void test() {
        Config config = new Config();

        config.setRedisAddr("127.0.0.1");
        config.setRedisPort(6379);
        config.setRedisPassword("123456");
        config.setTimeout(500);
        config.setSyncMod(SyncMod.CUSTOM_OFFSET.name());
        config.setOffset(65535L);
        config.setReplId("c18cece63c7b16851a6f387f52dbbb9eee07e46f");
        config.setCommands("SET,GET");
        config.setPosition(3926872L);
        config.setEventCommitRetryInterval(1000L);
        config.setEventCommitRetryTimes(10);


        Assert.assertEquals("127.0.0.1", config.getRedisAddr());
        Assert.assertEquals(6379, (int)config.getRedisPort());
        Assert.assertEquals("123456", config.getRedisPassword());
        Assert.assertEquals(500, (int)config.getTimeout());
        Assert.assertEquals(SyncMod.CUSTOM_OFFSET, config.getSyncMod());
        Assert.assertEquals(65535L, (long)config.getOffset());
        Assert.assertEquals("c18cece63c7b16851a6f387f52dbbb9eee07e46f", config.getReplId());
        Assert.assertEquals("SET,GET", config.getCommands());
        Assert.assertEquals(3926872L, (long)config.getPosition());
        Assert.assertEquals(1000L, (long)config.getEventCommitRetryInterval());

        RedisURI redisURI = config.getRedisUri();
        Assert.assertNotNull(redisURI);

        ByteBuffer byteBuffer = config.getPositionPartitionKey();
        Assert.assertNotNull(byteBuffer);

        Set<String> set = Config.getRequestConfig();
        Assert.assertNotNull(set);

        KeyValue keyValue = new DefaultKeyValue();
        keyValue.put("a", "B");
        String bo = Config.checkConfig(keyValue);
        Assert.assertNotNull(bo);

        keyValue.put("redisAddr", "127.0.0.1");
        keyValue.put("redisPort", "6379");

        bo = Config.checkConfig(keyValue);
        Assert.assertNotNull(bo);

        keyValue.put("redisPassword", "1234567");
        bo = Config.checkConfig(keyValue);
        Assert.assertNull(bo);

        keyValue.put("syncMod", SyncMod.LAST_OFFSET.name());

        Config config1 = new Config();
        config1.load(keyValue);
        Assert.assertEquals("127.0.0.1", config.getRedisAddr());
        Assert.assertTrue(6379 == config1.getRedisPort());
        Assert.assertEquals("1234567", config1.getRedisPassword());
        Assert.assertEquals(SyncMod.LAST_OFFSET, config1.getSyncMod());

        String err = config1.load(null);
        Assert.assertNotNull(err);
    }
}
