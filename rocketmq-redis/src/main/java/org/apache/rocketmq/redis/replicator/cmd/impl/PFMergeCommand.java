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

public class PFMergeCommand implements Command {

    private static final long serialVersionUID = 1L;

    private String destkey;
    private String[] sourcekeys;
    private byte[] rawDestkey;
    private byte[][] rawSourcekeys;

    public PFMergeCommand() {
    }

    public PFMergeCommand(String destkey, String[] sourcekeys) {
        this(destkey, sourcekeys, null, null);
    }

    public PFMergeCommand(String destkey, String[] sourcekeys, byte[] rawDestkey, byte[][] rawSourcekeys) {
        this.destkey = destkey;
        this.sourcekeys = sourcekeys;
        this.rawDestkey = rawDestkey;
        this.rawSourcekeys = rawSourcekeys;
    }

    public String getDestkey() {
        return destkey;
    }

    public void setDestkey(String destkey) {
        this.destkey = destkey;
    }

    public String[] getSourcekeys() {
        return sourcekeys;
    }

    public void setSourcekeys(String[] sourcekeys) {
        this.sourcekeys = sourcekeys;
    }

    public byte[] getRawDestkey() {
        return rawDestkey;
    }

    public void setRawDestkey(byte[] rawDestkey) {
        this.rawDestkey = rawDestkey;
    }

    public byte[][] getRawSourcekeys() {
        return rawSourcekeys;
    }

    public void setRawSourcekeys(byte[][] rawSourcekeys) {
        this.rawSourcekeys = rawSourcekeys;
    }

    @Override
    public String toString() {
        return "PFMergeCommand{" +
            "destkey='" + destkey + '\'' +
            ", sourcekey=" + Arrays.toString(sourcekeys) +
            '}';
    }
}
