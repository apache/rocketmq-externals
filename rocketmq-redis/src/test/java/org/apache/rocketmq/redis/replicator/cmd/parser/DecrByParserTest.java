/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.redis.replicator.cmd.parser;

import org.apache.rocketmq.redis.replicator.cmd.impl.DecrByCommand;
import org.apache.rocketmq.redis.replicator.cmd.impl.DecrCommand;
import org.apache.rocketmq.redis.replicator.cmd.impl.HIncrByCommand;
import org.apache.rocketmq.redis.replicator.cmd.impl.IncrByCommand;
import org.apache.rocketmq.redis.replicator.cmd.impl.IncrCommand;
import org.apache.rocketmq.redis.replicator.cmd.impl.ZIncrByCommand;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class DecrByParserTest extends AbstractParserTest {
    @Test
    public void parse() throws Exception {
        {
            DecrByParser parser = new DecrByParser();
            DecrByCommand cmd = parser.parse(toObjectArray("decrby key 5".split(" ")));
            assertEquals("key", cmd.getKey());
            assertEquals(5, cmd.getValue());
            System.out.println(cmd);
        }

        {
            IncrByParser parser = new IncrByParser();
            IncrByCommand cmd = parser.parse(toObjectArray("incrby key 5".split(" ")));
            assertEquals("key", cmd.getKey());
            assertEquals(5, cmd.getValue());
            System.out.println(cmd);
        }

        {
            DecrParser parser = new DecrParser();
            DecrCommand cmd = parser.parse(toObjectArray("decr key".split(" ")));
            assertEquals("key", cmd.getKey());
            System.out.println(cmd);
        }

        {
            IncrParser parser = new IncrParser();
            IncrCommand cmd = parser.parse(toObjectArray("incr key".split(" ")));
            assertEquals("key", cmd.getKey());
            System.out.println(cmd);
        }

        {
            ZIncrByParser parser = new ZIncrByParser();
            ZIncrByCommand cmd = parser.parse(toObjectArray("zincrby key 5 mem".split(" ")));
            assertEquals("key", cmd.getKey());
            assertEquals(5, cmd.getIncrement(), 0);
            assertEquals("mem", cmd.getMember());
            System.out.println(cmd);
        }

        {
            HIncrByParser parser = new HIncrByParser();
            HIncrByCommand cmd = parser.parse(toObjectArray("hincrby key mem 5".split(" ")));
            assertEquals("key", cmd.getKey());
            assertEquals(5, cmd.getIncrement());
            assertEquals("mem", cmd.getField());
            System.out.println(cmd);
        }
    }

}