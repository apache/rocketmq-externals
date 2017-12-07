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

package org.apache.rocketmq.redis.replicator.cmd.impl;

import java.util.Arrays;
import org.apache.rocketmq.redis.replicator.cmd.Command;

public class BitOpCommand implements Command {

    private static final long serialVersionUID = 1L;

    private Op op;
    private String destkey;
    private String[] keys;
    private byte[] rawDestkey;
    private byte[][] rawKeys;

    public BitOpCommand() {
    }

    public BitOpCommand(Op op, String destkey, String[] keys) {
        this(op, destkey, keys, null, null);
    }

    public BitOpCommand(Op op, String destkey, String[] keys, byte[] rawDestkey, byte[][] rawKeys) {
        this.op = op;
        this.destkey = destkey;
        this.keys = keys;
        this.rawDestkey = rawDestkey;
        this.rawKeys = rawKeys;
    }

    public Op getOp() {
        return op;
    }

    public void setOp(Op op) {
        this.op = op;
    }

    public String getDestkey() {
        return destkey;
    }

    public void setDestkey(String destkey) {
        this.destkey = destkey;
    }

    public String[] getKeys() {
        return keys;
    }

    public void setKeys(String[] keys) {
        this.keys = keys;
    }

    public byte[] getRawDestkey() {
        return rawDestkey;
    }

    public void setRawDestkey(byte[] rawDestkey) {
        this.rawDestkey = rawDestkey;
    }

    public byte[][] getRawKeys() {
        return rawKeys;
    }

    public void setRawKeys(byte[][] rawKeys) {
        this.rawKeys = rawKeys;
    }

    @Override
    public String toString() {
        return "BitOpCommand{" +
            "op=" + op +
            ", destkey='" + destkey + '\'' +
            ", keys=" + Arrays.toString(keys) +
            '}';
    }
}
