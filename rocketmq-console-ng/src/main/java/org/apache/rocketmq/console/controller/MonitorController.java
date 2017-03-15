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
package org.apache.rocketmq.console.controller;

import javax.annotation.Resource;
import org.apache.rocketmq.console.model.ConsumerMonitorConfig;
import org.apache.rocketmq.console.service.MonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/monitor")
public class MonitorController {

    private Logger logger = LoggerFactory.getLogger(MonitorController.class);
    @Resource
    private MonitorService monitorService;

    @RequestMapping(value = "/createOrUpdateConsumerMonitor.do", method = {RequestMethod.POST})
    @ResponseBody
    public Object createOrUpdateConsumerMonitor(@RequestParam String consumeGroupName, @RequestParam int minCount,
        @RequestParam int maxDiffTotal) {
        return monitorService.createOrUpdateConsumerMonitor(consumeGroupName, new ConsumerMonitorConfig(minCount, maxDiffTotal));
    }

    @RequestMapping(value = "/consumerMonitorConfig.query", method = {RequestMethod.GET})
    @ResponseBody
    public Object consumerMonitorConfig() {
        return monitorService.queryConsumerMonitorConfig();
    }

    @RequestMapping(value = "/consumerMonitorConfigByGroupName.query", method = {RequestMethod.GET})
    @ResponseBody
    public Object consumerMonitorConfigByGroupName(@RequestParam String consumeGroupName) {
        return monitorService.queryConsumerMonitorConfigByGroupName(consumeGroupName);
    }

    @RequestMapping(value = "/deleteConsumerMonitor.do", method = {RequestMethod.POST})
    @ResponseBody
    public Object deleteConsumerMonitor(@RequestParam String consumeGroupName) {
        return monitorService.deleteConsumerMonitor(consumeGroupName);
    }
}
