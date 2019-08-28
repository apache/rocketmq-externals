package org.apache.rocketmq.redis.test.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.openmessaging.connector.api.data.DataEntry;
import io.openmessaging.connector.api.data.EntryType;
import io.openmessaging.connector.api.data.FieldType;
import io.openmessaging.connector.api.data.SourceDataEntry;
import org.apache.rocketmq.connect.redis.common.Options;
import org.apache.rocketmq.connect.redis.converter.KVEntryConverter;
import org.apache.rocketmq.connect.redis.converter.RedisEntryConverter;
import org.apache.rocketmq.connect.redis.pojo.KVEntry;
import org.apache.rocketmq.connect.redis.pojo.RedisEntry;
import org.junit.Assert;
import org.junit.Test;

public class KVEntryTest {

    @Test
    public void testConstruct(){
        KVEntry entry = new RedisEntry(FieldType.STRING);
        entry.value("value");
        Assert.assertEquals(String.class, entry.getValue().getClass());


        entry = new RedisEntry("partition", FieldType.ARRAY);
        entry.value(new ArrayList<>());
        Assert.assertEquals(ArrayList.class, entry.getValue().getClass());
        Assert.assertEquals("partition", entry.getPartition());


        entry = RedisEntry.newEntry(FieldType.MAP);
        entry.value(new HashMap());
        Assert.assertEquals(HashMap.class, entry.getValue().getClass());
        entry = RedisEntry.newEntry("partition", FieldType.INT64);


        entry.value(123L);
        Assert.assertEquals(Long.class, entry.getValue().getClass());


        Assert.assertTrue(123L == (long)entry.getValue());
        Assert.assertEquals("partition", entry.getPartition());
        Assert.assertNotNull(entry.toString());


        List<SourceDataEntry> entries = getConverter().kVEntryToDataEntries(entry);
        Assert.assertNotNull(entries);
    }

    @Test
    public void test(){
        RedisEntry entry = RedisEntry.newEntry(FieldType.STRING);
        entry.partition("partition");
        entry.queueName("queue1");
        entry.entryType(EntryType.UPDATE);
        entry.sourceId("replId");
        entry.offset(65535L);
        entry.command("set");
        entry.key("key");
        entry.valueType(FieldType.STRING);
        entry.value("value");
        entry.param(Options.REDIS_INCREMENT, 15L);

        Assert.assertEquals("partition", entry.getPartition());
        Assert.assertEquals("queue1", entry.getQueueName());
        Assert.assertEquals(EntryType.UPDATE, entry.getEntryType());
        Assert.assertEquals("replId", entry.getSourceId());
        Assert.assertEquals(65535L, (long)entry.getOffset());
        Assert.assertEquals("set", entry.getCommand());
        Assert.assertEquals("key", entry.getKey());
        Assert.assertEquals(FieldType.STRING, entry.getValueType());
        Assert.assertEquals("value", entry.getValue());
        Assert.assertEquals(15L, (long)entry.getParam(Options.REDIS_INCREMENT));
        Assert.assertEquals(15L, (long)entry.getParams().get(Options.REDIS_INCREMENT.name()));
    }


    private KVEntryConverter getConverter(){
        return new RedisEntryConverter();
    }
}
