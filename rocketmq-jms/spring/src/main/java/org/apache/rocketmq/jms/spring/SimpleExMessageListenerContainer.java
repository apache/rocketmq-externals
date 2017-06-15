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

package org.apache.rocketmq.jms.spring;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import org.springframework.jms.listener.SimpleMessageListenerContainer;


public class SimpleExMessageListenerContainer extends SimpleMessageListenerContainer {

    private String cacheLevelName;

    /**
     * Create a MessageConsumer for the given JMS Session, registering a
     * MessageListener for the specified listener.
     *
     * @param session
     *         the JMS Session to work on
     *
     * @return the MessageConsumer
     *
     * @throws JMSException
     *         if thrown by JMS methods
     * @see #executeListener
     */
    protected MessageConsumer createListenerConsumer(final Session session) throws JMSException {
        Destination destination = getDestination();
        if (destination == null) {
            destination = resolveDestinationName(session, getDestinationName());
        }
        MessageConsumer consumer = createConsumer(session, destination);
        consumer.setMessageListener((MessageListener) super.getMessageListener());
        return consumer;
    }

    /**
     * Create a JMS MessageConsumer for the given Session and Destination.
     * <p>
     * This implementation uses JMS 1.1 API.
     *
     * @param session
     *         the JMS Session to create a MessageConsumer for
     * @param destination
     *         the JMS Destination to create a MessageConsumer for
     *
     * @return the new JMS MessageConsumer
     *
     * @throws JMSException
     *         if thrown by JMS API methods
     */
    protected MessageConsumer createConsumer(Session session, Destination destination)
            throws JMSException {
        //ONS not support message selector and other features nowadays
        return session.createConsumer(destination);
    }

    /**
     * @return the cacheLevelName
     */
    public String getCacheLevelName() {
        return cacheLevelName;
    }

    /**
     * @param cacheLevelName
     *         the cacheLevelName to set
     */
    public void setCacheLevelName(String cacheLevelName) {
        this.cacheLevelName = cacheLevelName;
    }
}
