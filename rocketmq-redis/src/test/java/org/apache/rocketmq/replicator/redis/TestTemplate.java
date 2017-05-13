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

import junit.framework.TestCase;

import java.io.IOException;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public abstract class TestTemplate extends TestCase {
    public void testSocket(final String host, final int port, final Configuration configuration, final long sleep) throws IOException {
        final Replicator replicator = new RedisReplicator(host,
                port,
                configuration);
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(sleep);
                    replicator.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
        test(replicator);
        replicator.open();
    }

    protected abstract void test(Replicator replicator);
}
