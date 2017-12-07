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

import org.apache.rocketmq.redis.replicator.cmd.impl.LTrimCommand;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class LTrimParserTest extends AbstractParserTest {
    @Test
    public void parse() throws Exception {
        LTrimParser parser = new LTrimParser();
        LTrimCommand cmd = parser.parse(toObjectArray("LTRIM mylist 0 99".split(" ")));
        assertEquals("mylist", cmd.getKey());
        assertEquals(0L, cmd.getStart());
        assertEquals(99L, cmd.getStop());
        System.out.println(cmd);
    }

}