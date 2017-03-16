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

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.apache.rocketmq.console.config.RMQConfigure;
import org.apache.rocketmq.console.service.AbstractCommonService;
import org.apache.rocketmq.console.service.OpsService;
import org.apache.rocketmq.console.service.checker.CheckerType;
import org.apache.rocketmq.console.service.checker.RocketMqChecker;
import org.springframework.stereotype.Service;

@Service
public class OpsServiceImpl extends AbstractCommonService implements OpsService {

    @Resource
    private RMQConfigure rMQConfigure;

    @Resource
    private List<RocketMqChecker> rocketMqCheckerList;

    @Override
    public Map<String, Object> homePageInfo() {
        Map<String, Object> homePageInfoMap = Maps.newHashMap();
        homePageInfoMap.put("namesvrAddrList", Splitter.on(";").splitToList(rMQConfigure.getNamesrvAddr()));
        homePageInfoMap.put("useVIPChannel", Boolean.valueOf(rMQConfigure.getIsVIPChannel()));
        return homePageInfoMap;
    }

    @Override
    public void updateNameSvrAddrList(String nameSvrAddrList) {
        rMQConfigure.setNamesrvAddr(nameSvrAddrList);
    }

    @Override
    public String getNameSvrList() {
        return rMQConfigure.getNamesrvAddr();
    }

    @Override
    public Map<CheckerType, Object> rocketMqStatusCheck() {
        Map<CheckerType, Object> checkResultMap = Maps.newHashMap();
        for (RocketMqChecker rocketMqChecker : rocketMqCheckerList) {
            checkResultMap.put(rocketMqChecker.checkerType(), rocketMqChecker.doCheck());
        }
        return checkResultMap;
    }

    @Override public boolean updateIsVIPChannel(String useVIPChannel) {
        rMQConfigure.setIsVIPChannel(useVIPChannel);
        return true;
    }
}
