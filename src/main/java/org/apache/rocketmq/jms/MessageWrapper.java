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

package org.apache.rocketmq.jms;

import javax.jms.Message;
import org.apache.rocketmq.common.message.MessageQueue;

public class MessageWrapper {

    private Message message;
    private RocketMQConsumer consumer;
    private MessageQueue mq;
    private long offset;

    public MessageWrapper(Message message, RocketMQConsumer consumer, MessageQueue mq, long offset) {
        this.message = message;
        this.consumer = consumer;
        this.mq = mq;
        this.offset = offset;
    }

    public Message getMessage() {
        return message;
    }

    public RocketMQConsumer getConsumer() {
        return consumer;
    }

    public MessageQueue getMq() {
        return mq;
    }

    public long getOffset() {
        return offset;
    }
}
