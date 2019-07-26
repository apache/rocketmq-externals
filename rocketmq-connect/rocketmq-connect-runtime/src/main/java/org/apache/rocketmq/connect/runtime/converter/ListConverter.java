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
import com.alibaba.fastjson.JSONArray;
import io.openmessaging.connector.api.data.Converter;
import java.io.UnsupportedEncodingException;
import java.util.List;
import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convert between a list and byte[].
 */
public class ListConverter implements Converter<List> {

    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_RUNTIME);

    private Class clazz;

    public ListConverter(Class clazz) {
        this.clazz = clazz;
    }

    @Override
    public byte[] objectToByte(List list) {
        try {
            return JSON.toJSONString(list).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("ListConverter#objectToByte failed", e);
        }
        return null;
    }

    @Override
    public List byteToObject(byte[] bytes) {
        try {
            String json = new String(bytes, "UTF-8");
            List list = JSONArray.parseArray(json, clazz);
            return list;
        } catch (UnsupportedEncodingException e) {
            log.error("ListConverter#byteToObject failed", e);
        }
        return null;
    }
}
