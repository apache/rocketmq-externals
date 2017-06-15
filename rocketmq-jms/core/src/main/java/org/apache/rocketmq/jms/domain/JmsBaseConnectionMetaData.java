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

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.jms.ConnectionMetaData;
import javax.jms.JMSException;

public class JmsBaseConnectionMetaData implements ConnectionMetaData {
    public static final String JMS_VERSION;
    public static final int JMS_MAJOR_VERSION;
    public static final int JMS_MINOR_VERSION;

    public static final String PROVIDER_VERSION;
    public static final int PROVIDER_MAJOR_VERSION;
    public static final int PROVIDER_MINOR_VERSION;

    public static final String PROVIDER_NAME = "Apache RocketMQ";

    public static final JmsBaseConnectionMetaData INSTANCE = new JmsBaseConnectionMetaData();

    public static InputStream resourceStream;

    static {
        Pattern pattern = Pattern.compile("(\\d+)\\.(\\d+).*");

        String jmsVersion = null;
        int jmsMajor = 0;
        int jmsMinor = 0;
        try {
            Package p = Package.getPackage("javax.jms");
            if (p != null) {
                jmsVersion = p.getImplementationVersion();
                Matcher m = pattern.matcher(jmsVersion);
                if (m.matches()) {
                    jmsMajor = Integer.parseInt(m.group(1));
                    jmsMinor = Integer.parseInt(m.group(2));
                }
            }
        }
        catch (Throwable e) {
        }
        JMS_VERSION = jmsVersion;
        JMS_MAJOR_VERSION = jmsMajor;
        JMS_MINOR_VERSION = jmsMinor;

        String providerVersion = null;
        int providerMajor = 0;
        int providerMinor = 0;
        Properties properties = new Properties();
        try {
            resourceStream = JmsBaseConnectionMetaData.class.getResourceAsStream("/application.conf");
            properties.load(resourceStream);
            providerVersion = properties.getProperty("version");

            Matcher m = pattern.matcher(providerVersion);
            if (m.matches()) {
                providerMajor = Integer.parseInt(m.group(1));
                providerMinor = Integer.parseInt(m.group(2));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        PROVIDER_VERSION = providerVersion;
        PROVIDER_MAJOR_VERSION = providerMajor;
        PROVIDER_MINOR_VERSION = providerMinor;

    }

    public String getJMSVersion() throws JMSException {
        return JMS_VERSION;
    }

    public int getJMSMajorVersion() throws JMSException {
        return JMS_MAJOR_VERSION;
    }

    public int getJMSMinorVersion() throws JMSException {
        return JMS_MINOR_VERSION;
    }

    public String getJMSProviderName() throws JMSException {
        return PROVIDER_NAME;
    }

    public String getProviderVersion() throws JMSException {
        return PROVIDER_VERSION;
    }

    public int getProviderMajorVersion() throws JMSException {
        return PROVIDER_MAJOR_VERSION;
    }

    public int getProviderMinorVersion() throws JMSException {
        return PROVIDER_MINOR_VERSION;
    }

    public Enumeration<?> getJMSXPropertyNames() throws JMSException {
        Vector<String> jmxProperties = new Vector<String>();
        jmxProperties.add("jmsXUserId");
        jmxProperties.add("jmsXAppId");
        jmxProperties.add("jmsXGroupID");
        jmxProperties.add("jmsXGroupSeq");
        jmxProperties.add("jmsXState");
        jmxProperties.add("jmsXDeliveryCount");
        jmxProperties.add("jmsXProducerTXID");
        jmxProperties.add("jmsConsumerTXID");
        jmxProperties.add("jmsRecvTimeStamp");
        return jmxProperties.elements();
    }

}
