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
import java.util.List;
import java.util.Map;
import org.apache.rocketmq.connect.runtime.common.ConnectKeyValue;
import org.apache.rocketmq.connect.runtime.utils.Plugin;

/**
 * Interface for config manager. Contains connector configs and task configs. All worker in a cluster should keep the
 * same configs.
 */
public interface ConfigManagementService {

    /**
     * Start the config manager.
     */
    void start();

    /**
     * Stop the config manager.
     */
    void stop();

    /**
     * Get all connector configs from the cluster.
     *
     * @return
     */
    Map<String, ConnectKeyValue> getConnectorConfigs();

    /**
     * Put the configs of the specified connector in the cluster.
     *
     * @param connectorName
     * @param configs
     * @return
     * @throws Exception
     */
    String putConnectorConfig(String connectorName, ConnectKeyValue configs) throws Exception;

    /**
     * Remove the connector with the specified connector name in the cluster.
     *
     * @param connectorName
     */
    void removeConnectorConfig(String connectorName);

    void recomputeTaskConfigs(String connectorName, Connector connector, Long currentTimestamp);

    /**
     * Get all Task configs.
     *
     * @return
     */
    Map<String, List<ConnectKeyValue>> getTaskConfigs();

    /**
     * Persist all the configs in a store.
     */
    void persist();

    /**
     * Register a listener to listen all config update operations.
     *
     * @param listener
     */
    void registerListener(ConnectorConfigUpdateListener listener);

    interface ConnectorConfigUpdateListener {

        /**
         * Invoke while connector config changed.
         */
        void onConfigUpdate();
    }

    Plugin getPlugin();
}
