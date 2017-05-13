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

import org.apache.rocketmq.replicator.redis.cmd.impl.GeoAddCommand;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class GeoAddParserTest {
    @Test
    public void parse() throws Exception {
        GeoAddParser parser = new GeoAddParser();
        GeoAddCommand cmd = parser.parse("GEOADD Sicily 13.361389 38.115556 Palermo 15.087269 37.502669 Catania".split(" "));
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