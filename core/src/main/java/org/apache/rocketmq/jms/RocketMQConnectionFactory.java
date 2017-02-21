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

import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import org.apache.rocketmq.jms.support.JmsHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement of {@link ConnectionFactory} using RocketMQ client.
 *
 * <P>In RocketMQ, all producers and consumers interactive with broker
 * by an {@link MQClientInstance} object, which encapsulates tcp connection,
 * schedule task and so on. The best way to control the behavior of producers/consumers
 * derived from a connection is to manipulate the {@link MQClientInstance} directly.
 *
 * <P>However, this object is not easy to access as it is maintained within RocketMQ Client.
 * Fortunately another equivalent identifier called "instanceName" is provided.
 * The "instanceName" is a one-to-one conception with {@link MQClientInstance} object.
 * Just like there is a hash map,"instanceName" is the key and a {@link MQClientInstance}
 * object is the value. So the essential keyword passed through all objects created by a
 * connection is "instanceName".
 */
public class RocketMQConnectionFactory implements ConnectionFactory {

    private static final Logger log = LoggerFactory.getLogger(RocketMQConnectionFactory.class);

    private String nameServerAddress;

    private String clientId;

    public RocketMQConnectionFactory(String nameServerAddress) {
        this.nameServerAddress = nameServerAddress;
        this.clientId = JmsHelper.uuid();
    }

    public RocketMQConnectionFactory(String nameServerAddress, String clientId) {
        this.nameServerAddress = nameServerAddress;
        this.clientId = clientId;
    }

    @Override
    public Connection createConnection() throws JMSException {
        return createConnection(null, null);
    }

    /**
     * Using userName and Password to register a connection. Now access RMQ broker
     * is anonymous and any userName/password is legal.
     *
     * @param userName ignored
     * @param password ignored
     * @return the new JMS Connection
     * @throws JMSException
     */
    @Override
    public Connection createConnection(String userName, String password) throws JMSException {
        return createRocketMQConnection(userName, password);
    }

    private Connection createRocketMQConnection(String userName, String password) throws JMSException {
        final String instanceName = JmsHelper.uuid();
        RocketMQConnection connection = new RocketMQConnection(this.nameServerAddress, this.clientId, instanceName);

        log.info("Create a connection successfully[instanceName:{},clientIdentifier:{},userName:{}", instanceName, clientId, userName);
        return connection;
    }

    @Override
    public JMSContext createContext() {
        //todo:
        return null;
    }

    @Override
    public JMSContext createContext(String userName, String password) {
        //todo:
        return null;
    }

    @Override
    public JMSContext createContext(String userName, String password, int sessionMode) {
        //todo:
        return null;
    }

    @Override
    public JMSContext createContext(int sessionMode) {
        //todo:
        return null;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getNameServerAddress() {
        return nameServerAddress;
    }

    public void setNameServerAddress(String nameServerAddress) {
        this.nameServerAddress = nameServerAddress;
    }
}
