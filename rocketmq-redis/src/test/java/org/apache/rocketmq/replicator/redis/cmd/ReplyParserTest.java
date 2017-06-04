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

package org.apache.rocketmq.replicator.redis.cmd;

import org.apache.rocketmq.replicator.redis.io.RedisInputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class ReplyParserTest {

    @Test
    public void testParse() throws Exception {
        {
            RedisInputStream in = new RedisInputStream(new ByteArrayInputStream(":56789\r\n".getBytes()));
            ReplyParser replyParser = new ReplyParser(in);
            Long r = replyParser.parse(new BulkReplyHandler.SimpleBulkReplyHandler()).getLen();
            assertEquals(56789l, r.longValue());
        }

    }
}