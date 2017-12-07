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

import java.io.InputStream;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LzfTest {
    @Test
    public void decode() throws Exception {
        {
            String str = "abcdsklafjslfjfd;sfdklafjlsafjslfjasl;fkjdsalfjasfjlas;dkfjalsvlasfkal;sj";
            byte[] out = compress(str.getBytes());
            ByteArray in = Lzf.decode(new ByteArray(out), str.getBytes().length);
            assertEquals(new String(in.first()), str);
        }

        {
            InputStream in = LzfTest.class.getClassLoader().getResourceAsStream("low-comp-120k.txt");
            byte[] bytes = new byte[121444];
            int len = in.read(bytes);
            byte[] out = compress(bytes);
            ByteArray bin = Lzf.decode(new ByteArray(out), len);
            byte[] oin = bin.first();
            for (int i = 0; i < len; i++) {
                assertEquals(oin[i], bytes[i]);
            }
        }

        {
            InputStream in = LzfTest.class.getClassLoader().getResourceAsStream("appendonly6.aof");
            byte[] bytes = new byte[3949];
            int len = in.read(bytes);
            byte[] out = compress(bytes);
            ByteArray bin = Lzf.decode(new ByteArray(out), len);
            byte[] oin = bin.first();
            for (int i = 0; i < len; i++) {
                assertEquals(oin[i], bytes[i]);
            }
        }

    }

    private byte[] compress(byte[] in) {
        CompressLZF c = new CompressLZF();
        byte[] compressed = new byte[in.length];
        int idx = c.compress(in, in.length, compressed, 0);
        byte[] out = new byte[idx];
        System.arraycopy(compressed, 0, out, 0, out.length);
        return out;
    }

}