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

package org.apache.rocketmq.connect.runtime.store;

import com.alibaba.fastjson.JSON;
import io.openmessaging.connector.api.data.Converter;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.apache.rocketmq.connect.runtime.utils.FileAndPropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File based Key value store.
 *
 * @param <K>
 * @param <V>
 */
public class FileBaseKeyValueStore<K, V> extends MemoryBasedKeyValueStore<K, V> {

    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_RUNTIME);

    private String configFilePath;
    private Converter keyConverter;
    private Converter valueConverter;

    public FileBaseKeyValueStore(String configFilePath,
        Converter keyConverter,
        Converter valueConverter) {

        super();
        this.configFilePath = configFilePath;
        this.keyConverter = keyConverter;
        this.valueConverter = valueConverter;
    }

    public String encode() {

        Map<String, String> map = new HashMap<>();
        for (K key : data.keySet()) {
            byte[] keyByte = keyConverter.objectToByte(key);
            byte[] valueByte = valueConverter.objectToByte(data.get(key));
            map.put(Base64.getEncoder().encodeToString(keyByte), Base64.getEncoder().encodeToString(valueByte));
        }
        return JSON.toJSONString(map);
    }

    public void decode(String jsonString) {

        Map<K, V> resultMap = new HashMap<>();
        Map<String, String> map = JSON.parseObject(jsonString, Map.class);
        for (String key : map.keySet()) {
            K decodeKey = (K) keyConverter.byteToObject(Base64.getDecoder().decode(key));
            V decodeValue = (V) valueConverter.byteToObject(Base64.getDecoder().decode(map.get(key)));
            resultMap.put(decodeKey, decodeValue);
        }
        this.data = resultMap;
    }

    @Override
    public boolean load() {
        String fileName = null;
        try {
            fileName = this.configFilePath;
            String jsonString = FileAndPropertyUtil.file2String(fileName);

            if (null == jsonString || jsonString.length() == 0) {
                return this.loadBak();
            } else {
                this.decode(jsonString);
                log.info("load " + fileName + " OK");
                return true;
            }
        } catch (Exception e) {
            log.error("load " + fileName + " failed, and try to load backup file", e);
            return this.loadBak();
        }
    }

    private boolean loadBak() {
        String fileName = null;
        try {
            fileName = this.configFilePath;
            String jsonString = FileAndPropertyUtil.file2String(fileName + ".bak");
            if (jsonString != null && jsonString.length() > 0) {
                this.decode(jsonString);
                log.info("load " + fileName + " OK");
                return true;
            }
        } catch (Exception e) {
            log.error("load " + fileName + " Failed", e);
            return false;
        }

        return true;
    }

    @Override
    public void persist() {

        String jsonString = this.encode();
        if (jsonString != null) {
            String fileName = this.configFilePath;
            try {
                FileAndPropertyUtil.string2File(jsonString, fileName);
            } catch (IOException e) {
                log.error("persist file " + fileName + " exception", e);
            }
        }
    }
}
