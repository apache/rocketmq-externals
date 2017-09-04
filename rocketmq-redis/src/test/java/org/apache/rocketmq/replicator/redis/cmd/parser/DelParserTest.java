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

import org.apache.rocketmq.replicator.redis.cmd.impl.DelCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.HDelCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.LRemCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.SRemCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.UnLinkCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.ZRemCommand;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class DelParserTest {
    @Test
    public void parse() throws Exception {
        {
            DelParser parser = new DelParser();
            DelCommand cmd = parser.parse("del key1 key2".split(" "));
            assertEquals("key1",cmd.getKeys()[0]);
            assertEquals("key2",cmd.getKeys()[1]);
            System.out.println(cmd);
            UnLinkParser parser1 = new UnLinkParser();
            UnLinkCommand cmd1 = parser1.parse("unlink key1 key2".split(" "));
            assertEquals("key1",cmd1.getKeys()[0]);
            assertEquals("key2",cmd1.getKeys()[1]);
            System.out.println(cmd1);
        }

        {
            HDelParser parser = new HDelParser();
            HDelCommand cmd = parser.parse("hdel key f1 f2".split(" "));
            assertEquals("key",cmd.getKey());
            assertEquals("f1",cmd.getFields()[0]);
            assertEquals("f2",cmd.getFields()[1]);
            System.out.println(cmd);
        }

        {
            LRemParser parser = new LRemParser();
            LRemCommand cmd = parser.parse("lrem key 1 val".split(" "));
            assertEquals("key",cmd.getKey());
            assertEquals("val",cmd.getValue());
            assertEquals(1,cmd.getIndex());
            System.out.println(cmd);
        }

        {
            SRemParser parser = new SRemParser();
            SRemCommand cmd = parser.parse("srem key m1 m2".split(" "));
            assertEquals("key",cmd.getKey());
            assertEquals("m1",cmd.getMembers()[0]);
            assertEquals("m2",cmd.getMembers()[1]);
            System.out.println(cmd);
        }

        {
            ZRemParser parser = new ZRemParser();
            ZRemCommand cmd = parser.parse("zrem key m1 m2".split(" "));
            assertEquals("key",cmd.getKey());
            assertEquals("m1",cmd.getMembers()[0]);
            assertEquals("m2",cmd.getMembers()[1]);
            System.out.println(cmd);
        }
    }

}