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
package org.apache.rocketmq.console.service;

import org.apache.rocketmq.tools.admin.MQAdminExt;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;

public abstract class AbstractCommonService {
    @Resource
    protected MQAdminExt mqAdminExt;
    protected final Set<String> changeToBrokerNameSet(HashMap<String, Set<String>> clusterAddrTable,
        List<String> clusterNameList, List<String> brokerNameList) {
        Set<String> finalBrokerNameList = Sets.newHashSet();
        if (CollectionUtils.isNotEmpty(clusterNameList)) {
            try {
                for (String clusterName : clusterNameList) {
                    finalBrokerNameList.addAll(clusterAddrTable.get(clusterName));
                }
            }
            catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
        if (CollectionUtils.isNotEmpty(brokerNameList)) {
            finalBrokerNameList.addAll(brokerNameList);
        }
        return finalBrokerNameList;
    }
}
