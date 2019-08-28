package org.apache.rocketmq.redis.test.util;

import java.util.List;
import java.util.Map;

import org.apache.rocketmq.connect.redis.util.ParseStringUtils;
import org.junit.Assert;
import org.junit.Test;

public class ParseStringUtilsTest {
    private String info = "# Replication\n"
        + "role:master\n"
        + "connected_slaves:2\n"
        + "slave0:ip=127.0.0.1,port=64690,state=online,offset=3926872,lag=1\n"
        + "slave1:ip=127.0.0.1,port=64691,state=online,offset=3926872,lag=1\n"
        + "master_replid:c18cece63c7b16851a6f387f52dbbb9eee07e46f\n"
        + "master_replid2:0000000000000000000000000000000000000000\n"
        + "master_repl_offset:3926872\n"
        + "second_repl_offset:-1\n"
        + "repl_backlog_active:1\n"
        + "repl_backlog_size:1048576\n"
        + "repl_backlog_first_byte_offset:3862270\n"
        + "repl_backlog_histlen:64603";
    @Test
    public void test(){
        Map<String, String> map = ParseStringUtils.parseRedisInfo2Map(info);
        Assert.assertTrue(map.size() == 12);
        Assert.assertEquals("c18cece63c7b16851a6f387f52dbbb9eee07e46f", map.get("master_replid"));
        Assert.assertEquals("1048576", map.get("repl_backlog_size"));
    }

    private String commands = "SET,APPEND,HMSET";
    @Test
    public void testParseCommands(){
        List<String> res = ParseStringUtils.parseCommands(commands);
        Assert.assertNotNull(res);
        Assert.assertEquals(3, res.size());
        Assert.assertEquals("SET", res.get(0));
        Assert.assertEquals("APPEND", res.get(1));
        Assert.assertEquals("HMSET", res.get(2));

        res = ParseStringUtils.parseCommands("  ");
        Assert.assertNull(res);
    }
}
