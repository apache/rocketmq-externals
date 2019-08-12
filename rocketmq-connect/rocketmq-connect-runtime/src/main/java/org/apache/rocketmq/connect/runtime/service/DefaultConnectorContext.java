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

import io.openmessaging.connector.api.Connector;
import io.openmessaging.connector.api.ConnectorContext;
import java.util.Set;
import org.apache.rocketmq.connect.runtime.ConnectController;
import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.apache.rocketmq.connect.runtime.connectorwrapper.WorkerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultConnectorContext implements ConnectorContext {

    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_RUNTIME);

    private final ConnectController controller;

    private final String connectorName;

    public DefaultConnectorContext(String connectorName, ConnectController connectController) {
        this.controller = connectController;
        this.connectorName = connectorName;
    }

    @Override public void requestTaskReconfiguration() {
        Set<WorkerConnector> connectors = controller.getWorker().getWorkingConnectors();
        WorkerConnector currentConnector = null;
        for (WorkerConnector workerConnector : connectors) {
            if (workerConnector.getConnectorName().equals(connectorName)) {
                currentConnector = workerConnector;
            }
        }
        if (null != currentConnector) {
            Connector connector = currentConnector.getConnector();
            controller.getConfigManagementService().recomputeTaskConfigs(connectorName, connector, System.currentTimeMillis());
            log.info("Connector {} recompute taskConfigs success.", connectorName);
        } else {
            log.info("Not found connector {}.", connectorName);
        }
    }

    @Override public void raiseError(Exception e) {
        log.error("Exception", e);
    }
}
