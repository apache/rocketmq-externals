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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.connect.redis.converter;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import com.alibaba.fastjson.JSONObject;

import org.apache.rocketmq.connect.redis.common.RedisConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisPositionConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisPositionConverter.class);

    public static Long jsonToLong(ByteBuffer byteBuffer){
        if(byteBuffer == null){
            return null;
        }
        try {
            String positionJson = new String(byteBuffer.array(), "UTF-8");
            JSONObject jsonObject = JSONObject.parseObject(positionJson);
            if(jsonObject != null){
                Long position = jsonObject.getLong(RedisConstants.POSITION);
                return position;
            }
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("position encoding error. {}", e);
        }
        return null;
    }

    public static JSONObject longToJson(Long replOffset) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(RedisConstants.POSITION, replOffset);
        return jsonObject;
    }
}
