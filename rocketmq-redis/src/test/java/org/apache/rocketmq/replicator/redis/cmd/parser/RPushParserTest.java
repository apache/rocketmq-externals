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

import org.apache.rocketmq.replicator.redis.cmd.impl.LPopCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.LPushCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.LPushXCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.RPopCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.RPushCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.RPushXCommand;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class RPushParserTest {
    @Test
    public void parse() throws Exception {
        {
            RPushParser parser = new RPushParser();
            RPushCommand cmd = parser.parse("rpush key v1 v2".split(" "));
            assertEquals("key",cmd.getKey());
            assertEquals("v1",cmd.getValues()[0]);
            assertEquals("v2",cmd.getValues()[1]);
            System.out.println(cmd);
        }

        {
            RPushXParser parser = new RPushXParser();
            RPushXCommand cmd = parser.parse("rpushx key v1 v2".split(" "));
            assertEquals("key",cmd.getKey());
            assertEquals("v1", cmd.getValues()[0]);
            assertEquals("v2", cmd.getValues()[1]);
            System.out.println(cmd);
        }

        {
            LPushParser parser = new LPushParser();
            LPushCommand cmd = parser.parse("lpush key v1 v2".split(" "));
            assertEquals("key",cmd.getKey());
            assertEquals("v1",cmd.getValues()[0]);
            assertEquals("v2",cmd.getValues()[1]);
            System.out.println(cmd);
        }

        {
            LPushXParser parser = new LPushXParser();
            LPushXCommand cmd = parser.parse("lpushx key v1 v2".split(" "));
            assertEquals("key",cmd.getKey());
            assertEquals("v1", cmd.getValues()[0]);
            assertEquals("v2", cmd.getValues()[1]);
            System.out.println(cmd);
        }

        {
            LPopParser parser = new LPopParser();
            LPopCommand cmd = parser.parse("lpop key".split(" "));
            assertEquals("key",cmd.getKey());
            System.out.println(cmd);
        }

        {
            RPopParser parser = new RPopParser();
            RPopCommand cmd = parser.parse("rpop key".split(" "));
            assertEquals("key",cmd.getKey());
            System.out.println(cmd);
        }
    }

}