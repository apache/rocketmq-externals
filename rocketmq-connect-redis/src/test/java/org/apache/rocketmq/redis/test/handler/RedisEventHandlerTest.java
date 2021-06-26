package org.apache.rocketmq.redis.test.handler;

import com.moilioncircle.redis.replicator.cmd.impl.GetSetCommand;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.moilioncircle.redis.replicator.rdb.datatype.AuxField;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyStringValueList;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyStringValueString;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyValuePair;
import com.moilioncircle.redis.replicator.rdb.datatype.Module;
import com.moilioncircle.redis.replicator.rdb.datatype.Stream;
import com.moilioncircle.redis.replicator.rdb.datatype.ZSetEntry;
import com.moilioncircle.redis.replicator.rdb.iterable.datatype.BatchedKeyStringValueString;
import com.moilioncircle.redis.replicator.rdb.iterable.datatype.BatchedKeyValuePair;
import io.openmessaging.connector.api.data.FieldType;
import org.apache.rocketmq.connect.redis.common.Config;
import org.apache.rocketmq.connect.redis.common.SyncMod;
import org.apache.rocketmq.connect.redis.handler.DefaultRedisEventHandler;
import org.apache.rocketmq.connect.redis.handler.RedisEventHandler;
import org.apache.rocketmq.connect.redis.pojo.KVEntry;
import org.apache.rocketmq.connect.redis.pojo.RedisEntry;
import org.junit.Assert;
import org.junit.Test;

import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_HASH;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_HASH_ZIPLIST;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_HASH_ZIPMAP;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_LIST;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_LIST_QUICKLIST;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_LIST_ZIPLIST;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_MODULE;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_MODULE_2;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_SET;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_SET_INTSET;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_STREAM_LISTPACKS;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_STRING;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_ZSET;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_ZSET_2;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_ZSET_ZIPLIST;

public class RedisEventHandlerTest {
    private String replId = "c18cece63c7b16851a6f387f52dbbb9eee07e46f";
    private Long offset = 3926872L;

    @Test
    public void testNull() {
        Config config = getConfig();
        RedisEventHandler handler = new DefaultRedisEventHandler(config);
        KVEntry res = null;
        Exception ex = null;

        // 测试increment下的rdb数据处理
        config.setSyncMod(SyncMod.LAST_OFFSET.name());
        KeyValuePair keyValuePair = new KeyStringValueString();
        keyValuePair.setKey("key".getBytes());
        keyValuePair.setValue("value".getBytes());
        try {
            res = handler.handleKVString(replId, offset, keyValuePair);
        } catch (Exception e) {
            e.printStackTrace();
            ex = e;
        }
        Assert.assertNull(res);
        Assert.assertNull(ex);

        // 测试未指定的command
        GetSetCommand command = new GetSetCommand();
        command.setKey("key".getBytes());
        command.setValue("value".getBytes());
        try {
            res = handler.handleCommand(replId, offset, command);
        } catch (Exception e) {
            ex = e;
        }
        Assert.assertNull(res);
        Assert.assertNull(ex);



    }

    @Test
    public void test() {
        Config config = getConfig();
        RedisEventHandler handler = new DefaultRedisEventHandler(config);
        KVEntry entry = getBuilder();

        KVEntry res = null;
        Exception ex = null;
        try {
            res = handler.handleCommand(replId, offset, entry);
        } catch (Exception e) {
            e.printStackTrace();
            ex = e;
        }
        Assert.assertEquals("c18cece63c7b16851a6f387f52dbbb9eee07e46f", res.getSourceId());
        Assert.assertTrue(res.getOffset() == 3926872L);
        Assert.assertNull(ex);
        AuxField auxField = new AuxField("a", "b");
        try {
            res = handler.handleOtherEvent(replId, offset, auxField);
        } catch (Exception e) {
            e.printStackTrace();
            ex = e;
        }
        Assert.assertNull(res);
        Assert.assertNull(ex);

        BatchedKeyValuePair pair = new BatchedKeyStringValueString();
        pair.setKey("A".getBytes());
        pair.setValue("B".getBytes());
        try {
            res = handler.handleBatchKVString(replId, offset, pair);
        } catch (Exception e) {
            e.printStackTrace();
            ex = e;
        }
        Assert.assertNull(res);
        Assert.assertNull(ex);

    }

    @Test
    public void testKeyValuePair() {
        KVEntry kvEntry =
            handlerTest(RDB_TYPE_STRING, () ->
                "value".getBytes()
            );
        Assert.assertEquals(replId, kvEntry.getSourceId());
        Assert.assertTrue(kvEntry.getOffset() == 3926872L);
        Assert.assertEquals("key", kvEntry.getKey());
        Assert.assertEquals("value", kvEntry.getValue());

    }

    @Test
    public void testList() {
        KVEntry builder =
            handlerTest(RDB_TYPE_LIST, () -> {
                List<byte[]> values = new ArrayList<>();
                values.add("a".getBytes());
                return values;
            });
        List<String> va = (List)builder.getValue();
        Assert.assertNotNull(va);
        Assert.assertEquals(1, va.size());
        Assert.assertEquals("a", va.get(0));
    }

    @Test
    public void testSet() {
        KVEntry builder =
            handlerTest(RDB_TYPE_SET, () -> {
                Set<byte[]> values = new HashSet<>();
                values.add("myValue".getBytes());
                values.add("myValue2".getBytes());
                return values;
            });

        List<String> va = (List<String>)builder.getValue();
        Assert.assertNotNull(va);
        Assert.assertEquals(2, va.size());
        Assert.assertTrue(va.contains("myValue"));
        Assert.assertTrue(va.contains("myValue2"));
    }


    @Test
    public void testZSET() {
        KVEntry builder =
            handlerTest(RDB_TYPE_ZSET, () -> {
                Set<ZSetEntry> values = new HashSet<>();
                values.add(new ZSetEntry("key1".getBytes(), 100));
                values.add(new ZSetEntry("key2".getBytes(), 80));
                return values;
            });

        Map<String, Double> va = (Map<String, Double>)builder.getValue();
        Assert.assertNotNull(va);
        Assert.assertEquals(2, va.size());
        Assert.assertTrue(100 == va.get("key1"));
        Assert.assertTrue(80 ==  va.get("key2"));
    }


    @Test
    public void testZSet2() {
        KVEntry builder =
            handlerTest(RDB_TYPE_ZSET_2, () -> {
                Set<ZSetEntry> values = new HashSet<>();
                values.add(new ZSetEntry("key1".getBytes(), 100));
                values.add(new ZSetEntry("key2".getBytes(), 80));
                return values;
            });

        Map<String, Double> va = (Map<String, Double>)builder.getValue();
        Assert.assertNotNull(va);
        Assert.assertEquals(2, va.size());
        Assert.assertTrue(100 == va.get("key1"));
        Assert.assertTrue(80 ==  va.get("key2"));
    }


    @Test
    public void testHash() {
        KVEntry builder =
            handlerTest(RDB_TYPE_HASH, () -> {
                Map<byte[], byte[]> values = new HashMap<>();
                values.put("key1".getBytes(), "value1".getBytes());
                values.put("key2".getBytes(), "value2".getBytes());
                return values;
            });

        Map<String, String> va = (Map<String, String>)builder.getValue();
        Assert.assertNotNull(va);
        Assert.assertEquals(2, va.size());
        Assert.assertEquals("value1", va.get("key1"));
        Assert.assertEquals("value2", va.get("key2"));
    }


    @Test
    public void testHashZipMap() {
        KVEntry builder =
            handlerTest(RDB_TYPE_HASH_ZIPMAP, () -> {
                Map<byte[], byte[]> values = new HashMap<>();
                values.put("key1".getBytes(), "value1".getBytes());
                values.put("key2".getBytes(), "value2".getBytes());
                return values;
            });

        Map<String, String> va = (Map<String, String>)builder.getValue();
        Assert.assertNotNull(va);
        Assert.assertEquals(2, va.size());
        Assert.assertEquals("value1", va.get("key1"));
        Assert.assertEquals("value2", va.get("key2"));
    }

    @Test
    public void testListZipList() {
        KVEntry builder =
            handlerTest(RDB_TYPE_LIST_ZIPLIST, () -> {
                List<byte[]> values = new ArrayList<>();
                values.add("v1".getBytes());
                values.add("v2".getBytes());
                return values;
            });

        List<String> va = (List<String>)builder.getValue();
        Assert.assertNotNull(va);
        Assert.assertEquals(2, va.size());
        Assert.assertEquals("v1", va.get(0));
        Assert.assertEquals("v2", va.get(1));
    }

    @Test
    public void testSetIntSet() {
        KVEntry builder =
            handlerTest(RDB_TYPE_SET_INTSET, () -> {
                Set<byte[]> values = new HashSet<>();
                values.add("v1".getBytes());
                values.add("v2".getBytes());
                return values;
            });

        List<String> va = (List<String>)builder.getValue();
        Assert.assertNotNull(va);
        Assert.assertEquals(2, va.size());
        Assert.assertTrue(va.contains("v1"));
        Assert.assertTrue(va.contains("v2"));
    }


    @Test
    public void testZSetZipList() {
        KVEntry builder =
            handlerTest(RDB_TYPE_ZSET_ZIPLIST, () -> {
                Set<ZSetEntry> values = new HashSet<>();
                values.add(new ZSetEntry("v1".getBytes(), 100));
                values.add(new ZSetEntry("v2".getBytes(), 80));
                return values;
            });

        Map<String, Double> va = (Map<String, Double>)builder.getValue();
        Assert.assertNotNull(va);
        Assert.assertEquals(2, va.size());
        Assert.assertTrue(100 == va.get("v1"));
        Assert.assertTrue(80 == va.get("v2"));
    }

    @Test
    public void testHashZipList() {
        KVEntry builder =
            handlerTest(RDB_TYPE_HASH_ZIPLIST, () -> {
                Map<byte[], byte[]> values = new HashMap<>();
                values.put("k1".getBytes(), "v1".getBytes());
                values.put("k2".getBytes(), "v2".getBytes());
                return values;
            });

        Map<String, String> va = (Map<String, String>)builder.getValue();
        Assert.assertNotNull(va);
        Assert.assertEquals(2, va.size());
        Assert.assertEquals("v1", va.get("k1"));
        Assert.assertEquals("v2", va.get("k2"));
    }

    @Test
    public void testListQuickList() {
        KVEntry builder =
            handlerTest(RDB_TYPE_LIST_QUICKLIST, () -> {
                List<byte[]> values = new ArrayList<>();
                values.add("v1".getBytes());
                values.add("v2".getBytes());
                return values;
            });

        List<String> va = (List<String>)builder.getValue();
        Assert.assertNotNull(va);
        Assert.assertEquals(2, va.size());
        Assert.assertEquals("v1", va.get(0));
        Assert.assertEquals("v2", va.get(1));
    }

    @Test
    public void testModule() {
        KVEntry builder =
            handlerTest(RDB_TYPE_MODULE, () -> {
                Module module = new Module() {
                    @Override
                    public int hashCode() {
                        return super.hashCode();
                    }

                    @Override
                    public boolean equals(Object obj) {
                        return super.equals(obj);
                    }

                    @Override
                    protected Object clone() throws CloneNotSupportedException {
                        return super.clone();
                    }

                    @Override
                    public String toString() {
                        return super.toString();
                    }

                    @Override
                    protected void finalize() throws Throwable {
                        super.finalize();
                    }
                };
                return module;
            });

        Module va = (Module)builder.getValue();
        Assert.assertNotNull(va);
    }

    @Test
    public void testModule2() {
        KVEntry builder =
            handlerTest(RDB_TYPE_MODULE_2, () -> {
                Module module = new Module() {
                    @Override
                    public int hashCode() {
                        return super.hashCode();
                    }

                    @Override
                    public boolean equals(Object obj) {
                        return super.equals(obj);
                    }

                    @Override
                    protected Object clone() throws CloneNotSupportedException {
                        return super.clone();
                    }

                    @Override
                    public String toString() {
                        return super.toString();
                    }

                    @Override
                    protected void finalize() throws Throwable {
                        super.finalize();
                    }
                };
                return module;
            });

        Module va = (Module)builder.getValue();
        Assert.assertNotNull(va);
    }

    @Test
    public void testStreamListPacks() {
        KVEntry builder =
            handlerTest(RDB_TYPE_STREAM_LISTPACKS, () -> {
                Stream stream = new Stream();
                return stream;
            });

        Stream va = (Stream)builder.getValue();
        Assert.assertNotNull(va);
    }

    @Test
    public void testException() {
        Error ex = null;
        try{
            handlerTest(999, () -> new Object());
        }catch (AssertionError e){
            ex = e;
        }
        Assert.assertNotNull(ex);
    }

    private interface ValueSetter<T> {
        T getValue();
    }


    private <T> KVEntry handlerTest(int rdbType, ValueSetter<T> setter) {
        KVEntry res = null;
        Exception ex = null;
        Config config = getConfig();
        RedisEventHandler handler = new DefaultRedisEventHandler(config);

        KeyValuePair keyValuePair = new KeyStringValueList();
        keyValuePair.setValueRdbType(rdbType);
        keyValuePair.setKey("key".getBytes());

        Object value = setter.getValue();

        keyValuePair.setValue(value);
        try {
            res = handler.handleKVString(replId, offset, keyValuePair);
        } catch (Exception e) {
            e.printStackTrace();
            ex = e;
        }
        Assert.assertNull(ex);
        return res;
    }

    private Config getConfig() {
        Config config = new Config();

        config.setRedisAddr("127.0.0.1");
        config.setRedisPort(6379);
        config.setRedisPassword("123456");
        return config;
    }

    private KVEntry getBuilder() {
        return RedisEntry.newEntry(FieldType.STRING);
    }

}
