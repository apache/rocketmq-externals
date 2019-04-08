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

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.console.model.MessageTraceView;
import org.apache.rocketmq.console.service.MessageTraceService;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MessageTraceServiceImpl implements MessageTraceService {

    private Logger logger = LoggerFactory.getLogger(MessageTraceServiceImpl.class);

    private final static int QUERY_MESSAGE_MAX_NUM = 64;
    @Resource
    private MQAdminExt mqAdminExt;
    
    @Override public List<MessageTraceView> queryMessageTraceByTopicAndKey(String topic, String key) {
        try {
            List<MessageTraceView> messageTraceViews = new ArrayList<MessageTraceView>();
            List<MessageExt> messageTraceList = mqAdminExt.queryMessage(topic, key, QUERY_MESSAGE_MAX_NUM, 0, System.currentTimeMillis()).getMessageList();
            for (MessageExt messageExt : messageTraceList) {
                List<MessageTraceView> messageTraceView = MessageTraceView.decodeFromTraceTransData(key, new String(messageExt.getBody(), Charsets.UTF_8));
                messageTraceViews.addAll(messageTraceView);
            }
            return messageTraceViews;
        }
        catch (Exception err) {
            throw Throwables.propagate(err);
        }
    }
}
