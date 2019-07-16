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
import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use fastJson to convert object to byte[].
 */
public class JsonConverter implements Converter {

    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_RUNTIME);

    private Class clazz;

    public JsonConverter() {
        this.clazz = null;
    }

    public JsonConverter(Class clazz) {
        this.clazz = clazz;
    }

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    @Override
    public byte[] objectToByte(Object object) {
        try {
            String json = JSON.toJSONString(object);
            return json.getBytes("UTF-8");
        } catch (Exception e) {
            log.error("JsonConverter#objectToByte failed", e);
        }
        return new byte[0];
    }

    @Override
    public Object byteToObject(byte[] bytes) {
        try {
            String text = new String(bytes, "UTF-8");

            Object res;
            if (clazz != null) {
                res = JSON.parseObject(text, clazz);
            } else {
                res = JSON.parse(text);
            }
            return res;
        } catch (UnsupportedEncodingException e) {
            log.error("JsonConverter#byteToObject failed", e);
        }
        return null;
    }
}
