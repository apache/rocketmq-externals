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

import org.apache.rocketmq.replicator.redis.cmd.impl.BitFieldCommand;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class BitFieldParserTest {

    @Test
    public void testParse() throws Exception {
        {
            BitFieldParser parser = new BitFieldParser();
            BitFieldCommand command = parser.parse(
                    new Object[]{"bitfield", "mykey", "overflow", "sat"});
            assertEquals("mykey", command.getKey());
            assertEquals(0, command.getStatements().size());
            assertEquals(1, command.getOverFlows().size());
            System.out.println(command);
        }


        //
        {
            BitFieldParser parser = new BitFieldParser();
            BitFieldCommand command = parser.parse(
                    new Object[]{"bitfield", "mykey", "incrby", "i5", "100", "1", "overflow", "sat"});
            assertEquals("mykey", command.getKey());
            assertEquals(1, command.getStatements().size());
            assertEquals(1, command.getOverFlows().size());
            System.out.println(command);
        }

        //
        {
            BitFieldParser parser = new BitFieldParser();
            BitFieldCommand command = parser.parse(
                    new Object[]{"bitfield", "mykey", "incrby", "i5", "100", "1", "set", "i8", "#0", "100", "overflow", "sat"});
            assertEquals("mykey", command.getKey());
            assertEquals(2, command.getStatements().size());
            assertEquals(1, command.getOverFlows().size());
            System.out.println(command);
        }

        //
        {
            BitFieldParser parser = new BitFieldParser();
            BitFieldCommand command = parser.parse(
                    new Object[]{"bitfield", "mykey", "incrby", "i5", "100", "1", "set", "i8", "#0", "100", "overflow", "fail"});
            assertEquals("mykey", command.getKey());
            assertEquals(2, command.getStatements().size());
            assertEquals(1, command.getOverFlows().size());
            System.out.println(command);
        }

        {
            BitFieldParser parser = new BitFieldParser();
            BitFieldCommand command = parser.parse(
                    new Object[]{"bitfield", "mykey", "incrby", "i5", "100", "1", "set", "i8", "#0", "100", "overflow", "wrap", "incrby", "i5", "100", "1", "set", "i8", "#0", "100", "overflow", "wrap", "incrby", "i5", "100", "1", "set", "i8", "#0", "100", "overflow", "fail"});
            assertEquals("mykey", command.getKey());
            assertEquals(2, command.getStatements().size());
            assertEquals(3, command.getOverFlows().size());
            assertEquals(2, command.getOverFlows().get(0).getStatements().size());
            System.out.println(command);
        }

        {
            BitFieldParser parser = new BitFieldParser();
            BitFieldCommand command = parser.parse(
                    new Object[]{"bitfield", "mykey", "incrby", "i5", "100", "1", "get", "i8", "10", "overflow", "wrap", "incrby", "i5", "100", "1", "set", "i8", "#0", "100", "overflow", "wrap", "incrby", "i5", "100", "1", "set", "i8", "#0", "100", "overflow", "fail"});
            assertEquals("mykey", command.getKey());
            assertEquals(2, command.getStatements().size());
            assertEquals(3, command.getOverFlows().size());
            assertEquals(2, command.getOverFlows().get(0).getStatements().size());
            System.out.println(command);
        }

    }
}