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

package org.apache.rocketmq.jms.domain;

import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import java.util.List;
import org.apache.rocketmq.jms.util.MessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmsBaseMessageListener implements MessageListenerConcurrently {
    private final Logger logger = LoggerFactory.getLogger(JmsBaseMessageListener.class);
    private javax.jms.MessageListener listener;

    public JmsBaseMessageListener(javax.jms.MessageListener listener) {
        this.listener = listener;
    }

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> exts,
        ConsumeConcurrentlyContext concurrentlyContext) {
        for (MessageExt ext : exts) {
            try {
                listener.onMessage(MessageConverter.convert2JMSMessage(ext));
            }
            catch (Exception e) {
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        }
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }
}
