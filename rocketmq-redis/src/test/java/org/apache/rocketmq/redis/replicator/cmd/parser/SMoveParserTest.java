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

import org.apache.rocketmq.redis.replicator.cmd.impl.SMoveCommand;
import org.apache.rocketmq.redis.replicator.cmd.impl.SwapDBCommand;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class SMoveParserTest extends AbstractParserTest {
    @Test
    public void parse() throws Exception {
        SMoveParser parser = new SMoveParser();
        SMoveCommand cmd = parser.parse(toObjectArray("smove src des field".split(" ")));
        assertEquals("src", cmd.getSource());
        assertEquals("des", cmd.getDestination());
        assertEquals("field", cmd.getMember());
        System.out.println(cmd);

        {
            SwapDBParser parser1 = new SwapDBParser();
            SwapDBCommand cmd1 = parser1.parse(toObjectArray("swapdb 0 1".split(" ")));
            assertEquals(0, cmd1.getSource());
            assertEquals(1, cmd1.getTarget());
            System.out.println(cmd1);
        }
    }

}