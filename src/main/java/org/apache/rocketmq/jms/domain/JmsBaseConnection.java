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

import com.google.common.base.Preconditions;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.Topic;
import org.apache.commons.lang.StringUtils;

public class JmsBaseConnection implements Connection {
    private final AtomicBoolean started = new AtomicBoolean(false);
    protected String clientID;
    protected ExceptionListener exceptionListener;
    protected CommonContext context;
    protected JmsBaseSession session;

    public JmsBaseConnection(Map<String, String> connectionParams) {

        this.clientID = UUID.randomUUID().toString();

        context = new CommonContext();

        //At lease one should be set
        context.setProducerId(connectionParams.get(CommonConstant.PRODUCERID));
        context.setConsumerId(connectionParams.get(CommonConstant.CONSUMERID));

        //optional
        context.setProvider(connectionParams.get(CommonConstant.PROVIDER));

        String nameServer = connectionParams.get(CommonConstant.NAMESERVER);
        String consumerThreadNums = connectionParams.get(CommonConstant.CONSUME_THREAD_NUMS);
        String sendMsgTimeoutMillis = connectionParams.get(CommonConstant.SEND_TIMEOUT_MILLIS);
        String instanceName = connectionParams.get(CommonConstant.INSTANCE_NAME);

        if (StringUtils.isNotEmpty(nameServer)) {
            context.setNameServer(nameServer);
        }
        if (StringUtils.isNotEmpty(instanceName)) {
            context.setInstanceName(connectionParams.get(CommonConstant.INSTANCE_NAME));
        }

        if (StringUtils.isNotEmpty(consumerThreadNums)) {
            context.setConsumeThreadNums(Integer.parseInt(consumerThreadNums));
        }
        if (StringUtils.isNotEmpty(sendMsgTimeoutMillis)) {
            context.setSendMsgTimeoutMillis(Integer.parseInt(sendMsgTimeoutMillis));
        }
    }

    @Override
    public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {

        Preconditions.checkArgument(!transacted, "Not support transaction Session !");
        Preconditions.checkArgument(Session.AUTO_ACKNOWLEDGE == acknowledgeMode,
            "Not support this acknowledge mode: " + acknowledgeMode);

        if (null != this.session) {
            return this.session;
        }
        synchronized (this) {
            if (null != this.session) {
                return this.session;
            }
            this.session = new JmsBaseSession(this, transacted, acknowledgeMode, context);
            if (isStarted()) {
                this.session.start();
            }
            return this.session;
        }
    }

    @Override
    public String getClientID() throws JMSException {
        return this.clientID;
    }

    @Override
    public void setClientID(String clientID) throws JMSException {
        this.clientID = clientID;
    }

    @Override
    public ConnectionMetaData getMetaData() throws JMSException {
        return new JmsBaseConnectionMetaData();
    }

    @Override
    public ExceptionListener getExceptionListener() throws JMSException {
        return this.exceptionListener;
    }

    @Override
    public void setExceptionListener(ExceptionListener listener) throws JMSException {
        this.exceptionListener = listener;
    }

    @Override
    public void start() throws JMSException {
        if (started.compareAndSet(false, true)) {
            if (this.session != null) {
                this.session.start();
            }

        }
    }

    @Override
    public void stop() throws JMSException {
        //Stop the connection before closing it.
        //Do nothing here.
    }

    @Override
    public void close() throws JMSException {
        if (started.compareAndSet(true, false)) {
            if (this.session != null) {
                this.session.close();
            }

        }
    }

    @Override
    public ConnectionConsumer createConnectionConsumer(Destination destination,
        String messageSelector,
        ServerSessionPool sessionPool,
        int maxMessages) throws JMSException {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String subscriptionName,
        String messageSelector,
        ServerSessionPool sessionPool,
        int maxMessages) throws JMSException {
        throw new UnsupportedOperationException("Unsupported");
    }

    /**
     * Whether the connection is started.
     *
     * @return whether the connection is started.
     */
    public boolean isStarted() {
        return started.get();
    }
}
