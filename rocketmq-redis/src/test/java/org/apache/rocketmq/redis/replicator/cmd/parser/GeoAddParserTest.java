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

import org.apache.rocketmq.redis.replicator.cmd.impl.GeoAddCommand;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class GeoAddParserTest extends AbstractParserTest {
    @Test
    public void parse() throws Exception {
        GeoAddParser parser = new GeoAddParser();
        GeoAddCommand cmd = parser.parse(toObjectArray("GEOADD Sicily 13.361389 38.115556 Palermo 15.087269 37.502669 Catania".split(" ")));
        assertEquals("Sicily", cmd.getKey());
        assertEquals(13.361389, cmd.getGeos()[0].getLongitude(), 0.000001);
        assertEquals(38.115556, cmd.getGeos()[0].getLatitude(), 0.000001);
        assertEquals("Palermo", cmd.getGeos()[0].getMember());

        assertEquals(15.087269, cmd.getGeos()[1].getLongitude(), 0.000001);
        assertEquals(37.502669, cmd.getGeos()[1].getLatitude(), 0.000001);
        assertEquals("Catania", cmd.getGeos()[1].getMember());
        System.out.println(cmd);
    }

}