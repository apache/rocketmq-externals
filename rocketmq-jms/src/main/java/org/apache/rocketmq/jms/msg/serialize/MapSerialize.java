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

package org.apache.rocketmq.jms.msg.serialize;

import com.alibaba.fastjson.JSON;
import java.util.HashMap;
import java.util.Map;
import javax.jms.JMSException;

public class MapSerialize implements Serialize<Map> {

    private static MapSerialize ins = new MapSerialize();

    public static MapSerialize instance() {
        return ins;
    }

    @Override public byte[] serialize(Map map) throws JMSException {
        return JSON.toJSONBytes(map);
    }

    private MapSerialize() {
    }

    @Override public Map deserialize(byte[] bytes) throws JMSException {
        return JSON.parseObject(bytes, HashMap.class);
    }
}
