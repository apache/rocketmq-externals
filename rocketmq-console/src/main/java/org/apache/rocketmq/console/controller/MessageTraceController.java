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

package org.apache.rocketmq.console.controller;

import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import javax.annotation.Resource;

import org.apache.rocketmq.common.Pair;
import org.apache.rocketmq.console.model.MessageView;
import org.apache.rocketmq.console.model.trace.MessageTraceGraph;
import org.apache.rocketmq.console.service.MessageService;
import org.apache.rocketmq.console.service.MessageTraceService;
import org.apache.rocketmq.tools.admin.api.MessageTrack;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/messageTrace")
public class MessageTraceController {

    @Resource
    private MessageService messageService;

    @Resource
    private MessageTraceService messageTraceService;

    @RequestMapping(value = "/viewMessage.query", method = RequestMethod.GET)
    @ResponseBody
    public Object viewMessage(@RequestParam(required = false) String topic, @RequestParam String msgId) {
        Map<String, Object> messageViewMap = Maps.newHashMap();
        Pair<MessageView, List<MessageTrack>> messageViewListPair = messageService.viewMessage(topic, msgId);
        messageViewMap.put("messageView", messageViewListPair.getObject1());
        return messageViewMap;
    }

    @RequestMapping(value = "/viewMessageTraceDetail.query", method = RequestMethod.GET)
    @ResponseBody
    public Object viewTraceMessages(@RequestParam String msgId) {
        return messageTraceService.queryMessageTraceKey(msgId);
    }

    @RequestMapping(value = "/viewMessageTraceGraph.query", method = RequestMethod.GET)
    @ResponseBody
    public MessageTraceGraph viewMessageTraceGraph(@RequestParam String msgId) {
        return messageTraceService.queryMessageTraceGraph(msgId);
    }
}
