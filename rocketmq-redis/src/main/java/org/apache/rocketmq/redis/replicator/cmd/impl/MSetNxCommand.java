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

import java.util.Map;
import org.apache.rocketmq.redis.replicator.cmd.Command;

public class MSetNxCommand implements Command {

    private static final long serialVersionUID = 1L;

    private Map<String, String> kv;
    private Map<byte[], byte[]> rawKv;

    public MSetNxCommand() {
    }

    public MSetNxCommand(Map<String, String> kv) {
        this(kv, null);
    }

    public MSetNxCommand(Map<String, String> kv, Map<byte[], byte[]> rawKv) {
        this.kv = kv;
        this.rawKv = rawKv;
    }

    public Map<String, String> getKv() {
        return kv;
    }

    public void setKv(Map<String, String> kv) {
        this.kv = kv;
    }

    public Map<byte[], byte[]> getRawKv() {
        return rawKv;
    }

    public void setRawKv(Map<byte[], byte[]> rawKv) {
        this.rawKv = rawKv;
    }

    @Override
    public String toString() {
        return "MSetNxCommand{" +
            "kv=" + kv +
            '}';
    }
}
