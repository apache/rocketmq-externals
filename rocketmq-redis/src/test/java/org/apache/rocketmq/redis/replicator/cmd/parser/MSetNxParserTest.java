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

import org.apache.rocketmq.redis.replicator.cmd.impl.PFAddCommand;
import org.apache.rocketmq.redis.replicator.cmd.impl.PFCountCommand;
import org.apache.rocketmq.redis.replicator.cmd.impl.PFMergeCommand;
import org.apache.rocketmq.redis.replicator.cmd.impl.PersistCommand;
import org.apache.rocketmq.redis.replicator.cmd.impl.MSetCommand;
import org.apache.rocketmq.redis.replicator.cmd.impl.MSetNxCommand;
import org.apache.rocketmq.redis.replicator.cmd.impl.PSetExCommand;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class MSetNxParserTest extends AbstractParserTest {
    @Test
    public void parse() throws Exception {
        {
            MSetNxParser parser = new MSetNxParser();
            MSetNxCommand cmd = parser.parse(toObjectArray("msetnx k1 v1 k2 v2".split(" ")));
            assertEquals("v1", cmd.getKv().get("k1"));
            assertEquals("v2", cmd.getKv().get("k2"));
            System.out.println(cmd);
        }

        {
            MSetParser parser = new MSetParser();
            MSetCommand cmd = parser.parse(toObjectArray("mset k1 v1 k2 v2".split(" ")));
            assertEquals("v1", cmd.getKv().get("k1"));
            assertEquals("v2", cmd.getKv().get("k2"));
            System.out.println(cmd);
        }

        {
            PersistParser parser = new PersistParser();
            PersistCommand cmd = parser.parse(toObjectArray("persist k1".split(" ")));
            assertEquals("k1", cmd.getKey());
            System.out.println(cmd);
        }

        {
            PFAddParser parser = new PFAddParser();
            PFAddCommand cmd = parser.parse(toObjectArray("pfadd k1 e1 e2".split(" ")));
            assertEquals("k1", cmd.getKey());
            assertEquals("e1", cmd.getElements()[0]);
            assertEquals("e2", cmd.getElements()[1]);
            System.out.println(cmd);
        }

        {
            PFCountParser parser = new PFCountParser();
            PFCountCommand cmd = parser.parse(toObjectArray("pfcount k1 k2".split(" ")));
            assertEquals("k1", cmd.getKeys()[0]);
            assertEquals("k2", cmd.getKeys()[1]);
            System.out.println(cmd);
        }

        {
            PFMergeParser parser = new PFMergeParser();
            PFMergeCommand cmd = parser.parse(toObjectArray("pfmerge des k1 k2".split(" ")));
            assertEquals("des", cmd.getDestkey());
            assertEquals("k1", cmd.getSourcekeys()[0]);
            assertEquals("k2", cmd.getSourcekeys()[1]);
            System.out.println(cmd);
        }

        {
            PSetExParser parser = new PSetExParser();
            PSetExCommand cmd = parser.parse(toObjectArray("psetex key 5 val".split(" ")));
            assertEquals("key", cmd.getKey());
            assertEquals(5, cmd.getEx());
            assertEquals("val", cmd.getValue());
            System.out.println(cmd);
        }
    }

}