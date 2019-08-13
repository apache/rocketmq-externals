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
package org.apache.rocketmq.replicator.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Utils {

    public static String createGroupName(String prefix) {
        return new StringBuilder().append(prefix).append("-").append(System.currentTimeMillis()).toString();
    }

    public static String createGroupName(String prefix, String postfix) {
        return new StringBuilder().append(prefix).append("-").append(postfix).toString();
    }

    public static String createTaskId(String prefix) {
        return new StringBuilder().append(prefix).append("-").append(System.currentTimeMillis()).toString();
    }

    public static String createInstanceName(String namesrvAddr) {
        String[] namesrvArray = namesrvAddr.split(";");
        List<String> namesrvList = new ArrayList<String>();
        for (String ns: namesrvArray) {
            if (!namesrvList.contains(ns)) {
                namesrvList.add(ns);
            }
        }
        Collections.sort(namesrvList);
        return String.valueOf(namesrvList.toString().hashCode());
    }
}
