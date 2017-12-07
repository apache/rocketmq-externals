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

import org.apache.rocketmq.redis.replicator.cmd.impl.FlushDBCommand;
import org.apache.rocketmq.redis.replicator.cmd.impl.FlushAllCommand;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class FlushAllParserTest extends AbstractParserTest {
    @Test
    public void parse() throws Exception {
        {
            FlushAllParser parser = new FlushAllParser();
            FlushAllCommand cmd = parser.parse(toObjectArray("flushall".split(" ")));
            assertEquals(null, cmd.isAsync());

            parser = new FlushAllParser();
            cmd = parser.parse(toObjectArray("flushall async".split(" ")));
            assertEquals(Boolean.TRUE, cmd.isAsync());
            System.out.println(cmd);
        }

        {
            FlushDBParser parser = new FlushDBParser();
            FlushDBCommand cmd = parser.parse(toObjectArray("flushdb".split(" ")));
            assertEquals(null, cmd.isAsync());

            parser = new FlushDBParser();
            cmd = parser.parse(toObjectArray("flushdb async".split(" ")));
            assertEquals(Boolean.TRUE, cmd.isAsync());
            System.out.println(cmd);
        }
    }

}