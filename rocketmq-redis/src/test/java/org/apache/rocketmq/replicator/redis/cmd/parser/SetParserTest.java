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

import org.apache.rocketmq.replicator.redis.cmd.impl.ExistType;
import org.apache.rocketmq.replicator.redis.cmd.impl.SetCommand;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class SetParserTest {
    @Test
    public void parse() throws Exception {
        SetParser parser = new SetParser();
        SetCommand cmd = parser.parse("set a b ex 15 nx".split(" "));
        assertEquals("a", cmd.getKey());
        assertEquals("b", cmd.getValue());
        assertEquals(15, cmd.getEx().intValue());
        Assert.assertEquals(ExistType.NX, cmd.getExistType());
        System.out.println(cmd);

        cmd = parser.parse("set a b px 123 xx".split(" "));
        assertEquals("a", cmd.getKey());
        assertEquals("b", cmd.getValue());
        assertEquals(123L, cmd.getPx().longValue());
        assertEquals(ExistType.XX, cmd.getExistType());
        System.out.println(cmd);

        cmd = parser.parse("set a b xx px 123".split(" "));
        assertEquals("a", cmd.getKey());
        assertEquals("b", cmd.getValue());
        assertEquals(123L, cmd.getPx().longValue());
        assertEquals(ExistType.XX, cmd.getExistType());
        System.out.println(cmd);

    }

}