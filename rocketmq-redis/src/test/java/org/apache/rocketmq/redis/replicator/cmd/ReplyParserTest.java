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

package org.apache.rocketmq.redis.replicator.cmd;

import java.io.ByteArrayInputStream;
import org.apache.rocketmq.redis.replicator.io.RedisInputStream;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReplyParserTest {

    @Test
    public void testParse() throws Exception {
        {
            RedisInputStream in = new RedisInputStream(new ByteArrayInputStream(":56789\r\n".getBytes()));
            ReplyParser replyParser = new ReplyParser(in);
            Long r = (Long) replyParser.parse(new BulkReplyHandler.SimpleBulkReplyHandler());
            assertEquals(56789L, r.longValue());
        }

    }
}