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

package org.apache.rocketmq.jms.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.rocketmq.jms.domain.CommonConstant;

public abstract class URISpecParser {

    private static final String DEFAULT_BROKER = "rocketmq";

    /**
     * ConnectionUrl spec is broker://ip:port?key1=value1&key2=value2
     *
     * @param uri Just like broker://ip:port?key1=value1&key2=value2
     * @return The parameters' map
     */
    public static Map<String, String> parseURI(String uri) {
        Preconditions.checkArgument(null != uri && !uri.trim().isEmpty(), "Uri can not be empty!");

        Map<String, String> results = Maps.newHashMap();
        String broker = uri.substring(0, uri.indexOf(":"));
        results.put(CommonConstant.PROVIDER, broker);

        if (broker.equals(DEFAULT_BROKER)) {
            //Special handle for alibaba inner mq broker
            String queryStr = uri.substring(uri.indexOf("?") + 1, uri.length());
            if (StringUtils.isNotEmpty(queryStr)) {
                String[] params = queryStr.split("&");
                for (String param : params) {
                    if (param.contains("=")) {
                        String[] values = param.split("=", 2);
                        results.put(values[0], values[1]);
                    }
                }
            }
        }
        else {
            throw new IllegalArgumentException("Broker must be rocketmq");
        }
        return results;
    }
}
