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

import java.util.Enumeration;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.jms.ConnectionMetaData;
import javax.jms.JMSException;
import org.apache.rocketmq.jms.msg.enums.JMSPropertiesEnum;
import org.apache.rocketmq.jms.support.ProviderVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocketMQConnectionMetaData implements ConnectionMetaData {

    private static final Logger log = LoggerFactory.getLogger(RocketMQConnectionMetaData.class);
    private static final String PROVIDER_NAME = "Apache RocketMQ";

    private String jmsVersion;
    private int jmsMajorVersion;
    private int jmsMinorVersion;

    private String providerVersion;
    private int providerMajorVersion;
    private int providerMinorVersion;

    private static RocketMQConnectionMetaData metaData = new RocketMQConnectionMetaData();

    private RocketMQConnectionMetaData() {
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
            log.error("Error during getting jms version", e);
        }

        this.jmsVersion = jmsVersion;
        this.jmsMajorVersion = jmsMajor;
        this.jmsMinorVersion = jmsMinor;

        this.providerVersion = ProviderVersion.CURRENT_VERSION.name();
        this.providerMinorVersion = ProviderVersion.CURRENT_VERSION.getValue();
        this.providerMajorVersion = ProviderVersion.CURRENT_VERSION.getValue();
    }

    public static RocketMQConnectionMetaData instance() {
        return metaData;
    }

    public String getJMSVersion() throws JMSException {
        return jmsVersion;
    }

    public int getJMSMajorVersion() throws JMSException {
        return jmsMajorVersion;
    }

    public int getJMSMinorVersion() throws JMSException {
        return jmsMinorVersion;
    }

    public String getJMSProviderName() throws JMSException {
        return PROVIDER_NAME;
    }

    public String getProviderVersion() throws JMSException {
        return providerVersion;
    }

    public int getProviderMajorVersion() throws JMSException {
        return providerMajorVersion;
    }

    public int getProviderMinorVersion() throws JMSException {
        return providerMinorVersion;
    }

    public Enumeration<?> getJMSXPropertyNames() throws JMSException {
        Vector<String> result = new Vector<String>();
        for (JMSPropertiesEnum e : JMSPropertiesEnum.values()) {
            result.add(e.name());
        }
        return result.elements();
    }

}
