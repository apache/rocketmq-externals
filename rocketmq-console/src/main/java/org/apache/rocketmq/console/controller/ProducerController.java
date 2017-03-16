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
import org.apache.rocketmq.common.protocol.body.ProducerConnection;
import org.apache.rocketmq.console.model.ConnectionInfo;
import org.apache.rocketmq.console.service.ProducerService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/producer")
public class ProducerController {

    @Resource
    private ProducerService producerService;

    @RequestMapping(value = "/producerConnection.query", method = {RequestMethod.GET})
    @ResponseBody
    public Object producerConnection(@RequestParam String producerGroup, @RequestParam String topic) {
        ProducerConnection producerConnection = producerService.getProducerConnection(producerGroup, topic);
        producerConnection.setConnectionSet(ConnectionInfo.buildConnectionInfoHashSet(producerConnection.getConnectionSet()));
        return producerConnection;
    }
}
