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

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.apache.rocketmq.connect.runtime.connectorwrapper.Worker;
import org.apache.rocketmq.connect.runtime.connectorwrapper.WorkerSinkTask;
import org.apache.rocketmq.connect.runtime.connectorwrapper.WorkerSourceTask;
import org.apache.rocketmq.connect.runtime.utils.ServiceThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A backend service task, commit position periodically.
 */
public class TaskPositionCommitService extends ServiceThread {

    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_RUNTIME);

    private Worker worker;


    private final PositionManagementService positionManagementService;


    private final PositionManagementService offsetManagementService;


    public TaskPositionCommitService(Worker worker,
        PositionManagementService positionManagementService,
        PositionManagementService offsetManagementService) {
        this.worker = worker;
        this.positionManagementService = positionManagementService;
        this.offsetManagementService = offsetManagementService;
    }

    @Override
    public void run() {
        log.info(this.getServiceName() + " service started");

        while (!this.isStopped()) {
            this.waitForRunning(10000);
            commitTaskPosition();
        }

        log.info(this.getServiceName() + " service end");
    }

    @Override
    public String getServiceName() {
        return TaskPositionCommitService.class.getSimpleName();
    }


    public void commitTaskPosition() {
        Map<ByteBuffer, ByteBuffer> positionData = new HashMap<>();
        Map<ByteBuffer, ByteBuffer> offsetData = new HashMap<>();
        for (Runnable task : worker.getWorkingTasks()) {
            if (task instanceof WorkerSourceTask) {
                positionData.putAll(((WorkerSourceTask) task).getPositionData());
                positionManagementService.putPosition(positionData);
            } else if (task instanceof WorkerSinkTask) {
                offsetData.putAll(((WorkerSinkTask) task).getOffsetData());
                offsetManagementService.putPosition(offsetData);
            }
        }
    }
}
