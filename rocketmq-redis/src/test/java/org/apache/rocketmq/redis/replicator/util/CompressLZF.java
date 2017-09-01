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

package org.apache.rocketmq.redis.replicator.util;

public final class CompressLZF {

    /**
     * The number of entries in the hash table. The size is a trade-off between
     * hash collisions (reduced compression) and speed (amount that fits in CPU
     * cache).
     */
    private static final int HASH_SIZE = 1 << 14;

    /**
     * The maximum number of literals in a chunk (32).
     */
    private static final int MAX_LITERAL = 1 << 5;

    /**
     * The maximum offset allowed for a back-reference (8192).
     */
    private static final int MAX_OFF = 1 << 13;

    /**
     * The maximum back-reference length (264).
     */
    private static final int MAX_REF = (1 << 8) + (1 << 3);

    /**
     * Hash table for matching byte sequences (reused for performance).
     */
    private int[] cachedHashTable;

    /**
     * Return byte with lower 2 bytes being byte at index, then index+1.
     */
    private static int first(byte[] in, int inPos) {
        return (in[inPos] << 8) | (in[inPos + 1] & 255);
    }

    /**
     * Shift v 1 byte left, add value at index inPos+2.
     */
    private static int next(int v, byte[] in, int inPos) {
        return (v << 8) | (in[inPos + 2] & 255);
    }

    /**
     * Compute the address in the hash table.
     */
    private static int hash(int h) {
        return ((h * 2777) >> 9) & (HASH_SIZE - 1);
    }

    public int compress(byte[] in, int inLen, byte[] out, int outPos) {
        int inPos = 0;
        if (cachedHashTable == null) {
            cachedHashTable = new int[HASH_SIZE];
        }
        int[] hashTab = cachedHashTable;
        int literals = 0;
        outPos++;
        int future = first(in, 0);
        while (inPos < inLen - 4) {
            byte p2 = in[inPos + 2];
            // next
            future = (future << 8) + (p2 & 255);
            int off = hash(future);
            int ref = hashTab[off];
            hashTab[off] = inPos;
            // if (ref < inPos
            //       && ref > 0
            //       && (off = inPos - ref - 1) < MAX_OFF
            //       && in[ref + 2] == p2
            //       && (((in[ref] & 255) << 8) | (in[ref + 1] & 255)) ==
            //           ((future >> 8) & 0xffff)) {
            if (ref < inPos
                && ref > 0
                && (off = inPos - ref - 1) < MAX_OFF
                && in[ref + 2] == p2
                && in[ref + 1] == (byte) (future >> 8)
                && in[ref] == (byte) (future >> 16)) {
                // match
                int maxLen = inLen - inPos - 2;
                if (maxLen > MAX_REF) {
                    maxLen = MAX_REF;
                }
                if (literals == 0) {
                    // multiple back-references,
                    // so there is no literal run control byte
                    outPos--;
                } else {
                    // set the control byte at the start of the literal run
                    // to store the number of literals
                    out[outPos - literals - 1] = (byte) (literals - 1);
                    literals = 0;
                }
                int len = 3;
                while (len < maxLen && in[ref + len] == in[inPos + len]) {
                    len++;
                }
                len -= 2;
                if (len < 7) {
                    out[outPos++] = (byte) ((off >> 8) + (len << 5));
                } else {
                    out[outPos++] = (byte) ((off >> 8) + (7 << 5));
                    out[outPos++] = (byte) (len - 7);
                }
                out[outPos++] = (byte) off;
                // move one byte forward to allow for a literal run control byte
                outPos++;
                inPos += len;
                // rebuild the future, and store the last bytes to the hashtable.
                // Storing hashes of the last bytes in back-reference improves
                // the compression ratio and only reduces speed slightly.
                future = first(in, inPos);
                future = next(future, in, inPos);
                hashTab[hash(future)] = inPos++;
                future = next(future, in, inPos);
                hashTab[hash(future)] = inPos++;
            } else {
                // copy one byte from input to output as part of literal
                out[outPos++] = in[inPos++];
                literals++;
                // at the end of this literal chunk, write the length
                // to the control byte and start a new chunk
                if (literals == MAX_LITERAL) {
                    out[outPos - literals - 1] = (byte) (literals - 1);
                    literals = 0;
                    // move ahead one byte to allow for the
                    // literal run control byte
                    outPos++;
                }
            }
        }
        // write the remaining few bytes as literals
        while (inPos < inLen) {
            out[outPos++] = in[inPos++];
            literals++;
            if (literals == MAX_LITERAL) {
                out[outPos - literals - 1] = (byte) (literals - 1);
                literals = 0;
                outPos++;
            }
        }
        // writes the final literal run length to the control byte
        out[outPos - literals - 1] = (byte) (literals - 1);
        if (literals == 0) {
            outPos--;
        }
        return outPos;
    }
}
