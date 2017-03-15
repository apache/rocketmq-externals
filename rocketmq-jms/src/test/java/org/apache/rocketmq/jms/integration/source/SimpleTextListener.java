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

package org.apache.rocketmq.jms.integration.source;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class SimpleTextListener {

    public static final String DESTINATION = "orderTest";

    @Autowired
    private RocketMQAdmin rocketMQAdmin;

    private List<String> receivedMsgs = new ArrayList();

    public SimpleTextListener() {
    }

    @PostConstruct
    public void init() {
        this.rocketMQAdmin.createTopic(DESTINATION);
    }

    @PreDestroy
    public void destroy() {
        this.rocketMQAdmin.deleteTopic(DESTINATION);
    }

    @JmsListener(destination = DESTINATION)
    public void processOrder(Message<String> message) {
        receivedMsgs.add(message.getPayload());
    }

    public List<String> getReceivedMsg() {
        return receivedMsgs;
    }
}
