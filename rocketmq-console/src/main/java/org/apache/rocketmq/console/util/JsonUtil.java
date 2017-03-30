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

package org.apache.rocketmq.console.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public class JsonUtil {

    private static Logger logger = LoggerFactory.getLogger(JsonUtil.class);
    private static ObjectMapper objectMapper = new ObjectMapper();

    private JsonUtil() {
    }

    static {
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.setFilters(new SimpleFilterProvider().setFailOnUnknownId(false));
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    }

    public static void writeValue(Writer writer, Object obj) {
        try {
            objectMapper.writeValue(writer, obj);
        }
        catch (IOException e) {
            Throwables.propagateIfPossible(e);
        }
    }

    public static <T> String obj2String(T src) {
        if (src == null) {
            return null;
        }

        try {
            return src instanceof String ? (String)src : objectMapper.writeValueAsString(src);
        }
        catch (Exception e) {
            logger.error("Parse Object to String error src=" + src, e);
            return null;
        }
    }

    public static <T> byte[] obj2Byte(T src) {
        if (src == null) {
            return null;
        }

        try {
            return src instanceof byte[] ? (byte[])src : objectMapper.writeValueAsBytes(src);
        }
        catch (Exception e) {
            logger.error("Parse Object to byte[] error", e);
            return null;
        }
    }

    public static <T> T string2Obj(String str, Class<T> clazz) {
        if (Strings.isNullOrEmpty(str) || clazz == null) {
            return null;
        }
        str = escapesSpecialChar(str);
        try {
            return clazz.equals(String.class) ? (T)str : objectMapper.readValue(str, clazz);
        }
        catch (Exception e) {
            logger.error("Parse String to Object error\nString: {}\nClass<T>: {}\nError: {}", str, clazz.getName(), e);
            return null;
        }
    }

    public static <T> T byte2Obj(byte[] bytes, Class<T> clazz) {
        if (bytes == null || clazz == null) {
            return null;
        }
        try {
            return clazz.equals(byte[].class) ? (T)bytes : objectMapper.readValue(bytes, clazz);
        }
        catch (Exception e) {
            logger.error("Parse byte[] to Object error\nbyte[]: {}\nClass<T>: {}\nError: {}", bytes, clazz.getName(), e);
            return null;
        }
    }

    public static <T> T string2Obj(String str, TypeReference<T> typeReference) {
        if (Strings.isNullOrEmpty(str) || typeReference == null) {
            return null;
        }
        str = escapesSpecialChar(str);
        try {
            return (T)(typeReference.getType().equals(String.class) ? str : objectMapper.readValue(str, typeReference));
        }
        catch (Exception e) {
            logger.error("Parse String to Object error\nString: {}\nTypeReference<T>: {}\nError: {}", str,
                typeReference.getType(), e);
            return null;
        }
    }

    public static <T> T byte2Obj(byte[] bytes, TypeReference<T> typeReference) {
        if (bytes == null || typeReference == null) {
            return null;
        }
        try {
            return (T)(typeReference.getType().equals(byte[].class) ? bytes : objectMapper.readValue(bytes,
                typeReference));
        }
        catch (Exception e) {
            logger.error("Parse byte[] to Object error\nbyte[]: {}\nTypeReference<T>: {}\nError: {}", bytes,
                typeReference.getType(), e);
            return null;
        }
    }

    public static <T> T map2Obj(Map<String, String> map, Class<T> clazz) {
        String str = obj2String(map);
        return string2Obj(str, clazz);
    }

    private static String escapesSpecialChar(String str) {
        return str.replace("\n", "\\n").replace("\r", "\\r");
    }
}
