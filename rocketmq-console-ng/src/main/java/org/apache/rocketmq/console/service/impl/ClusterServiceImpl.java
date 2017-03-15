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

package org.apache.rocketmq.console.service.impl;

import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.body.KVTable;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.apache.rocketmq.console.service.ClusterService;
import org.apache.rocketmq.console.util.JsonUtil;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Properties;

@Service
public class ClusterServiceImpl implements ClusterService {
    private Logger logger = LoggerFactory.getLogger(ClusterServiceImpl.class);
    @Resource
    private MQAdminExt mqAdminExt;

    @Override
    public Map<String, Object> list() {
        try {
            Map<String, Object> resultMap = Maps.newHashMap();
            ClusterInfo clusterInfo = mqAdminExt.examineBrokerClusterInfo();
            logger.info("op=look_clusterInfo {}", JsonUtil.obj2String(clusterInfo));
            Map<String/*brokerName*/, Map<Long/* brokerId */, Object/* brokerDetail */>> brokerServer = Maps.newHashMap();
            for (BrokerData brokerData : clusterInfo.getBrokerAddrTable().values()) {
                Map<Long, Object> brokerMasterSlaveMap = Maps.newHashMap();
                for (Map.Entry<Long/* brokerId */, String/* broker address */> brokerAddr : brokerData.getBrokerAddrs().entrySet()) {
                    KVTable kvTable = mqAdminExt.fetchBrokerRuntimeStats(brokerAddr.getValue());
//                KVTable kvTable = mqAdminExt.fetchBrokerRuntimeStats("127.0.0.1:10911");
                    brokerMasterSlaveMap.put(brokerAddr.getKey(), kvTable.getTable());
                }
                brokerServer.put(brokerData.getBrokerName(), brokerMasterSlaveMap);
            }
            resultMap.put("clusterInfo", clusterInfo);
            resultMap.put("brokerServer", brokerServer);
            return resultMap;
        }
        catch (Exception err) {
            throw Throwables.propagate(err);
        }
    }


    @Override
    public Properties getBrokerConfig(String brokerAddr) {
        try {
            return mqAdminExt.getBrokerConfig(brokerAddr);
        }
        catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
