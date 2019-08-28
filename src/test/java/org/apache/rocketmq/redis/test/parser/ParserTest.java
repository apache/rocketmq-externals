package org.apache.rocketmq.redis.test.parser;

import java.util.List;
import java.util.Map;

import com.moilioncircle.redis.replicator.rdb.datatype.ExpiredType;
import org.apache.rocketmq.connect.redis.common.Options;
import org.apache.rocketmq.connect.redis.parser.AppendParser;
import org.apache.rocketmq.connect.redis.parser.BitFieldParser;
import org.apache.rocketmq.connect.redis.parser.BitOpParser;
import org.apache.rocketmq.connect.redis.parser.BrPopLPushParser;
import org.apache.rocketmq.connect.redis.parser.DecrByParser;
import org.apache.rocketmq.connect.redis.parser.DecrParser;
import org.apache.rocketmq.connect.redis.parser.DelParser;
import org.apache.rocketmq.connect.redis.parser.EvalParser;
import org.apache.rocketmq.connect.redis.parser.EvalShaParser;
import org.apache.rocketmq.connect.redis.parser.ExecParser;
import org.apache.rocketmq.connect.redis.parser.ExpireAtParser;
import org.apache.rocketmq.connect.redis.parser.ExpireParser;
import org.apache.rocketmq.connect.redis.parser.FlushAllParser;
import org.apache.rocketmq.connect.redis.parser.FlushDbParser;
import org.apache.rocketmq.connect.redis.parser.GeoAddParser;
import org.apache.rocketmq.connect.redis.parser.GetsetParser;
import org.apache.rocketmq.connect.redis.parser.HDelParser;
import org.apache.rocketmq.connect.redis.parser.HIncrByParser;
import org.apache.rocketmq.connect.redis.parser.HSetNxParser;
import org.apache.rocketmq.connect.redis.parser.HSetParser;
import org.apache.rocketmq.connect.redis.parser.HmSetParser;
import org.apache.rocketmq.connect.redis.parser.IncrByParser;
import org.apache.rocketmq.connect.redis.parser.IncrParser;
import org.apache.rocketmq.connect.redis.parser.LPopParser;
import org.apache.rocketmq.connect.redis.parser.LPushParser;
import org.apache.rocketmq.connect.redis.parser.LPushXParser;
import org.apache.rocketmq.connect.redis.parser.LRemParser;
import org.apache.rocketmq.connect.redis.parser.LSetParser;
import org.apache.rocketmq.connect.redis.parser.LTrimParser;
import org.apache.rocketmq.connect.redis.parser.LinsertParser;
import org.apache.rocketmq.connect.redis.parser.MSetNxParser;
import org.apache.rocketmq.connect.redis.parser.MSetParser;
import org.apache.rocketmq.connect.redis.parser.MoveParser;
import org.apache.rocketmq.connect.redis.parser.MultiParser;
import org.apache.rocketmq.connect.redis.parser.PExpireAtParser;
import org.apache.rocketmq.connect.redis.parser.PExpireParser;
import org.apache.rocketmq.connect.redis.parser.PSetExParser;
import org.apache.rocketmq.connect.redis.parser.PersistParser;
import org.apache.rocketmq.connect.redis.parser.PfAddParser;
import org.apache.rocketmq.connect.redis.parser.PfCountParser;
import org.apache.rocketmq.connect.redis.parser.PfMergeParser;
import org.apache.rocketmq.connect.redis.parser.PublishParser;
import org.apache.rocketmq.connect.redis.parser.RPopLPushParser;
import org.apache.rocketmq.connect.redis.parser.RPopParser;
import org.apache.rocketmq.connect.redis.parser.RPushParser;
import org.apache.rocketmq.connect.redis.parser.RPushXParser;
import org.apache.rocketmq.connect.redis.parser.RenameNxParser;
import org.apache.rocketmq.connect.redis.parser.RenameParser;
import org.apache.rocketmq.connect.redis.parser.RestoreParser;
import org.apache.rocketmq.connect.redis.parser.SAddParser;
import org.apache.rocketmq.connect.redis.parser.SDiffStoreParser;
import org.apache.rocketmq.connect.redis.parser.SInterStoreParser;
import org.apache.rocketmq.connect.redis.parser.SMoveParser;
import org.apache.rocketmq.connect.redis.parser.SRemParser;
import org.apache.rocketmq.connect.redis.parser.SUnionStoreParser;
import org.apache.rocketmq.connect.redis.parser.ScriptParser;
import org.apache.rocketmq.connect.redis.parser.SelectParser;
import org.apache.rocketmq.connect.redis.parser.SetBitParser;
import org.apache.rocketmq.connect.redis.parser.SetExParser;
import org.apache.rocketmq.connect.redis.parser.SetNxParser;
import org.apache.rocketmq.connect.redis.parser.SetParser;
import org.apache.rocketmq.connect.redis.parser.SetRangeParser;
import org.apache.rocketmq.connect.redis.parser.SortParser;
import org.apache.rocketmq.connect.redis.parser.SwapDbParser;
import org.apache.rocketmq.connect.redis.parser.UnLinkParser;
import org.apache.rocketmq.connect.redis.parser.XAckParser;
import org.apache.rocketmq.connect.redis.parser.XAddParser;
import org.apache.rocketmq.connect.redis.parser.XClaimParser;
import org.apache.rocketmq.connect.redis.parser.XDelParser;
import org.apache.rocketmq.connect.redis.parser.XGroupParser;
import org.apache.rocketmq.connect.redis.parser.XSetIdParser;
import org.apache.rocketmq.connect.redis.parser.XTrimParser;
import org.apache.rocketmq.connect.redis.parser.ZAddParser;
import org.apache.rocketmq.connect.redis.parser.ZIncrByParser;
import org.apache.rocketmq.connect.redis.parser.ZInterStoreParser;
import org.apache.rocketmq.connect.redis.parser.ZPopMaxParser;
import org.apache.rocketmq.connect.redis.parser.ZPopMinParser;
import org.apache.rocketmq.connect.redis.parser.ZRemParser;
import org.apache.rocketmq.connect.redis.parser.ZRemRangeByLexParser;
import org.apache.rocketmq.connect.redis.parser.ZRemRangeByRankParser;
import org.apache.rocketmq.connect.redis.parser.ZRemRangeByScoreParser;
import org.apache.rocketmq.connect.redis.parser.ZUnionStoreParser;
import org.apache.rocketmq.connect.redis.pojo.Geo;
import org.apache.rocketmq.connect.redis.pojo.KVEntry;
import org.junit.Assert;
import org.junit.Test;

public class ParserTest {

    private Object[] parseCommand(String command){
        String[] commandArr = command.split(" ");
        Object[] res = new Object[commandArr.length];
        for (int i = 0; i < commandArr.length; i++) {
            String sg = commandArr[i];
            res[i] = sg.getBytes();
        }
        return res;
    }

    @Test
    public void testAppendParser(){
        String command = "APPEND k1 v1";
        KVEntry builder = new AppendParser().parse(parseCommand(command));
        Assert.assertEquals("APPEND", builder.getCommand());
        Assert.assertEquals("k1", builder.getKey());
        Assert.assertEquals("v1", builder.getValue());
    }

    @Test
    public void testBitFieldParser(){
        String command = "BITFIELD k1";
        KVEntry builder = new BitFieldParser().parse(parseCommand(command));
        Assert.assertEquals("BITFIELD", builder.getCommand());
        Assert.assertEquals("k1", builder.getKey());
    }

    @Test
    public void testBitOpParser(){
        String command = "BITOP operation destkey k1 k2";
        KVEntry builder = new BitOpParser().parse(parseCommand(command));
        Assert.assertEquals("BITOP", builder.getCommand());
        Assert.assertEquals("operation", builder.getKey());
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(3, res.size());
        Assert.assertEquals("destkey", res.get(0));
        Assert.assertEquals("k1", res.get(1));
        Assert.assertEquals("k2", res.get(2));
    }


    @Test
    public void testBrPopLPushParser(){
        String command = "BRPOPLPUSH source destination 500";
        KVEntry builder = new BrPopLPushParser().parse(parseCommand(command));
        Assert.assertEquals("BRPOPLPUSH", builder.getCommand());
        Assert.assertEquals("source", builder.getKey());
        Assert.assertEquals("destination", builder.getValue());
        Assert.assertTrue(500L == builder.getParam(Options.REDIS_TIMEOUT));
    }


    @Test
    public void testDecrByParser(){
        String command = "DECRBY key 100";
        KVEntry builder = new DecrByParser().parse(parseCommand(command));
        Assert.assertEquals("DECRBY", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertTrue(100L == builder.getParam(Options.REDIS_INCREMENT));
    }

    @Test
    public void testDecrParser(){
        String command = "DECR key";
        KVEntry builder = new DecrParser().parse(parseCommand(command));
        Assert.assertEquals("DECR", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
    }

    @Test
    public void testDelParser(){
        String command = "del k1 k2 k3";
        KVEntry builder = new DelParser().parse(parseCommand(command));
        Assert.assertEquals("del", builder.getCommand());
        Assert.assertEquals("k1", builder.getKey());
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(3, res.size());
        Assert.assertEquals("k1", res.get(0));
        Assert.assertEquals("k2", res.get(1));
        Assert.assertEquals("k3", res.get(2));
    }

    @Test
    public void testEvalParser(){
        String command = "EVAL script 2 k1 k2 arg1 arg2";
        KVEntry builder = new EvalParser().parse(parseCommand(command));
        Assert.assertEquals("EVAL", builder.getCommand());
        Assert.assertEquals("script", builder.getKey());
        List<List<String>> res = (List<List<String>>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals(2, res.get(0).size());
        Assert.assertEquals(2, res.get(1).size());
        Assert.assertEquals("k1", res.get(0).get(0));
        Assert.assertEquals("k2", res.get(0).get(1));
        Assert.assertEquals("arg1", res.get(1).get(0));
        Assert.assertEquals("arg2", res.get(1).get(1));
    }

    @Test
    public void testEvalShaParser(){
        String command = "EVAL sha1 2 k1 k2 arg1 arg2";
        KVEntry builder = new EvalShaParser().parse(parseCommand(command));
        Assert.assertEquals("EVAL", builder.getCommand());
        Assert.assertEquals("sha1", builder.getKey());
        List<List<String>> res = (List<List<String>>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals(2, res.get(0).size());
        Assert.assertEquals(2, res.get(1).size());
        Assert.assertEquals("k1", res.get(0).get(0));
        Assert.assertEquals("k2", res.get(0).get(1));
        Assert.assertEquals("arg1", res.get(1).get(0));
        Assert.assertEquals("arg2", res.get(1).get(1));
    }

    @Test
    public void testExecParser(){
        String command = "exec";
        KVEntry builder = new ExecParser().parse(parseCommand(command));
        Assert.assertEquals("exec", builder.getCommand());
    }

    @Test
    public void testExpireAtParser(){
        String command = "expireat 500";
        KVEntry builder = new ExpireAtParser().parse(parseCommand(command));
        Assert.assertEquals("expireat", builder.getCommand());
        Assert.assertEquals("500", builder.getKey());
        Assert.assertTrue(500 == builder.getParam(Options.REDIS_EX_TIMESTAMP));
    }

    @Test
    public void testExpireParser(){
        String command = "expire key 500";
        KVEntry builder = new ExpireParser().parse(parseCommand(command));
        Assert.assertEquals("expire", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertTrue(500L == builder.getParam(Options.EXPIRED_TIME));
        Assert.assertEquals(ExpiredType.SECOND, builder.getParam(Options.EXPIRED_TYPE));
    }

    @Test
    public void testFlushAllParser(){
        String command = "FLUSHALL ASYNC";
        KVEntry builder = new FlushAllParser().parse(parseCommand(command));
        Assert.assertEquals("FLUSHALL", builder.getCommand());
        Assert.assertEquals("ASYNC", builder.getKey());
    }

    @Test
    public void testFlushDbParser(){
        String command = "FLUSHDB ASYNC";
        KVEntry builder = new FlushDbParser().parse(parseCommand(command));
        Assert.assertEquals("FLUSHDB", builder.getCommand());
        Assert.assertEquals("ASYNC", builder.getKey());
    }

    @Test
    public void testGeoAddParser(){
        String command = "GEOADD K1 100 20 M1 300 90 M2";
        KVEntry builder = new GeoAddParser().parse(parseCommand(command));
        Assert.assertEquals("GEOADD", builder.getCommand());
        Assert.assertEquals("K1", builder.getKey());
        List<Geo> res = (List<Geo>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals(100, res.get(0).getLongitude());
        Assert.assertEquals(20, res.get(0).getLatitude());
        Assert.assertEquals("M1", res.get(0).getMember());
        Assert.assertEquals(300, res.get(1).getLongitude());
        Assert.assertEquals(90, res.get(1).getLatitude());
        Assert.assertEquals("M2", res.get(1).getMember());

    }

    @Test
    public void testGetsetParser(){
        String command = "Getset key value";
        KVEntry builder = new GetsetParser().parse(parseCommand(command));
        Assert.assertEquals("Getset", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertEquals("value", builder.getValue());
    }

    @Test
    public void testHDelParser(){
        String command = "HDEL key f1 f2 f3";
        KVEntry builder = new HDelParser().parse(parseCommand(command));
        Assert.assertEquals("HDEL", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(3, res.size());
        Assert.assertEquals("f1", res.get(0));
        Assert.assertEquals("f2", res.get(1));
        Assert.assertEquals("f3", res.get(2));
    }

    @Test
    public void testHIncrByParser(){
        String command = "HINCRBY key field 100";
        KVEntry builder = new HIncrByParser().parse(parseCommand(command));
        Assert.assertEquals("HINCRBY", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertEquals("field", builder.getValue());
        Assert.assertTrue(100L == builder.getParam(Options.REDIS_INCREMENT));
    }

    @Test
    public void testHmSetParser(){
        String command = "hmset key f1 v1 f2 v2";
        KVEntry builder = new HmSetParser().parse(parseCommand(command));
        Assert.assertEquals("hmset", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Map<String, String> res = (Map<String, String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("v1", res.get("f1"));
        Assert.assertEquals("v2", res.get("f2"));
    }

    @Test
    public void testHSetNxParser(){
        String command = "hsetnx key field value";
        KVEntry builder = new HSetNxParser().parse(parseCommand(command));
        Assert.assertEquals("hsetnx", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Map<String, String> res = (Map<String, String>)builder.getValue();
        Assert.assertEquals(1, res.size());
        Assert.assertEquals("value", res.get("field"));
    }

    @Test
    public void testHSetParser(){
        String command = "hset key field value";
        KVEntry builder = new HSetParser().parse(parseCommand(command));
        Assert.assertEquals("hset", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Map<String, String> res = (Map<String, String>)builder.getValue();
        Assert.assertEquals(1, res.size());
        Assert.assertEquals("value", res.get("field"));
    }

    @Test
    public void testIncrByParser(){
        String command = "INCRBY key 100";
        KVEntry builder = new IncrByParser().parse(parseCommand(command));
        Assert.assertEquals("INCRBY", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertTrue(100L == builder.getParam(Options.REDIS_INCREMENT));
    }

    @Test
    public void testIncrParser(){
        String command = "INCR key";
        KVEntry builder = new IncrParser().parse(parseCommand(command));
        Assert.assertEquals("INCR", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
    }

    @Test
    public void testLinsertParserBefore(){
        String command = "Linsert key BEFORE pivot value";
        KVEntry builder = new LinsertParser().parse(parseCommand(command));
        Assert.assertEquals("Linsert", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("pivot", res.get(0));
        Assert.assertEquals("value", res.get(1));
        Assert.assertEquals(true, builder.getParam(Options.REDIS_BEFORE));
    }

    @Test
    public void testLinsertParserAfter(){
        String command = "Linsert key AFTER pivot value";
        KVEntry builder = new LinsertParser().parse(parseCommand(command));
        Assert.assertEquals("Linsert", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("pivot", res.get(0));
        Assert.assertEquals("value", res.get(1));
        Assert.assertEquals(true, builder.getParam(Options.REDIS_AFTER));
    }

    @Test
    public void testLPopParser(){
        String command = "LPOP key";
        KVEntry builder = new LPopParser().parse(parseCommand(command));
        Assert.assertEquals("LPOP", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
    }

    @Test
    public void testLPushParser(){
        String command = "LPUSH key v1 v2";
        KVEntry builder = new LPushParser().parse(parseCommand(command));
        Assert.assertEquals("LPUSH", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("v1", res.get(0));
        Assert.assertEquals("v2", res.get(1));
    }

    @Test
    public void testLPushXParser(){
        String command = "LPUSHX key value";
        KVEntry builder = new LPushXParser().parse(parseCommand(command));
        Assert.assertEquals("LPUSHX", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertEquals("value", builder.getValue());
    }

    @Test
    public void testLRemParser(){
        String command = "LRem key 100 value";
        KVEntry builder = new LRemParser().parse(parseCommand(command));
        Assert.assertEquals("LRem", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertEquals("value", builder.getValue());
        Assert.assertTrue(100L == builder.getParam(Options.REDIS_COUNT));
    }

    @Test
    public void testLSetParser(){
        String command = "LSet key 10 value";
        KVEntry builder = new LSetParser().parse(parseCommand(command));
        Assert.assertEquals("LSet", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertEquals("value", builder.getValue());
        Assert.assertTrue(10L == builder.getParam(Options.REDIS_INDEX));
    }

    @Test
    public void testLTrimParser(){
        String command = "LTrim key 0 -1";
        KVEntry builder = new LTrimParser().parse(parseCommand(command));
        Assert.assertEquals("LTrim", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("0", res.get(0));
        Assert.assertEquals("-1", res.get(1));
    }

    @Test
    public void testMoveParser(){
        String command = "move key 1";
        KVEntry builder = new MoveParser().parse(parseCommand(command));
        Assert.assertEquals("move", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertTrue(1 == builder.getParam(Options.REDIS_DB_INDEX));
    }

    @Test
    public void testMSetNxParser(){
        String command = "msetnx k1 v1 k2 v2";
        KVEntry builder = new MSetNxParser().parse(parseCommand(command));
        Assert.assertEquals("msetnx", builder.getCommand());
        Assert.assertEquals("k1", builder.getKey());
        Map<String, String> res = (Map<String, String>)builder.getValue();
        Assert.assertEquals("v1", res.get("k1"));
        Assert.assertEquals("v2", res.get("k2"));
    }

    @Test
    public void testMSetParser(){
        String command = "mset k1 v1 k2 v2";
        KVEntry builder = new MSetParser().parse(parseCommand(command));
        Assert.assertEquals("mset", builder.getCommand());
        Assert.assertEquals("k1", builder.getKey());
        Map<String, String> res = (Map<String, String>)builder.getValue();
        Assert.assertEquals("v1", res.get("k1"));
        Assert.assertEquals("v2", res.get("k2"));
    }

    @Test
    public void testMultiParser(){
        String command = "Multi";
        KVEntry builder = new MultiParser().parse(parseCommand(command));
        Assert.assertEquals("Multi", builder.getCommand());
    }

    @Test
    public void testPersistParser(){
        String command = "PERSIST key";
        KVEntry builder = new PersistParser().parse(parseCommand(command));
        Assert.assertEquals("PERSIST", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
    }

    @Test
    public void testPExpireAtParser(){
        String command = "PExpireAt key 3600";
        KVEntry builder = new PExpireAtParser().parse(parseCommand(command));
        Assert.assertEquals("PExpireAt", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertTrue(3600L == builder.getParam(Options.REDIS_PX_TIMESTAMP));
    }

    @Test
    public void testPExpireParser(){
        String command = "PExpire key 3600";
        KVEntry builder = new PExpireParser().parse(parseCommand(command));
        Assert.assertEquals("PExpire", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertTrue(3600L == builder.getParam(Options.REDIS_PX));
    }

    @Test
    public void testPfAddParser(){
        String command = "PFADD key e1 e2";
        KVEntry builder = new PfAddParser().parse(parseCommand(command));
        Assert.assertEquals("PFADD", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("e1", res.get(0));
        Assert.assertEquals("e2", res.get(1));
    }

    @Test
    public void testPfCountParser(){
        String command = "PFCOUNT k1 k2";
        KVEntry builder = new PfCountParser().parse(parseCommand(command));
        Assert.assertEquals("PFCOUNT", builder.getCommand());
        Assert.assertEquals("k1", builder.getKey());
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("k1", res.get(0));
        Assert.assertEquals("k2", res.get(1));
    }

    @Test
    public void testPfMergeParser(){
        String command = "PFMERGE destkey sourcekey1 sourcekey2";
        KVEntry builder = new PfMergeParser().parse(parseCommand(command));
        Assert.assertEquals("PFMERGE", builder.getCommand());
        Assert.assertEquals("destkey", builder.getKey());
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("sourcekey1", res.get(0));
        Assert.assertEquals("sourcekey2", res.get(1));
    }

    @Test
    public void testPSetExParser(){
        String command = "PSETEX key 1000 value";
        KVEntry builder = new PSetExParser().parse(parseCommand(command));
        Assert.assertEquals("PSETEX", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertEquals("value", builder.getValue());
        Assert.assertTrue(1000L == builder.getParam(Options.REDIS_PX));
    }

    @Test
    public void testPublishParser(){
        String command = "Publish channel message";
        KVEntry builder = new PublishParser().parse(parseCommand(command));
        Assert.assertEquals("Publish", builder.getCommand());
        Assert.assertEquals("channel", builder.getKey());
        Assert.assertEquals("message", builder.getValue());
    }

    @Test
    public void testRenameNxParser(){
        String command = "RENAMENX key newkey";
        KVEntry builder = new RenameNxParser().parse(parseCommand(command));
        Assert.assertEquals("RENAMENX", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertEquals("newkey", builder.getValue());
    }

    @Test
    public void testRenameParser(){
        String command = "RENAME key newkey";
        KVEntry builder = new RenameParser().parse(parseCommand(command));
        Assert.assertEquals("RENAME", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertEquals("newkey", builder.getValue());
    }

    @Test
    public void testRestoreParser(){
        String command = "Restore key 300 serialized-value REPLACE";
        KVEntry builder = new RestoreParser().parse(parseCommand(command));
        Assert.assertEquals("Restore", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertEquals("serialized-value", builder.getValue());
        Assert.assertTrue(300L == builder.getParam(Options.REDIS_TTL));
        Assert.assertEquals(true, builder.getParam(Options.REDIS_REPLACE));
    }

    @Test
    public void testRPopLPushParser(){
        String command = "RPOPLPUSH source destination";
        KVEntry builder = new RPopLPushParser().parse(parseCommand(command));
        Assert.assertEquals("RPOPLPUSH", builder.getCommand());
        Assert.assertEquals("source", builder.getKey());
        Assert.assertEquals("destination", builder.getValue());
    }

    @Test
    public void testRPopParser(){
        String command = "RPOP key";
        KVEntry builder = new RPopParser().parse(parseCommand(command));
        Assert.assertEquals("RPOP", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
    }

    @Test
    public void testRPushParser(){
        String command = "RPUSH key v1 v2";
        KVEntry builder = new RPushParser().parse(parseCommand(command));
        Assert.assertEquals("RPUSH", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("v1", res.get(0));
        Assert.assertEquals("v2", res.get(1));
    }

    @Test
    public void testRPushXParser(){
        String command = "RPUSHX key value";
        KVEntry builder = new RPushXParser().parse(parseCommand(command));
        Assert.assertEquals("RPUSHX", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertEquals("value", builder.getValue());
    }

    @Test
    public void testSAddParser(){
        String command = "SADD key m1 m2";
        KVEntry builder = new SAddParser().parse(parseCommand(command));
        Assert.assertEquals("SADD", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("m1", res.get(0));
        Assert.assertEquals("m2", res.get(1));
    }

    @Test
    public void testScriptParser(){
        String command = "Script";
        KVEntry builder = new ScriptParser().parse(parseCommand(command));
        Assert.assertEquals("Script", builder.getCommand());
    }

    @Test
    public void testSDiffStoreParser(){
        String command = "SDIFFSTORE destination k1 k2";
        KVEntry builder = new SDiffStoreParser().parse(parseCommand(command));
        Assert.assertEquals("SDIFFSTORE", builder.getCommand());
        Assert.assertEquals("destination", builder.getKey());
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("k1", res.get(0));
        Assert.assertEquals("k2", res.get(1));
    }

    @Test
    public void testSelectParser(){
        String command = "Select 1";
        KVEntry builder = new SelectParser().parse(parseCommand(command));
        Assert.assertEquals("Select", builder.getCommand());
        Assert.assertEquals("1", builder.getKey());
        Assert.assertTrue(1 == builder.getParam(Options.REDIS_DB_INDEX));
    }

    @Test
    public void testSetBitParser(){
        String command = "SetBit key 100 value";
        KVEntry builder = new SetBitParser().parse(parseCommand(command));
        Assert.assertEquals("SetBit", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertEquals("value", builder.getValue());
        Assert.assertTrue(100L == builder.getParam(Options.REDIS_OFFSET));
    }

    @Test
    public void testSetExParser(){
        String command = "setex key 100 value";
        KVEntry builder = new SetExParser().parse(parseCommand(command));
        Assert.assertEquals("setex", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertEquals("value", builder.getValue());
        Assert.assertTrue(100 == builder.getParam(Options.REDIS_EX));
    }

    @Test
    public void testSetNxParser(){
        String command = "SETNX a b";
        KVEntry builder = new SetNxParser().parse(parseCommand(command));
        Assert.assertEquals("SETNX", builder.getCommand());
        Assert.assertEquals("a", builder.getKey());
        Assert.assertEquals("b", builder.getValue());
    }

    @Test
    public void testSetParser1(){
        String command = "set key value EX 100 NX";
        KVEntry builder = new SetParser().parse(parseCommand(command));
        Assert.assertEquals("set", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertEquals("value", builder.getValue());
        Assert.assertTrue(100 == builder.getParam(Options.REDIS_EX));
        Assert.assertEquals(true, builder.getParam(Options.REDIS_NX));
    }

    @Test
    public void testSetParser2(){
        String command = "set key value EX 100";
        KVEntry builder = new SetParser().parse(parseCommand(command));
        Assert.assertEquals("set", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertEquals("value", builder.getValue());
        Assert.assertTrue(100 == builder.getParam(Options.REDIS_EX));
    }

    @Test
    public void testSetParser3(){
        String command = "set key value NX";
        KVEntry builder = new SetParser().parse(parseCommand(command));
        Assert.assertEquals("set", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertEquals("value", builder.getValue());
        Assert.assertEquals(true, builder.getParam(Options.REDIS_NX));
    }

    @Test
    public void testSetParser4(){
        String command = "set key value PX 100 XX";
        KVEntry builder = new SetParser().parse(parseCommand(command));
        Assert.assertEquals("set", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertEquals("value", builder.getValue());
        Assert.assertTrue(100L == builder.getParam(Options.REDIS_PX));
        Assert.assertEquals(true, builder.getParam(Options.REDIS_XX));
    }

    @Test
    public void testSetRangeParser(){
        String command = "SETRANGE key 100 value";
        KVEntry builder = new SetRangeParser().parse(parseCommand(command));
        Assert.assertEquals("SETRANGE", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertEquals("value", builder.getValue());
        Assert.assertTrue(100L == builder.getParam(Options.REDIS_OFFSET));
    }

    @Test
    public void testSInterStoreParser(){
        String command = "SINTERSTORE destination k1 k2";
        KVEntry builder = new SInterStoreParser().parse(parseCommand(command));
        Assert.assertEquals("SINTERSTORE", builder.getCommand());
        Assert.assertEquals("destination", builder.getKey());
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("k1", res.get(0));
        Assert.assertEquals("k2", res.get(1));
    }

    @Test
    public void testSMoveParser(){
        String command = "SMOVE source destination member";
        KVEntry builder = new SMoveParser().parse(parseCommand(command));
        Assert.assertEquals("SMOVE", builder.getCommand());
        Assert.assertEquals("source", builder.getKey());
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("destination", res.get(0));
        Assert.assertEquals("member", res.get(1));
    }

    @Test
    public void testSortParser(){
        String command = "sort key";
        KVEntry builder = new SortParser().parse(parseCommand(command));
        Assert.assertEquals("sort", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
    }

    @Test
    public void testSRemParser(){
        String command = "SREM key m1 m2";
        KVEntry builder = new SRemParser().parse(parseCommand(command));
        Assert.assertEquals("SREM", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("m1", res.get(0));
        Assert.assertEquals("m2", res.get(1));
    }

    @Test
    public void testSUnionStoreParser(){
        String command = "SUNIONSTORE destination k1 k2";
        KVEntry builder = new SUnionStoreParser().parse(parseCommand(command));
        Assert.assertEquals("SUNIONSTORE", builder.getCommand());
        Assert.assertEquals("destination", builder.getKey());
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("k1", res.get(0));
        Assert.assertEquals("k2", res.get(1));
    }

    @Test
    public void testSwapDbParser(){
        String command = "SWAPDB 0 1";
        KVEntry builder = new SwapDbParser().parse(parseCommand(command));
        Assert.assertEquals("SWAPDB", builder.getCommand());
        Assert.assertEquals("0", builder.getKey());
        Assert.assertEquals("1", builder.getValue());
    }

    @Test
    public void testUnLinkParser(){
        String command = "UnLink k1 k2";
        KVEntry builder = new UnLinkParser().parse(parseCommand(command));
        Assert.assertEquals("UnLink", builder.getCommand());
        Assert.assertEquals("k1", builder.getKey());
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("k1", res.get(0));
        Assert.assertEquals("k2", res.get(1));
    }

    @Test
    public void testXAckParser(){
        String command = "XAck key group 1526569498055-0 1526569498055-1";
        KVEntry builder = new XAckParser().parse(parseCommand(command));
        Assert.assertEquals("XAck", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertEquals("group", builder.getParam(Options.REDIS_GROUP));
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("1526569498055-0", res.get(0));
        Assert.assertEquals("1526569498055-1", res.get(1));
    }

    @Test
    public void testXAddParser(){
        String command = "XAdd key ID f1 v1 f2 v2";
        KVEntry builder = new XAddParser().parse(parseCommand(command));
        Assert.assertEquals("XAdd", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertEquals("ID", builder.getParam(Options.REDIS_ID));
        Map<String, String> res = (Map<String, String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("v1", res.get("f1"));
        Assert.assertEquals("v2", res.get("f2"));
    }

    @Test
    public void testXClaimParser1(){
        // XCLAIM key group consumer min-idle-time ID [ID ...] [IDLE ms] [TIME ms-unix-time] [RETRYCOUNT count] [force]
        String command = "XCLAIM mystream mygroup Alice 3600000 1526569498055-0 1526569498055-1";
        KVEntry builder = new XClaimParser().parse(parseCommand(command));
        Assert.assertEquals("XCLAIM", builder.getCommand());
        Assert.assertEquals("mystream", builder.getKey());
        Assert.assertEquals("mygroup", builder.getParam(Options.REDIS_GROUP));
        Assert.assertEquals("Alice", builder.getParam(Options.REDIS_CONSUMER));
        Assert.assertTrue(3600000L == builder.getParam(Options.REDIS_MIN_IDLE_TIME));
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("1526569498055-0", res.get(0));
        Assert.assertEquals("1526569498055-1", res.get(1));
    }

    @Test
    public void testXClaimParser2(){
        // XCLAIM key group consumer min-idle-time ID [ID ...] [IDLE ms] [TIME ms-unix-time] [RETRYCOUNT count] [force]
        String command = "XCLAIM mystream mygroup Alice 3600000 1526569498055-0 1526569498055-1 IDLE 100 TIME 3000 RETRYCOUNT 10 force";
        KVEntry builder = new XClaimParser().parse(parseCommand(command));
        Assert.assertEquals("XCLAIM", builder.getCommand());
        Assert.assertEquals("mystream", builder.getKey());
        Assert.assertEquals("mygroup", builder.getParam(Options.REDIS_GROUP));
        Assert.assertEquals("Alice", builder.getParam(Options.REDIS_CONSUMER));
        Assert.assertTrue(3600000L == builder.getParam(Options.REDIS_MIN_IDLE_TIME));
        Assert.assertTrue(100L == builder.getParam(Options.REDIS_IDLE));
        Assert.assertTrue(3000L == builder.getParam(Options.REDIS_TIME));
        Assert.assertTrue(10 == builder.getParam(Options.REDIS_RETRYCOUNT));
        Assert.assertEquals(true, builder.getParam(Options.REDIS_FORCE));
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("1526569498055-0", res.get(0));
        Assert.assertEquals("1526569498055-1", res.get(1));
    }

    @Test
    public void testXDelParser(){
        String command = "XDel key 1526569498055-0 1526569498055-1";
        KVEntry builder = new XDelParser().parse(parseCommand(command));
        Assert.assertEquals("XDel", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("1526569498055-0", res.get(0));
        Assert.assertEquals("1526569498055-1", res.get(1));
    }

    @Test
    public void testXGroupParser(){
        String command = "XGroup [CREATE key groupname id-or-$] [SETID key id-or-$] [DESTROY key groupname] [DELCONSUMER key groupname consumername]";
        KVEntry builder = new XGroupParser().parse(parseCommand(command));
        Assert.assertEquals("XGroup", builder.getCommand());
    }

    @Test
    public void testXSetIdParser(){
        String command = "XSetId key arg";
        KVEntry builder = new XSetIdParser().parse(parseCommand(command));
        Assert.assertEquals("XSetId", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertEquals("arg", builder.getValue());
    }

    @Test
    public void testXTrimParser(){
        String command = "XTrim key MAXLEN ~ 100";
        KVEntry builder = new XTrimParser().parse(parseCommand(command));
        Assert.assertEquals("XTrim", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertEquals(true, builder.getParam(Options.REDIS_XTRIM));
    }

    @Test
    public void testXTrimParser2(){
        String command = "XTrim key MAXLEN 100";
        KVEntry builder = new XTrimParser().parse(parseCommand(command));
        Assert.assertEquals("XTrim", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertEquals(false, builder.getParam(Options.REDIS_XTRIM));
    }

    @Test
    public void testZAddParser1(){
        // ZADD key [NX|XX] [CH] [INCR] score member [score member ...]
        String command = "ZADD key 100 m1 80 m2";
        KVEntry builder = new ZAddParser().parse(parseCommand(command));
        Assert.assertEquals("ZADD", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Map<String, String> res = (Map<String, String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("100", res.get("m1"));
        Assert.assertEquals("80", res.get("m2"));
    }

    @Test
    public void testZAddParser2(){
        // ZADD key [NX|XX] [CH] [INCR] score member [score member ...]
        String command = "ZADD key NX CH INCR 100 m1 80 m2";
        KVEntry builder = new ZAddParser().parse(parseCommand(command));
        Assert.assertEquals("ZADD", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Map<String, String> res = (Map<String, String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("100", res.get("m1"));
        Assert.assertEquals("80", res.get("m2"));
        Assert.assertEquals(true, builder.getParam(Options.REDIS_NX));
        Assert.assertEquals(true, builder.getParam(Options.REDIS_CH));
        Assert.assertEquals(true, builder.getParam(Options.REDIS_INCR));
    }

    @Test
    public void testZAddParser3(){
        // ZADD key [NX|XX] [CH] [INCR] score member [score member ...]
        String command = "ZADD key XX CH INCR 100 m1 80 m2";
        KVEntry builder = new ZAddParser().parse(parseCommand(command));
        Assert.assertEquals("ZADD", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Map<String, String> res = (Map<String, String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("100", res.get("m1"));
        Assert.assertEquals("80", res.get("m2"));
        Assert.assertEquals(true, builder.getParam(Options.REDIS_XX));
        Assert.assertEquals(true, builder.getParam(Options.REDIS_CH));
        Assert.assertEquals(true, builder.getParam(Options.REDIS_INCR));
    }

    @Test
    public void testZIncrByParser(){
        String command = "ZINCRBY key 100 member";
        KVEntry builder = new ZIncrByParser().parse(parseCommand(command));
        Assert.assertEquals("ZINCRBY", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertEquals("member", builder.getValue());
        Assert.assertTrue(100L == builder.getParam(Options.REDIS_INCREMENT));
    }

    @Test
    public void testZInterStoreParser1(){
        // ZINTERSTORE destination numkeys key [key ...] [weights weight] [aggregate SUM|MIN|MAX]
        String command = "ZINTERSTORE destination 2 k1 k2 weights 100 80";
        KVEntry builder = new ZInterStoreParser().parse(parseCommand(command));
        Assert.assertEquals("ZINTERSTORE", builder.getCommand());
        Assert.assertEquals("destination", builder.getKey());
        Map<String,String> res = (Map<String, String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("100", res.get("k1"));
        Assert.assertEquals("80", res.get("k2"));
    }

    @Test
    public void testZInterStoreParser2(){
        // ZINTERSTORE destination numkeys key [key ...] [weights weight] [aggregate SUM|MIN|MAX]
        String command = "ZINTERSTORE destination 2 k1 k2 weights 100 80 aggregate SUM";
        KVEntry builder = new ZInterStoreParser().parse(parseCommand(command));
        Assert.assertEquals("ZINTERSTORE", builder.getCommand());
        Assert.assertEquals("destination", builder.getKey());
        Map<String,String> res = (Map<String, String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("100", res.get("k1"));
        Assert.assertEquals("80", res.get("k2"));
        Assert.assertEquals("SUM", builder.getParam(Options.REDIS_AGGREGATE));
    }

    @Test
    public void testZPopMaxParser(){
        String command = "ZPOPMAX key 100";
        KVEntry builder = new ZPopMaxParser().parse(parseCommand(command));
        Assert.assertEquals("ZPOPMAX", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertTrue(100L == builder.getParam(Options.REDIS_COUNT));
    }

    @Test
    public void testZPopMinParser(){
        String command = "ZPOPMIN key 100";
        KVEntry builder = new ZPopMinParser().parse(parseCommand(command));
        Assert.assertEquals("ZPOPMIN", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        Assert.assertTrue(100L == builder.getParam(Options.REDIS_COUNT));
    }

    @Test
    public void testZRemParser(){
        String command = "ZRem key m1 m2";
        KVEntry builder = new ZRemParser().parse(parseCommand(command));
        Assert.assertEquals("ZRem", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("m1", res.get(0));
        Assert.assertEquals("m2", res.get(1));
    }


    @Test
    public void testZRemRangeByLexParser(){
        String command = "ZRemRangeByLex key 0 10";
        KVEntry builder = new ZRemRangeByLexParser().parse(parseCommand(command));
        Assert.assertEquals("ZRemRangeByLex", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("0", res.get(0));
        Assert.assertEquals("10", res.get(1));
    }

    @Test
    public void testZRemRangeByRankParser(){
        String command = "ZREMRANGEBYRANK key 0 10";
        KVEntry builder = new ZRemRangeByRankParser().parse(parseCommand(command));
        Assert.assertEquals("ZREMRANGEBYRANK", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("0", res.get(0));
        Assert.assertEquals("10", res.get(1));
    }

    @Test
    public void testZRemRangeByScoreParser(){
        String command = "ZREMRANGEBYSCORE key 0 10";
        KVEntry builder = new ZRemRangeByScoreParser().parse(parseCommand(command));
        Assert.assertEquals("ZREMRANGEBYSCORE", builder.getCommand());
        Assert.assertEquals("key", builder.getKey());
        List<String> res = (List<String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("0", res.get(0));
        Assert.assertEquals("10", res.get(1));
    }

    @Test
    public void testZUnionStoreParser1(){
        // ZUNIONSTORE destination numkeys key [key ...] [weights weight] [aggregate SUM|MIN|MAX]
        String command = "ZUNIONSTORE destination 2 k1 k2 weights 100 80";
        KVEntry builder = new ZUnionStoreParser().parse(parseCommand(command));
        Assert.assertEquals("ZUNIONSTORE", builder.getCommand());
        Assert.assertEquals("destination", builder.getKey());
        Map<String,String> res = (Map<String, String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("100", res.get("k1"));
        Assert.assertEquals("80", res.get("k2"));
    }

    @Test
    public void testZUnionStoreParser2(){
        // ZUNIONSTORE destination numkeys key [key ...] [weights weight] [aggregate SUM|MIN|MAX]
        String command = "ZUNIONSTORE destination 2 k1 k2 weights 100 80 aggregate SUM";
        KVEntry builder = new ZUnionStoreParser().parse(parseCommand(command));
        Assert.assertEquals("ZUNIONSTORE", builder.getCommand());
        Assert.assertEquals("destination", builder.getKey());
        Map<String,String> res = (Map<String, String>)builder.getValue();
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("100", res.get("k1"));
        Assert.assertEquals("80", res.get("k2"));
        Assert.assertEquals("SUM", builder.getParam(Options.REDIS_AGGREGATE));
    }

}
