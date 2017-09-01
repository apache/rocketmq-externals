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

import org.apache.rocketmq.redis.replicator.cmd.impl.ZRemRangeByLexCommand;
import org.apache.rocketmq.redis.replicator.cmd.impl.ZRemRangeByRankCommand;
import org.apache.rocketmq.redis.replicator.cmd.impl.ExistType;
import org.apache.rocketmq.redis.replicator.cmd.impl.ZAddCommand;
import org.apache.rocketmq.redis.replicator.cmd.impl.ZRemRangeByScoreCommand;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class ZAddParserTest extends AbstractParserTest {
    @Test
    public void parse() throws Exception {
        ZAddParser parser = new ZAddParser();
        ZAddCommand cmd = parser.parse(toObjectArray("zadd abc nx ch incr 1 b".split(" ")));
        assertEquals("abc", cmd.getKey());
        assertEquals(ExistType.NX, cmd.getExistType());
        assertEquals(Boolean.TRUE, cmd.getCh());
        assertEquals(Boolean.TRUE, cmd.getIncr());
        assertEquals(1, cmd.getZSetEntries()[0].getScore(), 0);
        assertEquals("b", cmd.getZSetEntries()[0].getElement());
        System.out.println(cmd);

        cmd = parser.parse(toObjectArray("zadd abc 1 b".split(" ")));
        assertEquals("abc", cmd.getKey());
        assertEquals(ExistType.NONE, cmd.getExistType());
        assertEquals(null, cmd.getCh());
        assertEquals(null, cmd.getIncr());
        assertEquals(1, cmd.getZSetEntries()[0].getScore(), 0);
        assertEquals("b", cmd.getZSetEntries()[0].getElement());
        System.out.println(cmd);

        cmd = parser.parse(toObjectArray("zadd abc xx 1 b".split(" ")));
        assertEquals("abc", cmd.getKey());
        assertEquals(ExistType.XX, cmd.getExistType());
        assertEquals(null, cmd.getCh());
        assertEquals(null, cmd.getIncr());
        assertEquals(1, cmd.getZSetEntries()[0].getScore(), 0);
        assertEquals("b", cmd.getZSetEntries()[0].getElement());
        System.out.println(cmd);

        cmd = parser.parse(toObjectArray("zadd abc incr 1 b".split(" ")));
        assertEquals("abc", cmd.getKey());
        assertEquals(ExistType.NONE, cmd.getExistType());
        assertEquals(null, cmd.getCh());
        assertEquals(Boolean.TRUE, cmd.getIncr());
        assertEquals(1, cmd.getZSetEntries()[0].getScore(), 0);
        assertEquals("b", cmd.getZSetEntries()[0].getElement());
        System.out.println(cmd);

        {
            ZRemRangeByLexParser parser1 = new ZRemRangeByLexParser();
            ZRemRangeByLexCommand cmd1 = parser1.parse(toObjectArray("ZREMRANGEBYLEX myzset [alpha [omega".split(" ")));
            assertEquals("myzset", cmd1.getKey());
            assertEquals("[alpha", cmd1.getMin());
            assertEquals("[omega", cmd1.getMax());
            System.out.println(cmd1);
        }

        {
            ZRemRangeByScoreParser parser1 = new ZRemRangeByScoreParser();
            ZRemRangeByScoreCommand cmd1 = parser1.parse(toObjectArray("ZREMRANGEBYSCORE myzset -inf (2".split(" ")));
            assertEquals("myzset", cmd1.getKey());
            assertEquals("-inf", cmd1.getMin());
            assertEquals("(2", cmd1.getMax());
            System.out.println(cmd1);
        }

        {
            ZRemRangeByRankParser parser1 = new ZRemRangeByRankParser();
            ZRemRangeByRankCommand cmd1 = parser1.parse(toObjectArray("ZREMRANGEBYRANK myzset 0 1".split(" ")));
            assertEquals("myzset", cmd1.getKey());
            assertEquals(0L, cmd1.getStart());
            assertEquals(1L, cmd1.getStop());
            System.out.println(cmd1);
        }

    }

}