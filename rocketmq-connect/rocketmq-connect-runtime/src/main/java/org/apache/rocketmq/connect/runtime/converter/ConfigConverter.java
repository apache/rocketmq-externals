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
import org.apache.rocketmq.connect.runtime.common.ConfigWrapper;
import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.apache.rocketmq.connect.runtime.utils.TransferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class ConfigConverter implements Converter<ConfigWrapper> {

    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_RUNTIME);

    @Override
    public byte[] objectToByte(ConfigWrapper config) {
        try {
            ConfigWrapper configs = config;
            Map<String, String> connectorMap = new HashMap<>();
            Map<String, String> taskMap = new HashMap<>();
            for (String key : configs.getConnAndTaskConfigs().getConnectorConfigs().keySet()) {
                connectorMap.put(key, TransferUtils.keyValueToString(configs.getConnAndTaskConfigs().getConnectorConfigs().get(key)));
            }
            for (String key : configs.getConnAndTaskConfigs().getTaskConfigs().keySet()) {
                taskMap.put(key, TransferUtils.keyValueListToString(configs.getConnAndTaskConfigs().getTaskConfigs().get(key)));
            }
            return TransferUtils.toJsonString(config.getLeader(), connectorMap, taskMap).getBytes("UTF-8");
        } catch (Exception e) {
            log.error("ConfigConverter#objectToByte failed", e);
        }
        return new byte[0];
    }

    @Override
    public ConfigWrapper byteToObject(byte[] bytes) {
        try {
            String jsonString = new String(bytes, "UTF-8");
            ConfigWrapper configs = TransferUtils.toConfigs(jsonString);
            return configs;
        } catch (UnsupportedEncodingException e) {
            log.error("ConfigConverter#byteToObject failed", e);
        }
        return null;
    }
}
