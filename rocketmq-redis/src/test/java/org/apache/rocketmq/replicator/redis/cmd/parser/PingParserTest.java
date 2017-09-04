/*
 *
 *   Copyright 2016 leon chen
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  modified:
 *    1. rename package from com.moilioncircle.redis.replicator to
 *        org.apache.rocketmq.replicator.redis
 *
 */

package org.apache.rocketmq.replicator.redis.cmd.parser;

import org.apache.rocketmq.replicator.redis.cmd.impl.AggregateType;
import org.apache.rocketmq.replicator.redis.cmd.impl.AppendCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.EvalCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.ExpireAtCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.ExpireCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.GetSetCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.HSetCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.HSetNxCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.LSetCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.MoveCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.PExpireAtCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.PExpireCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.PSetExCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.PingCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.RenameCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.RenameNxCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.RestoreCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.SAddCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.SDiffStoreCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.SInterStoreCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.SUnionStoreCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.ScriptFlushCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.ScriptLoadCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.SelectCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.SetBitCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.SetNxCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.SetRangeCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.ZInterStoreCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.ZUnionStoreCommand;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class PingParserTest {
    @Test
    public void parse() throws Exception {
        {
            PingParser parser = new PingParser();
            PingCommand cmd = parser.parse("ping msg".split(" "));
            assertEquals("msg", cmd.getMessage());
            System.out.println(cmd);
        }

        {
            MoveParser parser = new MoveParser();
            MoveCommand cmd = parser.parse("move key 2".split(" "));
            assertEquals("key", cmd.getKey());
            assertEquals(2, cmd.getDb());
            System.out.println(cmd);
        }

        {
            SelectParser parser = new SelectParser();
            SelectCommand cmd = parser.parse("select 2".split(" "));
            assertEquals(2, cmd.getIndex());
            System.out.println(cmd);
        }

        {
            RenameParser parser = new RenameParser();
            RenameCommand cmd = parser.parse("rename key key1".split(" "));
            assertEquals("key", cmd.getKey());
            assertEquals("key1", cmd.getNewKey());
            System.out.println(cmd);
        }

        {
            RenameNxParser parser = new RenameNxParser();
            RenameNxCommand cmd = parser.parse("renamenx key key1".split(" "));
            assertEquals("key", cmd.getKey());
            assertEquals("key1", cmd.getNewKey());
            System.out.println(cmd);
        }

        {
            AppendParser parser = new AppendParser();
            AppendCommand cmd = parser.parse("append key val".split(" "));
            assertEquals("key", cmd.getKey());
            assertEquals("val", cmd.getValue());
            System.out.println(cmd);
        }

        {
            SetBitParser parser = new SetBitParser();
            SetBitCommand cmd = parser.parse("setbit key 10 0".split(" "));
            assertEquals("key", cmd.getKey());
            assertEquals(10, cmd.getOffset());
            assertEquals(0, cmd.getValue());
            System.out.println(cmd);
        }

        {
            SetRangeParser parser = new SetRangeParser();
            SetRangeCommand cmd = parser.parse("setrange key 10 val".split(" "));
            assertEquals("key", cmd.getKey());
            assertEquals(10, cmd.getIndex());
            assertEquals("val", cmd.getValue());
            System.out.println(cmd);
        }

        {
            GetSetParser parser = new GetSetParser();
            GetSetCommand cmd = parser.parse("getset key val".split(" "));
            assertEquals("key", cmd.getKey());
            assertEquals("val", cmd.getValue());
            System.out.println(cmd);
        }

        {
            HSetNxParser parser = new HSetNxParser();
            HSetNxCommand cmd = parser.parse("hsetnx key fie val".split(" "));
            assertEquals("key", cmd.getKey());
            assertEquals("fie", cmd.getField());
            assertEquals("val", cmd.getValue());
            System.out.println(cmd);
        }

        {
            HSetParser parser = new HSetParser();
            HSetCommand cmd = parser.parse("hset key fie val".split(" "));
            assertEquals("key", cmd.getKey());
            assertEquals("fie", cmd.getField());
            assertEquals("val", cmd.getValue());
            System.out.println(cmd);
        }

        {
            LSetParser parser = new LSetParser();
            LSetCommand cmd = parser.parse("lset key 1 val".split(" "));
            assertEquals("key", cmd.getKey());
            assertEquals(1, cmd.getIndex());
            assertEquals("val", cmd.getValue());
            System.out.println(cmd);
        }

        {
            PSetExParser parser = new PSetExParser();
            PSetExCommand cmd = parser.parse("pset key 1 val".split(" "));
            assertEquals("key", cmd.getKey());
            assertEquals(1, cmd.getEx());
            assertEquals("val", cmd.getValue());
            System.out.println(cmd);
        }

        {
            SAddParser parser = new SAddParser();
            SAddCommand cmd = parser.parse("sadd key v1 v2".split(" "));
            assertEquals("key", cmd.getKey());
            assertEquals("v1", cmd.getMembers()[0]);
            assertEquals("v2", cmd.getMembers()[1]);
            System.out.println(cmd);
        }

        {
            SetNxParser parser = new SetNxParser();
            SetNxCommand cmd = parser.parse("setnx key v1".split(" "));
            assertEquals("key", cmd.getKey());
            assertEquals("v1", cmd.getValue());
            System.out.println(cmd);
        }

        {
            ExpireAtParser parser = new ExpireAtParser();
            ExpireAtCommand cmd = parser.parse("expireat key 5".split(" "));
            assertEquals("key", cmd.getKey());
            assertEquals(5, cmd.getEx());
            System.out.println(cmd);
        }

        {
            ExpireParser parser = new ExpireParser();
            ExpireCommand cmd = parser.parse("expire key 5".split(" "));
            assertEquals("key", cmd.getKey());
            assertEquals(5, cmd.getEx());
            System.out.println(cmd);
        }

        {
            PExpireAtParser parser = new PExpireAtParser();
            PExpireAtCommand cmd = parser.parse("pexpireat key 5".split(" "));
            assertEquals("key", cmd.getKey());
            assertEquals(5, cmd.getEx());
            System.out.println(cmd);
        }

        {
            PExpireParser parser = new PExpireParser();
            PExpireCommand cmd = parser.parse("pexpire key 5".split(" "));
            assertEquals("key", cmd.getKey());
            assertEquals(5, cmd.getEx());
            System.out.println(cmd);
        }

        {
            SDiffStoreParser parser = new SDiffStoreParser();
            SDiffStoreCommand cmd = parser.parse("sdiffstore des k1 k2".split(" "));
            assertEquals("des", cmd.getDestination());
            assertEquals("k1", cmd.getKeys()[0]);
            assertEquals("k2", cmd.getKeys()[1]);
            System.out.println(cmd);
        }

        {
            SInterStoreParser parser = new SInterStoreParser();
            SInterStoreCommand cmd = parser.parse("sinterstore des k1 k2".split(" "));
            assertEquals("des", cmd.getDestination());
            assertEquals("k1", cmd.getKeys()[0]);
            assertEquals("k2", cmd.getKeys()[1]);
            System.out.println(cmd);
        }

        {
            SUnionStoreParser parser = new SUnionStoreParser();
            SUnionStoreCommand cmd = parser.parse("sunionstore des k1 k2".split(" "));
            assertEquals("des", cmd.getDestination());
            assertEquals("k1", cmd.getKeys()[0]);
            assertEquals("k2", cmd.getKeys()[1]);
            System.out.println(cmd);
        }

        {
            ZInterStoreParser parser = new ZInterStoreParser();
            ZInterStoreCommand cmd = parser.parse("zinterstore des 2 k1 k2 WEIGHTS 2 3 AGGREGATE sum".split(" "));
            assertEquals("des", cmd.getDestination());
            assertEquals(2, cmd.getNumkeys());
            assertEquals("k1", cmd.getKeys()[0]);
            assertEquals("k2", cmd.getKeys()[1]);
            assertEquals(2, cmd.getWeights()[0], 0);
            assertEquals(3, cmd.getWeights()[1], 0);
            Assert.assertEquals(AggregateType.SUM, cmd.getAggregateType());
            System.out.println(cmd);
        }

        {
            ZUnionStoreParser parser = new ZUnionStoreParser();
            ZUnionStoreCommand cmd = parser.parse("zunionstore des 2 k1 k2 WEIGHTS 2 3 AGGREGATE min".split(" "));
            assertEquals("des", cmd.getDestination());
            assertEquals(2, cmd.getNumkeys());
            assertEquals("k1", cmd.getKeys()[0]);
            assertEquals("k2", cmd.getKeys()[1]);
            assertEquals(2, cmd.getWeights()[0], 0);
            assertEquals(3, cmd.getWeights()[1], 0);
            assertEquals(AggregateType.MIN, cmd.getAggregateType());
            System.out.println(cmd);
        }

        {
            EvalParser parser = new EvalParser();
            EvalCommand cmd = parser.parse(new Object[]{"eval", "return redis.call('set',KEYS[1],'bar')", "1", "foo"});
            assertEquals("return redis.call('set',KEYS[1],'bar')",cmd.getScript());
            assertEquals(1,cmd.getNumkeys());
            assertEquals("foo",cmd.getKeys()[0]);
            System.out.println(cmd);
        }

        {
            ScriptParser parser = new ScriptParser();
            ScriptLoadCommand cmd = (ScriptLoadCommand)parser.parse(new Object[]{"script","load", "return redis.call('set',KEYS[1],'bar')"});
            assertEquals("return redis.call('set',KEYS[1],'bar')",cmd.getScript());
            System.out.println(cmd);
        }

        {
            ScriptParser parser = new ScriptParser();
            ScriptFlushCommand cmd = (ScriptFlushCommand)parser.parse(new Object[]{"script","flush"});
            System.out.println(cmd);
        }

        {
            RestoreParser parser = new RestoreParser();
            RestoreCommand cmd = parser.parse(new Object[]{"restore", "mykey", "0", "\\n\\x17\\x17\\x00\\x00\\x00\\x12\\x00\\x00\\x00\\x03\\x00\\x00\\xc0\\x01\\x00\\x04\\xc0\\x02\\x00\\x04\\xc0\\x03\\x00\\xff\\x04\\x00u#<\\xc0;.\\xe9\\xdd"});
            assertEquals("\\n\\x17\\x17\\x00\\x00\\x00\\x12\\x00\\x00\\x00\\x03\\x00\\x00\\xc0\\x01\\x00\\x04\\xc0\\x02\\x00\\x04\\xc0\\x03\\x00\\xff\\x04\\x00u#<\\xc0;.\\xe9\\xdd",cmd.getSerializedValue());
            assertEquals("mykey",cmd.getKey());
            assertEquals(0,cmd.getTtl());
            assertEquals(null,cmd.getReplace());
            System.out.println(cmd);
        }

    }

}