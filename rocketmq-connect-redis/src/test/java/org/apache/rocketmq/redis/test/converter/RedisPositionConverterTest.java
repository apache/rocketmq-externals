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

package org.apache.rocketmq.redis.test.converter;

import com.alibaba.fastjson.JSONObject;
import java.nio.ByteBuffer;
import org.apache.rocketmq.connect.redis.common.Config;
import org.apache.rocketmq.connect.redis.common.RedisConstants;
import org.apache.rocketmq.connect.redis.converter.RedisPositionConverter;
import org.junit.Assert;
import org.junit.Test;

public class RedisPositionConverterTest {
    @Test
    public void test(){
        JSONObject jsonObject = RedisPositionConverter.longToJson(10000L);
        long offset = jsonObject.getLong(RedisConstants.POSITION);
        Assert.assertEquals(10000L, offset);

        Config config = getConfig();
        ByteBuffer byteBuffer = ByteBuffer.wrap(jsonObject.toJSONString().getBytes());
        Long position = RedisPositionConverter.jsonToLong(byteBuffer);
        config.setPosition(position);
        Assert.assertEquals(10000L, (long)config.getPosition());
    }

    private Config getConfig(){
        Config config = new Config();

        config.setRedisAddr("127.0.0.1");
        config.setRedisPort(6379);
        config.setRedisPassword("");
        config.setEventCommitRetryTimes(10);
        return config;
    }
}
