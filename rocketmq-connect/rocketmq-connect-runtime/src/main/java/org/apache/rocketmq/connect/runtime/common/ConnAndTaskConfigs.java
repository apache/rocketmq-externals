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

package org.apache.rocketmq.connect.runtime.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulation of connector configs and  task configs.
 */
public class ConnAndTaskConfigs {

    private Map<String, ConnectKeyValue> connectorConfigs = new HashMap<>();
    private Map<String, List<ConnectKeyValue>> taskConfigs = new HashMap<>();

    public Map<String, ConnectKeyValue> getConnectorConfigs() {
        return connectorConfigs;
    }

    public void setConnectorConfigs(Map<String, ConnectKeyValue> connectorConfigs) {
        this.connectorConfigs = connectorConfigs;
    }

    public Map<String, List<ConnectKeyValue>> getTaskConfigs() {
        return taskConfigs;
    }

    public void setTaskConfigs(Map<String, List<ConnectKeyValue>> taskConfigs) {
        this.taskConfigs = taskConfigs;
    }

    @Override public String toString() {
        return "ConnAndTaskConfigs{" +
            "connectorConfigs=" + connectorConfigs +
            ", taskConfigs=" + taskConfigs +
            '}';
    }
}
