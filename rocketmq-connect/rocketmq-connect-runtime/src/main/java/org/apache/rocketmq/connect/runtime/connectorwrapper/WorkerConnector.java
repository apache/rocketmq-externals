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

package org.apache.rocketmq.connect.runtime.connectorwrapper;

import com.alibaba.fastjson.JSON;
import io.openmessaging.connector.api.Connector;
import io.openmessaging.connector.api.ConnectorContext;
import org.apache.rocketmq.connect.runtime.common.ConnectKeyValue;

/**
 * A wrapper of {@link Connector} for runtime.
 */
public class WorkerConnector {

    /**
     * Name of the worker connector.
     */
    private String connectorName;

    /**
     * Instance of a connector implements.
     */
    private Connector connector;

    /**
     * The configs for the current connector.
     */
    private ConnectKeyValue keyValue;

    private final ConnectorContext context;

    public WorkerConnector(String connectorName, Connector connector, ConnectKeyValue keyValue, ConnectorContext context) {
        this.connectorName = connectorName;
        this.connector = connector;
        this.keyValue = keyValue;
        this.context = context;
    }

    public void initialize() {
        connector.initialize(this.context);
    }

    public void start() {
        connector.verifyAndSetConfig(keyValue);
        connector.start();
    }

    public void stop() {
        connector.stop();
    }

    public String getConnectorName() {
        return connectorName;
    }

    public ConnectKeyValue getKeyValue() {
        return keyValue;
    }

    public void reconfigure(ConnectKeyValue keyValue) {
        this.keyValue = keyValue;
        stop();
        start();
    }

    public Connector getConnector() {
        return connector;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("connectorName:" + connectorName)
            .append("\nConfigs:" + JSON.toJSONString(keyValue));
        return sb.toString();
    }
}
