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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.rocketmq.connect.runtime.service;

import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.apache.rocketmq.connect.runtime.utils.ServiceThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RebalanceService extends ServiceThread {

    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_RUNTIME);

    private static long waitInterval = Long.parseLong(System.getProperty("rocketmq.runtime.cluster.rebalance.waitInterval", "20000"));

    /**
     * Assign all connectors and tasks to all alive process in the cluster.
     */
    private final RebalanceImpl rebalanceImpl;

    /**
     * ConfigManagementService to access current config info.
     */
    private final ConfigManagementService configManagementService;

    /**
     * ClusterManagementService to access current cluster info.
     */
    private final ClusterManagementService clusterManagementService;

    public RebalanceService(RebalanceImpl rebalanceImpl, ConfigManagementService configManagementService,
        ClusterManagementService clusterManagementService) {
        this.rebalanceImpl = rebalanceImpl;
        this.configManagementService = configManagementService;
        this.clusterManagementService = clusterManagementService;
        this.configManagementService.registerListener(new ConnectorConnectorConfigChangeListenerImpl());
        this.clusterManagementService.registerListener(new WorkerStatusListenerImpl());
    }

    @Override
    public void run() {
        log.info(this.getServiceName() + " service started");

        this.rebalanceImpl.checkClusterStoreTopic();

        while (!this.isStopped()) {
            this.waitForRunning(waitInterval);
            this.rebalanceImpl.doRebalance();
        }

        log.info(this.getServiceName() + " service end");
    }

    @Override
    public String getServiceName() {
        return RebalanceService.class.getSimpleName();
    }

    class WorkerStatusListenerImpl implements ClusterManagementService.WorkerStatusListener {

        /**
         * When alive workers change.
         */
        @Override
        public void onWorkerChange() {
            log.info("Wake up rebalance service");
            RebalanceService.this.wakeup();
        }
    }

    class ConnectorConnectorConfigChangeListenerImpl implements ConfigManagementService.ConnectorConfigUpdateListener {

        /**
         * When config change.
         */
        @Override
        public void onConfigUpdate() {
            RebalanceService.this.wakeup();
        }
    }
}
