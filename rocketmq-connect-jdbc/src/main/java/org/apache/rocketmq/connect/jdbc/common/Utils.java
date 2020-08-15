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
package org.apache.rocketmq.connect.jdbc.common;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Utils {
    private static final Logger log = LoggerFactory.getLogger(Utils.class);

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
        List<String> namesrvList = new ArrayList<>();
        for (String ns : namesrvArray) {
            if (!namesrvList.contains(ns)) {
                namesrvList.add(ns);
            }
        }
        Collections.sort(namesrvList);
        return String.valueOf(namesrvList.toString().hashCode());
    }

    public static List<BrokerData> examineBrokerData(DefaultMQAdminExt defaultMQAdminExt, String topic,
        String cluster) throws RemotingException, MQClientException, InterruptedException {
        List<BrokerData> brokerList = new ArrayList<>();

        TopicRouteData topicRouteData = defaultMQAdminExt.examineTopicRouteInfo(topic);
        if (topicRouteData.getBrokerDatas() != null) {
            for (BrokerData broker : topicRouteData.getBrokerDatas()) {
                if (StringUtils.equals(broker.getCluster(), cluster)) {
                    brokerList.add(broker);
                }
            }
        }
        return brokerList;
    }

}
