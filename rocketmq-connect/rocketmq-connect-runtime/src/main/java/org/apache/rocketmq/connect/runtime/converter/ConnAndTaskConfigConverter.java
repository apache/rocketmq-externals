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

package org.apache.rocketmq.connect.runtime.converter;

import io.openmessaging.connector.api.data.Converter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import org.apache.rocketmq.connect.runtime.common.ConnAndTaskConfigs;
import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.apache.rocketmq.connect.runtime.utils.TransferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converter data between ConnAndTaskConfigs and byte[].
 */
public class ConnAndTaskConfigConverter implements Converter<ConnAndTaskConfigs> {

    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_RUNTIME);

    @Override
    public byte[] objectToByte(ConnAndTaskConfigs object) {
        try {
            ConnAndTaskConfigs configs = object;
            Map<String, String> connectorMap = new HashMap<>();
            Map<String, String> taskMap = new HashMap<>();
            for (String key : configs.getConnectorConfigs().keySet()) {
                connectorMap.put(key, TransferUtils.keyValueToString(configs.getConnectorConfigs().get(key)));
            }
            for (String key : configs.getTaskConfigs().keySet()) {
                taskMap.put(key, TransferUtils.keyValueListToString(configs.getTaskConfigs().get(key)));
            }
            return TransferUtils.toJsonString(connectorMap, taskMap).getBytes("UTF-8");
        } catch (Exception e) {
            log.error("ConnAndTaskConfigConverter#objectToByte failed", e);
        }
        return new byte[0];
    }

    @Override
    public ConnAndTaskConfigs byteToObject(byte[] bytes) {

        try {
            String jsonString = new String(bytes, "UTF-8");
            ConnAndTaskConfigs configs = TransferUtils.toConnAndTaskConfigs(jsonString);
            return configs;
        } catch (UnsupportedEncodingException e) {
            log.error("ConnAndTaskConfigConverter#byteToObject failed", e);
        }
        return null;
    }
}
