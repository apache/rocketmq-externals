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

package org.apache.rocketmq.console.service.impl;

import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.apache.rocketmq.console.service.DashboardCollectService;
import org.apache.rocketmq.console.service.DashboardService;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Resource
    private DashboardCollectService dashboardCollectService;
    /**
     * @param date format yyyy-MM-dd
     * @return
     */
    @Override public Map<String, List<String>> queryBrokerData(String date) {
        return dashboardCollectService.getBrokerCache().asMap();
    }
}
