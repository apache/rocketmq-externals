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

package org.apache.rocketmq.connect.redis.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

public class ParseStringUtils {

    public static Map<String, String> parseRedisInfo2Map(String info) {
        Map<String, String> res = new HashMap<>();
        String[] kvs = info.split("\n");
        for (int i = 0; i < kvs.length; i++) {
            String kv = kvs[i];
            if (kv != null && !kv.startsWith("#")){
                String[] kvArr = kv.split(":");
                if(kvArr.length == 2){
                    res.put(StringUtils.trimToEmpty(kvArr[0]), StringUtils.trimToEmpty(kvArr[1]));
                }
            }
        }
        return res;
    }

    public static List<String> parseCommands(String commands){
        if (StringUtils.isEmpty(StringUtils.trimToEmpty(commands))) {
            return null;
        }
        List<String> res = new ArrayList<>();
        String[] commandNameArr = commands.split(",");
        for (int i = 0; i < commandNameArr.length; i++) {
            res.add(commandNameArr[i]);
        }
        return res;
    }
}
