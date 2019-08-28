package org.apache.rocketmq.redis.test.processor;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.moilioncircle.redis.replicator.rdb.datatype.KeyStringValueSet;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyStringValueString;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyValuePair;
import org.apache.rocketmq.connect.redis.common.Config;
import org.apache.rocketmq.connect.redis.common.SyncMod;
import org.apache.rocketmq.connect.redis.handler.DefaultRedisEventHandler;
import org.apache.rocketmq.connect.redis.handler.RedisEventHandler;
import org.apache.rocketmq.connect.redis.pojo.KVEntry;
import org.apache.rocketmq.connect.redis.pojo.RedisEvent;
import org.apache.rocketmq.connect.redis.processor.DefaultRedisEventProcessor;
import org.apache.rocketmq.connect.redis.processor.RedisEventProcessor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import redis.clients.jedis.exceptions.JedisConnectionException;

import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_SET;

public class ProcessorTest {
    private RedisEventProcessor processor;

    @Test
    public void testHandler() throws Exception {
        processor = getProcessor();
        DefaultRedisEventProcessor defaultRedisEventProcessor = (DefaultRedisEventProcessor)processor;


        RedisEvent event = new RedisEvent();
        event.setEvent(getKeyValuePair());
        event.setReplOffset(123L);
        event.setStreamDB(0);
        event.setReplId("asdfsdfa");
        defaultRedisEventProcessor.commit(event);

        KVEntry res = null;
        try {
            res = defaultRedisEventProcessor.poll();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertNotNull(res);


        event.setEvent(getKVCommandPair());
        defaultRedisEventProcessor.commit(event);
        try {
            res = defaultRedisEventProcessor.poll();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertNotNull(res);
    }

    private KeyValuePair getKVCommandPair(){
        KeyValuePair event = new KeyStringValueSet();
        event.setValueRdbType(RDB_TYPE_SET);
        event.setKey("mySet".getBytes());
        Set<byte[]> values= new HashSet<>();
        values.add("myValue".getBytes());
        values.add("myValue2".getBytes());
        event.setValue(values);
        return event;
    }

    @Test
    public void testRepeatStart() throws IOException {
        try{
            processor = getProcessor();
            processor.start();
            Assert.assertFalse(processor.isStopped());
            processor.start();
            Assert.assertFalse(processor.isStopped());

            processor.stop();
            Assert.assertTrue(processor.isStopped());
            processor.stop();
            Assert.assertTrue(processor.isStopped());
        }catch (JedisConnectionException e){

        }
    }

    @Test
    public void testSyncMod() throws IOException {
        RedisEventProcessor processor = null;
        try{
            processor = getProcessor(SyncMod.LAST_OFFSET_FORCE);
            processor.start();

            processor = getProcessor(SyncMod.CUSTOM_OFFSET_FORCE);
            processor.start();

            processor = getProcessor(SyncMod.LAST_OFFSET);
            processor.start();

            processor = getProcessor(SyncMod.CUSTOM_OFFSET);
            processor.start();
        }catch (JedisConnectionException e){

        }
    }

    @Test
    public void test(){
        processor = getProcessor();
        RedisEvent event = getRedisEvent();
        Exception ex = null;
        try {
            processor.commit(event);
        } catch (Exception e) {
            e.printStackTrace();
            ex = e;
        }
        Assert.assertNull(ex);

        try {
            KVEntry entry = processor.poll();
            Assert.assertEquals("key", entry.getKey());
            Assert.assertEquals("value", entry.getValue());
            Assert.assertEquals("c18cece63c7b16851a6f387f52dbbb9eee07e46f", entry.getSourceId());
        } catch (InterruptedException e) {
            e.printStackTrace();
            ex = e;
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertNull(ex);
    }

    @After
    public void stop() throws IOException {
        if(this.processor != null){
            this.processor.stop();
        }
    }

    private RedisEvent getRedisEvent(){
        RedisEvent redisEvent = new RedisEvent();
        redisEvent.setEvent(getKeyValuePair());
        redisEvent.setReplOffset(3926872L);
        redisEvent.setReplId("c18cece63c7b16851a6f387f52dbbb9eee07e46f");
        redisEvent.setStreamDB(0);
        return redisEvent;
    }

    private KeyValuePair getKeyValuePair(){
        KeyValuePair pair = new KeyStringValueString();
        pair.setKey("key".getBytes());
        pair.setValue("value".getBytes());
        return pair;
    }

    private RedisEventProcessor getProcessor(){
        return getProcessor(getConfig());
    }

    private RedisEventProcessor getProcessor(SyncMod syncMod){
        return getProcessor(getConfig(syncMod));
    }

    private RedisEventProcessor getProcessor(Config config){
        RedisEventHandler eventHandler = new DefaultRedisEventHandler(config);
        RedisEventProcessor processor = new DefaultRedisEventProcessor(config);
        processor.registEventHandler(eventHandler);
        return processor;
    }

    private Config getConfig(){
        return getConfig(SyncMod.CUSTOM_OFFSET);
    }

    private Config getConfig(SyncMod syncMod){
        Config config = new Config();

        config.setRedisAddr("127.0.0.1");
        config.setRedisPort(6379);
        config.setOffset(100L);
        config.setPosition(200L);
        config.setCommands("SET,HSET");

        if(syncMod != null){
            config.setSyncMod(syncMod.name());
        }
        return config;
    }

}
