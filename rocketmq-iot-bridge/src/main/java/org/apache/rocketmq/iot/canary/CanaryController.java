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

package org.apache.rocketmq.iot.canary;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.rocketmq.iot.canary.config.CanaryConfig;
import org.apache.rocketmq.iot.canary.service.AvailabilityService;
import org.apache.rocketmq.iot.canary.service.ClusterConnectService;
import org.apache.rocketmq.iot.canary.service.FalconDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CanaryController {
    private Logger logger = LoggerFactory.getLogger(CanaryController.class);

    private CanaryConfig canaryConfig;
    private ScheduledExecutorService scheduledExecutorService;
    private FalconDataService availabilityService;
    private FalconDataService connectService;

    public CanaryController(CanaryConfig canaryConfig) {
        this.canaryConfig = canaryConfig;
        this.scheduledExecutorService = Executors.newScheduledThreadPool(5);
        this.availabilityService = new AvailabilityService(canaryConfig);
        this.connectService = new ClusterConnectService(canaryConfig);
    }

    public void start() {
        // availability, latency
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                availabilityService.report();
                availabilityService.shutdown();
            }
        }, canaryConfig.getCanaryIntervalMillis(), canaryConfig.getCanaryIntervalMillis(), TimeUnit.MILLISECONDS);
        logger.info("start availability, latency scheduled.");

        // connectNumber
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                connectService.report();
                connectService.shutdown();
            }
        }, canaryConfig.getCanaryIntervalMillis(), canaryConfig.getCanaryIntervalMillis(), TimeUnit.MILLISECONDS);
        logger.info("start cluster connect scheduled.");
    }
}
