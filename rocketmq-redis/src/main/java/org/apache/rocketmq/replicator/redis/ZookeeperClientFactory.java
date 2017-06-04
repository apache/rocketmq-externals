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

package org.apache.rocketmq.replicator.redis;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.rocketmq.replicator.redis.conf.Configure;

import static org.apache.rocketmq.replicator.redis.conf.ReplicatorConstants.CONFIG_PROP_ZK_ADDRESS;

public class ZookeeperClientFactory {

    private static CuratorFramework client;

    static {
        client = CuratorFrameworkFactory.newClient(Configure.get(CONFIG_PROP_ZK_ADDRESS), new ExponentialBackoffRetry(1000, 3));
        client.start();
    }

    public static CuratorFramework get() {
        return client;
    }
}
