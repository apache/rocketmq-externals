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

package org.apache.rocketmq.mysql;

import org.apache.rocketmq.mysql.schema.column.IntColumnParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IntColumnParserTest {

    @Test
    public void testInt() {
        IntColumnParser parser = new IntColumnParser("int", "int(10) unsigned");

        Long v1 = (Long) parser.getValue(Integer.MIN_VALUE);
        Long v2 = (long) Integer.MAX_VALUE + 1;
        assertEquals(v1, v2);
    }

    @Test
    public void testSmallint() {
        IntColumnParser parser = new IntColumnParser("smallint", "smallint(5) unsigned");

        Long v1 = (Long) parser.getValue((int) Short.MIN_VALUE);
        Long v2 = (long) (Short.MAX_VALUE + 1);
        assertEquals(v1, v2);
    }

    @Test
    public void testTinyint() {
        IntColumnParser parser = new IntColumnParser("tinyint", "tinyint(3) unsigned");

        Long v1 = (Long) parser.getValue((int) Byte.MIN_VALUE);
        Long v2 = (long) (Byte.MAX_VALUE + 1);
        assertEquals(v1, v2);
    }

}
