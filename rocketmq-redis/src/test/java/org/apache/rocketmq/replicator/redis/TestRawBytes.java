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

package org.apache.rocketmq.replicator.redis;

import org.apache.rocketmq.replicator.redis.rdb.RdbListener;
import org.apache.rocketmq.replicator.redis.rdb.datatype.KeyStringValueString;
import org.apache.rocketmq.replicator.redis.rdb.datatype.KeyValuePair;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class TestRawBytes {
    public static void main(String[] args) throws IOException, InterruptedException {
        Replicator replicator = new RedisRdbReplicator(
                TestRawBytes.class.getClassLoader().getResourceAsStream("dumpV7.rdb"),
                Configuration.defaultSetting());
        replicator.addRdbListener(new RdbListener.Adaptor() {
            @Override
            public void handle(Replicator replicator, KeyValuePair<?> kv) {
                if (kv.getValueRdbType() == 0) {
                    KeyStringValueString ksvs = (KeyStringValueString) kv;
                    System.out.println("key:" + ksvs.getKey() + ",value:" + ksvs.getValue() + ",len:" + ksvs.getRawBytes().length + "," + Arrays.toString(ksvs.getRawBytes()));
                }
            }
        });
        replicator.open();
    }
}
