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

import org.apache.rocketmq.replicator.redis.Constants;
import org.apache.rocketmq.replicator.redis.io.RedisInputStream;

import java.io.IOException;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public interface BulkReplyHandler {
    String handle(long len, RedisInputStream in) throws IOException;

    class SimpleBulkReplyHandler implements BulkReplyHandler {
        @Override
        public String handle(long len, RedisInputStream in) throws IOException {
            String reply = len == 0 ? Constants.EMPTY : in.readString((int) len);
            int c;
            if ((c = in.read()) != '\r') throw new AssertionError("expect '\\r' but :" + (char) c);
            if ((c = in.read()) != '\n') throw new AssertionError("expect '\\n' but :" + (char) c);
            //simple reply
            return reply;
        }
    }
}
