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

package org.apache.rocketmq.jms.hook;

import java.util.UUID;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import org.apache.rocketmq.jms.RocketMQProducer;
import org.apache.rocketmq.jms.exception.UnsupportDeliveryModelException;
import org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum;

import static org.apache.rocketmq.jms.Constant.MESSAGE_ID_PREFIX;

/**
 * Hook that executes before sending message.
 */
public class SendMessageHook {

    private RocketMQProducer producer;

    public SendMessageHook() {
    }

    public SendMessageHook(RocketMQProducer producer) {
        this.producer = producer;
    }

    public void before(Message message, Destination destination, int deliveryMode, int priority,
        long timeToLive) throws JMSException {

        validate(deliveryMode);

        setHeader(message, destination, deliveryMode, priority, timeToLive);

    }

    private void setHeader(Message message, Destination destination, int deliveryMode, int priority,
        long timeToLive) throws JMSException {
        // destination
        message.setJMSDestination(destination);

        // delivery mode
        message.setJMSDeliveryMode(deliveryMode);

        // expiration
        if (timeToLive != 0) {
            message.setJMSExpiration(System.currentTimeMillis() + timeToLive);
        }
        else {
            message.setJMSExpiration(0L);
        }

        // delivery time
        message.setJMSDeliveryTime(message.getJMSTimestamp() + this.producer.getDeliveryDelay());

        // priority
        message.setJMSPriority(priority);

        // messageID is also required in async model, so {@link MessageExt#getMsgId()} can't be used.
        if (!this.producer.getDisableMessageID()) {
            message.setJMSMessageID(new StringBuffer(MESSAGE_ID_PREFIX).append(UUID.randomUUID().getLeastSignificantBits()).toString());
        }

        // timestamp
        if (!this.producer.getDisableMessageTimestamp()) {
            message.setJMSTimestamp(System.currentTimeMillis());
        }
    }

    private void validate(int deliveryMode) {
        if (deliveryMode != JMSHeaderEnum.JMS_DELIVERY_MODE_DEFAULT_VALUE) {
            throw new UnsupportDeliveryModelException();
        }
    }

    public void setProducer(RocketMQProducer producer) {
        this.producer = producer;
    }
}
