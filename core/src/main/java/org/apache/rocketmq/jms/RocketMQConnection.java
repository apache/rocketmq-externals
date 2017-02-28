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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.IllegalStateException;
import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.Topic;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.MQClientManager;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static javax.jms.Session.SESSION_TRANSACTED;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.exception.ExceptionUtils.getStackTrace;

public class RocketMQConnection implements Connection {

    private static final Logger log = LoggerFactory.getLogger(RocketMQConnection.class);

    private String clientID;
    private ClientConfig clientConfig;
    private MQClientInstance clientInstance;
    private String userName;
    private String password;

    private List<RocketMQSession> sessionList = new ArrayList();
    private AtomicBoolean started = new AtomicBoolean(false);

    public RocketMQConnection(String nameServerAddress, String clientID, String instanceName, String userName,
        String password) {
        this.clientID = clientID;
        this.userName = userName;
        this.password = password;

        this.clientConfig = new ClientConfig();
        this.clientConfig.setNamesrvAddr(nameServerAddress);
        this.clientConfig.setInstanceName(instanceName);

        startClientInstance();
    }

    private void startClientInstance() {
        try {
            // create a tcp connection to broker and some other background thread
            this.clientInstance = MQClientManager.getInstance().getAndCreateMQClientInstance(this.clientConfig);
            clientInstance.start();
        }
        catch (MQClientException e) {
            throw new JMSRuntimeException(format("Fail to startClientInstance connection object[namesrvAddr:%s,instanceName:%s]. Error message:%s",
                this.clientConfig.getNamesrvAddr(), this.clientConfig.getInstanceName(), getStackTrace(e)));
        }
    }

    @Override
    public Session createSession() throws JMSException {
        return createSession(false, AUTO_ACKNOWLEDGE);
    }

    @Override
    public Session createSession(int sessionMode) throws JMSException {
        if (sessionMode == SESSION_TRANSACTED) {
            return createSession(true, Session.AUTO_ACKNOWLEDGE);
        }
        else {
            return createSession(false, sessionMode);
        }
    }

    @Override
    public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
        //todo: support transacted and more acknowledge mode
        if (transacted) {
            throw new JMSException("Not support local transaction session");
        }
        if (acknowledgeMode != AUTO_ACKNOWLEDGE) {
            throw new JMSException("Only support AUTO_ACKNOWLEDGE mode now");
        }

        RocketMQSession session = new RocketMQSession(this, acknowledgeMode, transacted);
        this.sessionList.add(session);

        return session;
    }

    @Override
    public ConnectionConsumer createConnectionConsumer(Destination destination,
        String messageSelector,
        ServerSessionPool sessionPool,
        int maxMessages) throws JMSException {
        //todo
        throw new JMSException("Not support yet");
    }

    @Override
    public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String subscriptionName,
        String messageSelector,
        ServerSessionPool sessionPool,
        int maxMessages) throws JMSException {
        //todo
        throw new JMSException("Not support yet");
    }

    @Override
    public ConnectionConsumer createSharedConnectionConsumer(Topic topic, String subscriptionName,
        String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
        //todo
        throw new JMSException("Not support yet");
    }

    @Override
    public ConnectionConsumer createSharedDurableConnectionConsumer(Topic topic, String subscriptionName,
        String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
        //todo
        throw new JMSException("Not support yet");
    }

    @Override
    public String getClientID() throws JMSException {
        return this.clientID;
    }

    @Override
    public void setClientID(String clientID) throws JMSException {
        if (isNotBlank(this.clientID)) {
            throw new IllegalStateException("administratively client identifier has been configured.");
        }
        this.clientID = clientID;
    }

    @Override
    public ConnectionMetaData getMetaData() throws JMSException {
        return RocketMQConnectionMetaData.instance();
    }

    @Override
    public ExceptionListener getExceptionListener() throws JMSException {
        //todo
        throw new JMSException("Not support yet");
    }

    @Override
    public void setExceptionListener(ExceptionListener listener) throws JMSException {
        //todo
        throw new JMSException("Not support yet");
    }

    @Override
    public void start() throws JMSException {
        if (this.started.compareAndSet(false, true)) {
            for (RocketMQSession session : sessionList) {
                for (RocketMQConsumer consumer : session.getConsumerList()) {
                    consumer.getDeliverMessageService().recover();
                }
            }
            log.debug("Start connection successfully:{}", toString());
        }
    }

    @Override
    public void stop() throws JMSException {
        if (this.started.compareAndSet(true, false)) {
            for (RocketMQSession session : sessionList) {
                for (RocketMQConsumer consumer : session.getConsumerList()) {
                    consumer.getDeliverMessageService().pause();
                }
            }
            log.debug("Stop connection successfully:{}", toString());
        }
    }

    @Override
    public void close() throws JMSException {
        log.info("Begin to close connection:{}", toString());

        for (RocketMQSession session : sessionList) {
            session.close();
        }

        this.clientInstance.shutdown();

        log.info("Success to close connection:{}", toString());
    }

    public boolean isStarted() {
        return started.get();
    }

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public String getUserName() {
        return userName;
    }

    @Override public String toString() {
        return new ToStringBuilder(this)
            .append("nameServerAddress", this.clientConfig.getNamesrvAddr())
            .append("instanceName", this.clientConfig.getInstanceName())
            .append("clientIdentifier", this.clientID)
            .toString();
    }
}
