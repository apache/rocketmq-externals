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
import org.apache.rocketmq.replicator.redis.util.ByteBuilder;

import java.io.IOException;
import org.apache.rocketmq.replicator.redis.Constants;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class ReplyParser {
    private final RedisInputStream in;

    public ReplyParser(RedisInputStream in) {
        this.in = in;
    }

    public Object parse() throws IOException {
        return parse(new BulkReplyHandler.SimpleBulkReplyHandler(), null);
    }

    public Object parse(OffsetHandler offsetHandler) throws IOException {
        return parse(new BulkReplyHandler.SimpleBulkReplyHandler(), offsetHandler);
    }

    public Object parse(BulkReplyHandler handler, OffsetHandler offsetHandler) throws IOException {
        in.mark();
        Object rs = parse(handler);
        long len = in.unmark();
        if (offsetHandler != null) offsetHandler.handle(len);
        return rs;
    }

    /**
     * @param handler bulk string handler
     * @return return Object[] or String or Long
     * @throws IOException when read timeout
     */
    public Object parse(BulkReplyHandler handler) throws IOException {
        int c = in.read();
        switch (c) {
            case Constants.DOLLAR:
                //RESP Bulk Strings
                ByteBuilder builder = ByteBuilder.allocate(128);
                while (true) {
                    while ((c = in.read()) != '\r') {
                        builder.put((byte) c);
                    }
                    if ((c = in.read()) == '\n') {
                        break;
                    } else {
                        builder.put((byte) c);
                    }
                }
                long len = Long.parseLong(builder.toString());
                // $-1\r\n. this is called null string.
                // see http://redis.io/topics/protocol
                if (len == -1) return null;
                if (handler != null) return handler.handle(len, in);
                throw new AssertionError("callback is null");
            case Constants.COLON:
                // RESP Integers
                builder = ByteBuilder.allocate(128);
                while (true) {
                    while ((c = in.read()) != '\r') {
                        builder.put((byte) c);
                    }
                    if ((c = in.read()) == '\n') {
                        break;
                    } else {
                        builder.put((byte) c);
                    }
                }
                //as integer
                return Long.parseLong(builder.toString());
            case Constants.STAR:
                // RESP Arrays
                builder = ByteBuilder.allocate(128);
                while (true) {
                    while ((c = in.read()) != '\r') {
                        builder.put((byte) c);
                    }
                    if ((c = in.read()) == '\n') {
                        break;
                    } else {
                        builder.put((byte) c);
                    }
                }
                len = Long.parseLong(builder.toString());
                if (len == -1) return null;
                Object[] ary = new Object[(int) len];
                for (int i = 0; i < len; i++) {
                    Object obj = parse(new BulkReplyHandler.SimpleBulkReplyHandler());
                    ary[i] = obj;
                }
                return ary;
            case Constants.PLUS:
                // RESP Simple Strings
                builder = ByteBuilder.allocate(128);
                while (true) {
                    while ((c = in.read()) != '\r') {
                        builder.put((byte) c);
                    }
                    if ((c = in.read()) == '\n') {
                        return builder.toString();
                    } else {
                        builder.put((byte) c);
                    }
                }
            case Constants.MINUS:
                // RESP Errors
                builder = ByteBuilder.allocate(128);
                while (true) {
                    while ((c = in.read()) != '\r') {
                        builder.put((byte) c);
                    }
                    if ((c = in.read()) == '\n') {
                        return builder.toString();
                    } else {
                        builder.put((byte) c);
                    }
                }
            case '\n':
                //skip +CONTINUE\r\n[\n]
                //skip +FULLRESYNC 8de1787ba490483314a4d30f1c628bc5025eb761 2443808505[\n]$2443808505\r\nxxxxxxxxxxxxxxxx\r\n
                //At this stage just a newline works as a PING in order to take the connection live
                //bug fix
                return parse(handler);
            default:
                throw new AssertionError("expect [$,:,*,+,-] but: " + (char) c);

        }
    }
}
