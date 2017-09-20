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

package org.apache.rocketmq.mysql.ha;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.rocketmq.mysql.Replicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MasterElectionLatch implements LeaderLatchListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MasterElectionLatch.class);

    private Replicator replicator;

    public MasterElectionLatch(Replicator replicator) {

        this.replicator = replicator;
    }

    public void elect() throws Exception {

        CuratorFramework client = CuratorFrameworkFactory.builder()
            .connectString(replicator.getConfig().zkAddr)
            .retryPolicy(new ExponentialBackoffRetry(1000, 3))
            .sessionTimeoutMs(3000)
            .connectionTimeoutMs(3000)
            .namespace("rocketmq-mysql-replicator")
            .build();
        client.start();

        LeaderLatch leaderLatch = new LeaderLatch(client, "/master", "replicator");
        leaderLatch.addListener(this);
        leaderLatch.start();
    }

    @Override
    public void isLeader() {

        LOGGER.info("ZK_EVENT:isLeader!");

        replicator.startProcess();
    }

    @Override
    public void notLeader() {

        LOGGER.info("ZK_EVENT:notLeader!");

        replicator.stopProcess();
    }


}
