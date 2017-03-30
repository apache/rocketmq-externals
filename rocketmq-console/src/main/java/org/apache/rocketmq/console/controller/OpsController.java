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
import org.apache.rocketmq.console.service.OpsService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/ops")
public class OpsController {

    @Resource
    private OpsService opsService;

    @RequestMapping(value = "/homePage.query", method = RequestMethod.GET)
    @ResponseBody
    public Object homePage() {
        return opsService.homePageInfo();
    }

    @RequestMapping(value = "/updateNameSvrAddr.do", method = RequestMethod.POST)
    @ResponseBody
    public Object updateNameSvrAddr(@RequestParam String nameSvrAddrList) {
        opsService.updateNameSvrAddrList(nameSvrAddrList);
        return true;
    }

    @RequestMapping(value = "/updateIsVIPChannel.do", method = RequestMethod.POST)
    @ResponseBody
    public Object updateIsVIPChannel(@RequestParam String useVIPChannel) {
        opsService.updateIsVIPChannel(useVIPChannel);
        return true;
    }


    @RequestMapping(value = "/rocketMqStatus.query", method = RequestMethod.GET)
    @ResponseBody
    public Object clusterStatus() {
        return opsService.rocketMqStatusCheck();
    }
}
