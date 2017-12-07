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

import org.apache.rocketmq.redis.replicator.cmd.impl.SortCommand;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.apache.rocketmq.redis.replicator.cmd.impl.OrderType.DESC;
import static org.apache.rocketmq.redis.replicator.cmd.impl.OrderType.NONE;

public class SortParserTest extends AbstractParserTest {
    @Test
    public void parse() throws Exception {

        {
            SortParser parser = new SortParser();
            SortCommand cmd = parser.parse(toObjectArray("SORT mylist".split(" ")));
            assertEquals("mylist", cmd.getKey());
            assertEquals(NONE, cmd.getOrder());
            assertEquals(0, cmd.getGetPatterns().length);
            assertEquals(null, cmd.getAlpha());
            System.out.println(cmd);
        }

        {
            SortParser parser = new SortParser();
            SortCommand cmd = parser.parse(toObjectArray("SORT mylist DESC".split(" ")));
            assertEquals("mylist", cmd.getKey());
            assertEquals(DESC, cmd.getOrder());
            assertEquals(0, cmd.getGetPatterns().length);
            assertEquals(null, cmd.getAlpha());
            System.out.println(cmd);
        }

        {
            SortParser parser = new SortParser();
            SortCommand cmd = parser.parse(toObjectArray("SORT mylist ALPHA DESC".split(" ")));
            assertEquals("mylist", cmd.getKey());
            assertEquals(DESC, cmd.getOrder());
            assertEquals(0, cmd.getGetPatterns().length);
            assertEquals(true, cmd.getAlpha().booleanValue());
            System.out.println(cmd);
        }

        {
            SortParser parser = new SortParser();
            SortCommand cmd = parser.parse(toObjectArray("SORT mylist ALPHA DESC limit 0 10".split(" ")));
            assertEquals("mylist", cmd.getKey());
            assertEquals(DESC, cmd.getOrder());
            assertEquals(0, cmd.getGetPatterns().length);
            assertEquals(true, cmd.getAlpha().booleanValue());
            assertEquals(0L, cmd.getLimit().getOffset());
            assertEquals(10L, cmd.getLimit().getCount());
            System.out.println(cmd);
        }

        {
            SortParser parser = new SortParser();
            SortCommand cmd = parser.parse(toObjectArray("sort mylist alpha desc limit 0 10 by weight_*".split(" ")));
            assertEquals("mylist", cmd.getKey());
            assertEquals(DESC, cmd.getOrder());
            assertEquals(0, cmd.getGetPatterns().length);
            assertEquals(true, cmd.getAlpha().booleanValue());
            assertEquals(0L, cmd.getLimit().getOffset());
            assertEquals(10L, cmd.getLimit().getCount());
            assertEquals("weight_*", cmd.getByPattern());
            System.out.println(cmd);
        }

        {
            SortParser parser = new SortParser();
            SortCommand cmd = parser.parse(toObjectArray("sort mylist alpha desc limit 0 10 by weight_* get object_* get #".split(" ")));
            assertEquals("mylist", cmd.getKey());
            assertEquals(DESC, cmd.getOrder());
            assertEquals(true, cmd.getAlpha().booleanValue());
            assertEquals(0L, cmd.getLimit().getOffset());
            assertEquals(10L, cmd.getLimit().getCount());
            assertEquals("weight_*", cmd.getByPattern());
            assertEquals(2, cmd.getGetPatterns().length);
            assertEquals("object_*", cmd.getGetPatterns()[0]);
            assertEquals("#", cmd.getGetPatterns()[1]);
            System.out.println(cmd);
        }

    }

}