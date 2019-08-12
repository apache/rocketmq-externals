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

import com.alibaba.fastjson.JSON;
import io.openmessaging.connector.api.data.Converter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Byte Map to byte[].
 */
public class ByteMapConverter implements Converter<Map<ByteBuffer, ByteBuffer>> {

    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_RUNTIME);

    @Override
    public byte[] objectToByte(Map<ByteBuffer, ByteBuffer> map) {

        try {
            Map<String, String> resultMap = new HashMap<>();

            for (Map.Entry<ByteBuffer, ByteBuffer> entry : map.entrySet()) {
                resultMap.put(Base64.getEncoder().encodeToString(entry.getKey().array()), Base64.getEncoder().encodeToString(entry.getValue().array()));
            }
            return JSON.toJSONString(resultMap).getBytes("UTF-8");
        } catch (Exception e) {
            log.error("ByteMapConverter#objectToByte failed", e);
        }
        return new byte[0];
    }

    @Override
    public Map<ByteBuffer, ByteBuffer> byteToObject(byte[] bytes) {

        Map<ByteBuffer, ByteBuffer> resultMap = new HashMap<>();
        try {
            String rawString = new String(bytes, "UTF-8");
            Map<String, String> map = JSON.parseObject(rawString, Map.class);
            for (String key : map.keySet()) {
                byte[] decodeKey = Base64.getDecoder().decode(key);
                byte[] decodeValue = Base64.getDecoder().decode(map.get(key));
                resultMap.put(ByteBuffer.wrap(decodeKey), ByteBuffer.wrap(decodeValue));
            }
            return resultMap;
        } catch (UnsupportedEncodingException e) {
            log.error("ByteMapConverter#byteToObject failed", e);
        }
        return resultMap;
    }

}
