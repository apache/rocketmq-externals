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

/*
 * Copyright 2009-2010 Ning, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * author Ning
 * modification: support more than 2GB laf decompression
 */

package org.apache.rocketmq.redis.replicator.util;

public class Lzf {

    public static ByteArray decode(ByteArray bytes, long len) {
        ByteArray out = new ByteArray(len);
        decode(bytes, 0, out, 0, len);
        return out;
    }

    private static void decode(ByteArray in, long inPos, ByteArray out, long outPos, long outEnd) {
        do {
            int ctrl = in.get(inPos++) & 255;
            if (ctrl < 1 << 5) {
                switch (ctrl) {
                    case 31:
                        out.set(outPos++, in.get(inPos++));
                    case 30:
                        out.set(outPos++, in.get(inPos++));
                    case 29:
                        out.set(outPos++, in.get(inPos++));
                    case 28:
                        out.set(outPos++, in.get(inPos++));
                    case 27:
                        out.set(outPos++, in.get(inPos++));
                    case 26:
                        out.set(outPos++, in.get(inPos++));
                    case 25:
                        out.set(outPos++, in.get(inPos++));
                    case 24:
                        out.set(outPos++, in.get(inPos++));
                    case 23:
                        out.set(outPos++, in.get(inPos++));
                    case 22:
                        out.set(outPos++, in.get(inPos++));
                    case 21:
                        out.set(outPos++, in.get(inPos++));
                    case 20:
                        out.set(outPos++, in.get(inPos++));
                    case 19:
                        out.set(outPos++, in.get(inPos++));
                    case 18:
                        out.set(outPos++, in.get(inPos++));
                    case 17:
                        out.set(outPos++, in.get(inPos++));
                    case 16:
                        out.set(outPos++, in.get(inPos++));
                    case 15:
                        out.set(outPos++, in.get(inPos++));
                    case 14:
                        out.set(outPos++, in.get(inPos++));
                    case 13:
                        out.set(outPos++, in.get(inPos++));
                    case 12:
                        out.set(outPos++, in.get(inPos++));
                    case 11:
                        out.set(outPos++, in.get(inPos++));
                    case 10:
                        out.set(outPos++, in.get(inPos++));
                    case 9:
                        out.set(outPos++, in.get(inPos++));
                    case 8:
                        out.set(outPos++, in.get(inPos++));
                    case 7:
                        out.set(outPos++, in.get(inPos++));
                    case 6:
                        out.set(outPos++, in.get(inPos++));
                    case 5:
                        out.set(outPos++, in.get(inPos++));
                    case 4:
                        out.set(outPos++, in.get(inPos++));
                    case 3:
                        out.set(outPos++, in.get(inPos++));
                    case 2:
                        out.set(outPos++, in.get(inPos++));
                    case 1:
                        out.set(outPos++, in.get(inPos++));
                    case 0:
                        out.set(outPos++, in.get(inPos++));
                }
                continue;
            }

            long len = ctrl >> 5;
            ctrl = -((ctrl & 0x1f) << 8) - 1;
            if (len < 7) {
                ctrl -= in.get(inPos++) & 255;
                out.set(outPos, out.get(outPos++ + ctrl));
                out.set(outPos, out.get(outPos++ + ctrl));
                switch ((int) len) {
                    case 6:
                        out.set(outPos, out.get(outPos++ + ctrl));
                    case 5:
                        out.set(outPos, out.get(outPos++ + ctrl));
                    case 4:
                        out.set(outPos, out.get(outPos++ + ctrl));
                    case 3:
                        out.set(outPos, out.get(outPos++ + ctrl));
                    case 2:
                        out.set(outPos, out.get(outPos++ + ctrl));
                    case 1:
                        out.set(outPos, out.get(outPos++ + ctrl));
                }
                continue;
            }

            len = in.get(inPos++) & 255;
            ctrl -= in.get(inPos++) & 255;

            if ((ctrl + len) < -9) {
                len += 9;
                if (len <= 32) {
                    long inPos1 = outPos + ctrl;
                    long outPos1 = outPos;
                    switch ((int) len - 1) {
                        case 31:
                            out.set(outPos1++, out.get(inPos1++));
                        case 30:
                            out.set(outPos1++, out.get(inPos1++));
                        case 29:
                            out.set(outPos1++, out.get(inPos1++));
                        case 28:
                            out.set(outPos1++, out.get(inPos1++));
                        case 27:
                            out.set(outPos1++, out.get(inPos1++));
                        case 26:
                            out.set(outPos1++, out.get(inPos1++));
                        case 25:
                            out.set(outPos1++, out.get(inPos1++));
                        case 24:
                            out.set(outPos1++, out.get(inPos1++));
                        case 23:
                            out.set(outPos1++, out.get(inPos1++));
                        case 22:
                            out.set(outPos1++, out.get(inPos1++));
                        case 21:
                            out.set(outPos1++, out.get(inPos1++));
                        case 20:
                            out.set(outPos1++, out.get(inPos1++));
                        case 19:
                            out.set(outPos1++, out.get(inPos1++));
                        case 18:
                            out.set(outPos1++, out.get(inPos1++));
                        case 17:
                            out.set(outPos1++, out.get(inPos1++));
                        case 16:
                            out.set(outPos1++, out.get(inPos1++));
                        case 15:
                            out.set(outPos1++, out.get(inPos1++));
                        case 14:
                            out.set(outPos1++, out.get(inPos1++));
                        case 13:
                            out.set(outPos1++, out.get(inPos1++));
                        case 12:
                            out.set(outPos1++, out.get(inPos1++));
                        case 11:
                            out.set(outPos1++, out.get(inPos1++));
                        case 10:
                            out.set(outPos1++, out.get(inPos1++));
                        case 9:
                            out.set(outPos1++, out.get(inPos1++));
                        case 8:
                            out.set(outPos1++, out.get(inPos1++));
                        case 7:
                            out.set(outPos1++, out.get(inPos1++));
                        case 6:
                            out.set(outPos1++, out.get(inPos1++));
                        case 5:
                            out.set(outPos1++, out.get(inPos1++));
                        case 4:
                            out.set(outPos1++, out.get(inPos1++));
                        case 3:
                            out.set(outPos1++, out.get(inPos1++));
                        case 2:
                            out.set(outPos1++, out.get(inPos1++));
                        case 1:
                            out.set(outPos1++, out.get(inPos1++));
                        case 0:
                            out.set(outPos1++, out.get(inPos1++));
                    }
                } else {
                    ByteArray.arraycopy(out, outPos + ctrl, out, outPos, len);
                }
                outPos += len;
                continue;
            }
            out.set(outPos, out.get(outPos++ + ctrl));
            out.set(outPos, out.get(outPos++ + ctrl));
            out.set(outPos, out.get(outPos++ + ctrl));
            out.set(outPos, out.get(outPos++ + ctrl));
            out.set(outPos, out.get(outPos++ + ctrl));
            out.set(outPos, out.get(outPos++ + ctrl));
            out.set(outPos, out.get(outPos++ + ctrl));
            out.set(outPos, out.get(outPos++ + ctrl));
            out.set(outPos, out.get(outPos++ + ctrl));

            len += outPos;
            final long end = len - 3;
            while (outPos < end) {
                out.set(outPos, out.get(outPos++ + ctrl));
                out.set(outPos, out.get(outPos++ + ctrl));
                out.set(outPos, out.get(outPos++ + ctrl));
                out.set(outPos, out.get(outPos++ + ctrl));
            }
            if (len - outPos == 3) {
                out.set(outPos, out.get(outPos++ + ctrl));
                out.set(outPos, out.get(outPos++ + ctrl));
                out.set(outPos, out.get(outPos++ + ctrl));
            } else if (len - outPos == 2) {
                out.set(outPos, out.get(outPos++ + ctrl));
                out.set(outPos, out.get(outPos++ + ctrl));
            } else if (len - outPos == 1) {
                out.set(outPos, out.get(outPos++ + ctrl));
            }
        }
        while (outPos < outEnd);

        if (outPos != outEnd) {
            throw new AssertionError("corrupt data: overrun in decompress, input offset " + inPos + ", output offset " + outPos);
        }
    }
}
