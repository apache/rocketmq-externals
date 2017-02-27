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
import java.net.URI;
import java.util.Map;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import org.apache.rocketmq.jms.util.URISpecParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmsBaseConnectionFactory implements ConnectionFactory {

    private static Logger logger = LoggerFactory
        .getLogger(JmsBaseConnectionFactory.class);
    /**
     * Synchronization monitor for the shared Connection
     */
    private final Object connectionMonitor = new Object();
    /**
     * Can be configured in a consistent way without too much URL hacking.
     */
    protected URI connectionUri;
    /**
     * Store connection uri query parameters.
     */
    protected Map<String, String> connectionParams;
    /**
     * Wrapped Connection
     */
    protected JmsBaseConnection connection;

    public JmsBaseConnectionFactory() {

    }

    public JmsBaseConnectionFactory(URI connectionUri) {
        setConnectionUri(connectionUri);
    }

    public void setConnectionUri(URI connectionUri) {
        Preconditions.checkNotNull(connectionUri, "Please set URI !");
        this.connectionUri = connectionUri;
        this.connectionParams = URISpecParser.parseURI(connectionUri.toString());

        if (null != connectionParams) {
            Preconditions.checkState(null != connectionParams.get(CommonConstant.CONSUMERID) ||
                null != connectionParams.get(CommonConstant.PRODUCERID), "Please set consumerId or ProducerId !");
        }

    }

    @Override
    public Connection createConnection() throws JMSException {
        synchronized (this.connectionMonitor) {
            if (this.connection == null) {
                initConnection();
            }
            return this.connection;
        }
    }

    /**
     * Using userName and Password to create a connection
     *
     * @param userName ignored
     * @param password ignored
     * @return the new JMS Connection
     * @throws JMSException
     */
    @Override
    public Connection createConnection(String userName, String password) throws JMSException {
        logger.debug("Using userName and Password to create a connection.");
        return this.createConnection();
    }

    /**
     * Initialize the underlying shared Connection.
     * <p/>
     * Closes and reInitializes the Connection if an underlying Connection is present already.
     *
     * @throws javax.jms.JMSException if thrown by JMS API methods
     */
    protected void initConnection() throws JMSException {
        synchronized (this.connectionMonitor) {
            if (this.connection != null) {
                closeConnection(this.connection);
            }
            this.connection = doCreateConnection();
            logger.debug("Established shared JMS Connection: {}", this.connection);
        }
    }

    /**
     * Close the given Connection.
     *
     * @param con the Connection to close
     */
    protected void closeConnection(Connection con) {
        logger.debug("Closing shared JMS Connection: {}", this.connection);
        try {
            try {
                con.stop();
            }
            finally {
                con.close();
            }
        }
        catch (Throwable ex) {
            logger.error("Could not close shared JMS Connection.", ex);
        }
    }

    /**
     * Create a JMS Connection
     *
     * @return the new JMS Connection
     * @throws javax.jms.JMSException if thrown by JMS API methods
     */
    protected JmsBaseConnection doCreateConnection() throws JMSException {
        Preconditions.checkState(null != this.connectionParams && this.connectionParams.size() > 0,
            "Connection Parameters can not be null!");
        this.connection = new JmsBaseConnection(this.connectionParams);

        return connection;
    }

}
