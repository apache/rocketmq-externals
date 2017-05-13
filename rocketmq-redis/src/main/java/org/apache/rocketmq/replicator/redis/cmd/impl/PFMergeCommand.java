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

package org.apache.rocketmq.replicator.redis.cmd.impl;

import org.apache.rocketmq.replicator.redis.cmd.Command;

import java.util.Arrays;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class PFMergeCommand implements Command {
    private String destkey;
    private String sourcekeys[];

    public PFMergeCommand() {
    }

    public PFMergeCommand(String destkey, String... sourcekeys) {
        this.destkey = destkey;
        this.sourcekeys = sourcekeys;
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

    @Override
    public String toString() {
        return "PFMergeCommand{" +
                "destkey='" + destkey + '\'' +
                ", sourcekey=" + Arrays.toString(sourcekeys) +
                '}';
    }
}
