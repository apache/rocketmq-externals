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

import org.apache.rocketmq.replicator.redis.cmd.impl.HMSetCommand;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class HMSetParserTest {

    @Test
    public void testParse() throws Exception {
        {
            HMSetParser hmSetParser = new HMSetParser();
            HMSetCommand command = hmSetParser.parse(new Object[]{"hmset", "key", "field", "value"});
            assertEquals("key", command.getKey());
            assertEquals(1, command.getFields().size());
        }

        {
            HMSetParser hmSetParser = new HMSetParser();
            HMSetCommand command = hmSetParser.parse(new Object[]{"hmset", "key", "field", "value", "field1", "value1"});
            assertEquals("key", command.getKey());
            assertEquals(2, command.getFields().size());
        }
    }
}