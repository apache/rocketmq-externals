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

package org.apache.rocketmq.connect.runtime.utils;

import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class UUIDUtil {

    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_RUNTIME);

    private static final AtomicInteger CNT = new AtomicInteger(0);

    private UUIDUtil() {
    }

    public static String getUUID(String clientID, String connectorName) {
        StringBuffer sb = new StringBuffer();
        sb.append(clientID);
        sb.append(connectorName);
        sb.append(System.currentTimeMillis());
        sb.append(CNT.getAndIncrement());
        String rs = sb.toString();
        rs = UUID.nameUUIDFromBytes(rs.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
        return rs;
    }
}
