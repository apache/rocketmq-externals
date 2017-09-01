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

import org.apache.rocketmq.redis.replicator.cmd.impl.ExistType;
import org.apache.rocketmq.redis.replicator.cmd.impl.SetCommand;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class SetParserTest extends AbstractParserTest {
    @Test
    public void parse() throws Exception {
        SetParser parser = new SetParser();
        SetCommand cmd = parser.parse(toObjectArray("set a b ex 15 nx".split(" ")));
        assertEquals("a", cmd.getKey());
        assertEquals("b", cmd.getValue());
        assertEquals(15, cmd.getEx().intValue());
        assertEquals(ExistType.NX, cmd.getExistType());
        System.out.println(cmd);

        cmd = parser.parse(toObjectArray("set a b px 123 xx".split(" ")));
        assertEquals("a", cmd.getKey());
        assertEquals("b", cmd.getValue());
        assertEquals(123L, cmd.getPx().longValue());
        assertEquals(ExistType.XX, cmd.getExistType());
        System.out.println(cmd);

        cmd = parser.parse(toObjectArray("set a b xx px 123".split(" ")));
        assertEquals("a", cmd.getKey());
        assertEquals("b", cmd.getValue());
        assertEquals(123L, cmd.getPx().longValue());
        assertEquals(ExistType.XX, cmd.getExistType());
        System.out.println(cmd);

    }
}