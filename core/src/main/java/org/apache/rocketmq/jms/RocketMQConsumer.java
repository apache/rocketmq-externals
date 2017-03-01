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

package org.apache.rocketmq.jms;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import org.apache.rocketmq.jms.support.JMSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocketMQConsumer implements MessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(RocketMQConsumer.class);
    private RocketMQSession session;
    private Destination destination;
    private String messageSelector;
    private MessageListener messageListener;
    private String subscriptionName;
    private boolean durable;
    private boolean shared;

    private DeliverMessageService deliverMessageService;

    public RocketMQConsumer(RocketMQSession session, Destination destination,
        String messageSelector,
        boolean durable, boolean shared) {
        this(session, destination, messageSelector, UUID.randomUUID().toString(), durable, shared);
    }

    public RocketMQConsumer(RocketMQSession session, Destination destination,
        String messageSelector,
        String subscriptionName, boolean durable, boolean shared) {
        this.session = session;
        this.destination = destination;
        this.messageSelector = messageSelector;
        this.subscriptionName = subscriptionName;
        this.durable = durable;
        this.shared = shared;

        String consumerGroup = JMSUtils.getConsumerGroup(this);
        this.deliverMessageService = new DeliverMessageService(this, this.destination, consumerGroup,
            this.messageSelector, this.durable, this.shared);
        this.deliverMessageService.start();
    }

    @Override
    public String getMessageSelector() throws JMSException {
        return messageSelector;
    }

    @Override
    public MessageListener getMessageListener() throws JMSException {
        return this.messageListener;
    }

    @Override
    public void setMessageListener(MessageListener listener) throws JMSException {
        if (this.session.isSyncModel()) {
            throw new JMSException("A asynchronously call is not permitted when a session is being used synchronously");
        }

        this.messageListener = listener;
        this.deliverMessageService.setConsumeModel(ConsumeModel.ASYNC);
        this.session.addAsyncConsumer(this);
    }

    @Override
    public Message receive() throws JMSException {
        return this.receive(0);
    }

    @Override
    public Message receive(long timeout) throws JMSException {
        if (this.session.isAsyncModel()) {
            throw new JMSException("A synchronous call is not permitted when a session is being used asynchronously.");
        }

        this.session.addSyncConsumer(this);

        if (timeout == 0) {
            MessageWrapper wrapper = this.deliverMessageService.poll();
            wrapper.getConsumer().getDeliverMessageService().ack(wrapper.getMq(), wrapper.getOffset());
            return wrapper.getMessage();
        }
        else {
            MessageWrapper wrapper = this.deliverMessageService.poll(timeout, TimeUnit.MILLISECONDS);
            if (wrapper == null) {
                return null;
            }
            wrapper.getConsumer().getDeliverMessageService().ack(wrapper.getMq(), wrapper.getOffset());
            return wrapper.getMessage();
        }
    }

    @Override
    public Message receiveNoWait() throws JMSException {
        return receive(1);
    }

    @Override
    public void close() throws JMSException {
        this.deliverMessageService.close();
    }

    public void start() {
        this.deliverMessageService.recover();
    }

    public void stop() {
        this.deliverMessageService.pause();
    }

    public DeliverMessageService getDeliverMessageService() {
        return deliverMessageService;
    }

    public RocketMQSession getSession() {
        return session;
    }

    public String getSubscriptionName() {
        return subscriptionName;
    }

    public boolean isDurable() {
        return durable;
    }

    public boolean isShared() {
        return shared;
    }
}
