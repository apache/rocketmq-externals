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

import java.io.IOException;
import org.apache.rocketmq.redis.replicator.io.RedisInputStream;

public interface BulkReplyHandler {
    byte[] handle(long len, RedisInputStream in) throws IOException;

    class SimpleBulkReplyHandler implements BulkReplyHandler {
        @Override
        public byte[] handle(long len, RedisInputStream in) throws IOException {
            byte[] reply = len == 0 ? new byte[] {} : in.readBytes(len).first();
            int c;
            if ((c = in.read()) != '\r')
                throw new AssertionError("expect '\\r' but :" + (char) c);
            if ((c = in.read()) != '\n')
                throw new AssertionError("expect '\\n' but :" + (char) c);
            return reply;
        }
    }
}
