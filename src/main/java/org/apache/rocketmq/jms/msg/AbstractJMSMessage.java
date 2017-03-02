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

package org.apache.rocketmq.jms.msg;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageNotWriteableException;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum;
import org.apache.rocketmq.jms.support.JMSUtils;

import static org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum.JMSCorrelationID;
import static org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum.JMSDeliveryMode;
import static org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum.JMSDeliveryTime;
import static org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum.JMSDestination;
import static org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum.JMSExpiration;
import static org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum.JMSMessageID;
import static org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum.JMSPriority;
import static org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum.JMSRedelivered;
import static org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum.JMSReplyTo;
import static org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum.JMSTimestamp;
import static org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum.JMSType;
import static org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum.JMS_DELIVERY_MODE_DEFAULT_VALUE;
import static org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum.JMS_DELIVERY_TIME_DEFAULT_VALUE;
import static org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum.JMS_EXPIRATION_DEFAULT_VALUE;
import static org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum.JMS_PRIORITY_DEFAULT_VALUE;
import static org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum.JMS_REDELIVERED_DEFAULT_VALUE;
import static org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum.JMS_TIMESTAMP_DEFAULT_VALUE;
import static org.apache.rocketmq.jms.support.ObjectTypeCast.cast2Boolean;
import static org.apache.rocketmq.jms.support.ObjectTypeCast.cast2Integer;
import static org.apache.rocketmq.jms.support.ObjectTypeCast.cast2Long;
import static org.apache.rocketmq.jms.support.ObjectTypeCast.cast2Object;
import static org.apache.rocketmq.jms.support.ObjectTypeCast.cast2String;

public abstract class AbstractJMSMessage implements javax.jms.Message {

    protected Map<JMSHeaderEnum, Object> headers = new HashMap();
    protected Map<String, Object> properties = new HashMap();

    protected boolean writeOnly;

    @Override
    public String getJMSMessageID() {
        return cast2String(headers.get(JMSMessageID));
    }

    @Override
    public void setJMSMessageID(String id) {
        setHeader(JMSMessageID, id);
    }

    @Override
    public long getJMSTimestamp() {
        if (headers.containsKey(JMSTimestamp)) {
            return cast2Long(headers.get(JMSTimestamp));
        }
        return JMS_TIMESTAMP_DEFAULT_VALUE;
    }

    @Override
    public void setJMSTimestamp(long timestamp) {
        setHeader(JMSTimestamp, timestamp);
    }

    @Override
    public byte[] getJMSCorrelationIDAsBytes() {
        String jmsCorrelationID = getJMSCorrelationID();
        if (jmsCorrelationID != null) {
            try {
                return JMSUtils.string2Bytes(jmsCorrelationID);
            }
            catch (Exception e) {
                return jmsCorrelationID.getBytes();
            }
        }
        return null;
    }

    @Override
    public void setJMSCorrelationIDAsBytes(byte[] correlationID) {
        String encodedText = JMSUtils.bytes2String(correlationID);
        setJMSCorrelationID(encodedText);
    }

    @Override
    public String getJMSCorrelationID() {
        return cast2String(headers.get(JMSCorrelationID));
    }

    @Override
    public void setJMSCorrelationID(String correlationID) {
        setHeader(JMSCorrelationID, correlationID);
    }

    @Override
    public Destination getJMSReplyTo() {
        return cast2Object(headers.get(JMSReplyTo), Destination.class);
    }

    @Override
    public void setJMSReplyTo(Destination replyTo) {
        setHeader(JMSReplyTo, replyTo);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public Destination getJMSDestination() {
        return cast2Object(headers.get(JMSDestination), Destination.class);
    }

    @Override
    public void setJMSDestination(Destination destination) {
        setHeader(JMSDestination, destination);
    }

    @SuppressWarnings("unchecked")
    public abstract <T> T getBody(Class<T> clazz) throws JMSException;

    public abstract byte[] getBody() throws JMSException;

    @Override
    public int getJMSDeliveryMode() {
        if (headers.containsKey(JMSDeliveryMode)) {
            return cast2Integer(headers.get(JMSDeliveryMode));
        }
        return JMS_DELIVERY_MODE_DEFAULT_VALUE;
    }

    @Override
    public void setJMSDeliveryMode(int deliveryMode) {
        setHeader(JMSDeliveryMode, deliveryMode);
    }

    @Override
    public boolean getJMSRedelivered() {
        if (headers.containsKey(JMSRedelivered)) {
            return cast2Boolean(headers.get(JMSRedelivered));
        }
        return JMS_REDELIVERED_DEFAULT_VALUE;
    }

    @Override
    public void setJMSRedelivered(boolean redelivered) {
        setHeader(JMSRedelivered, redelivered);
    }

    @Override
    public String getJMSType() {
        return cast2String(headers.get(JMSType));
    }

    @Override
    public void setJMSType(String type) {
        setHeader(JMSType, type);
    }

    public Map<JMSHeaderEnum, Object> getHeaders() {
        return this.headers;
    }

    @Override
    public long getJMSExpiration() {
        if (headers.containsKey(JMSExpiration)) {
            return cast2Long(headers.get(JMSExpiration));
        }
        return JMS_EXPIRATION_DEFAULT_VALUE;
    }

    @Override
    public void setJMSExpiration(long expiration) {
        setHeader(JMSExpiration, expiration);
    }

    @Override
    public int getJMSPriority() {
        if (headers.containsKey(JMSPriority)) {
            return cast2Integer(headers.get(JMSPriority));
        }
        return JMS_PRIORITY_DEFAULT_VALUE;
    }

    @Override
    public void setJMSPriority(int priority) {
        setHeader(JMSPriority, priority);
    }

    @Override
    public long getJMSDeliveryTime() throws JMSException {
        if (headers.containsKey(JMSDeliveryTime)) {
            return cast2Long(headers.get(JMSDeliveryTime));
        }
        return JMS_DELIVERY_TIME_DEFAULT_VALUE;
    }

    @Override
    public void setJMSDeliveryTime(long deliveryTime) throws JMSException {
        setHeader(JMSDeliveryTime, deliveryTime);
    }

    private void setHeader(JMSHeaderEnum name, Object value) {
        this.headers.put(name, value);
    }

    public Map<String, Object> getProperties() {
        return this.properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public void acknowledge() throws JMSException {
        //todo
        throw new UnsupportedOperationException("Unsupported!");
    }

    @Override
    public void clearProperties() {
        this.properties.clear();
    }

    @Override
    public void clearBody() {
        this.writeOnly = true;
    }

    @Override
    public boolean propertyExists(String name) {
        return properties.containsKey(name);
    }

    @Override
    public boolean getBooleanProperty(String name) throws JMSException {
        if (propertyExists(name)) {
            Object value = getObjectProperty(name);
            return Boolean.valueOf(value.toString());
        }
        return false;
    }

    @Override
    public byte getByteProperty(String name) throws JMSException {
        if (propertyExists(name)) {
            Object value = getObjectProperty(name);
            return Byte.valueOf(value.toString());
        }
        return 0;
    }

    @Override
    public short getShortProperty(String name) throws JMSException {
        if (propertyExists(name)) {
            Object value = getObjectProperty(name);
            return Short.valueOf(value.toString());
        }
        return 0;
    }

    @Override
    public int getIntProperty(String name) throws JMSException {
        if (propertyExists(name)) {
            Object value = getObjectProperty(name);
            return Integer.valueOf(value.toString());
        }
        return 0;
    }

    @Override
    public long getLongProperty(String name) throws JMSException {
        if (propertyExists(name)) {
            Object value = getObjectProperty(name);
            return Long.valueOf(value.toString());
        }
        return 0L;
    }

    @Override
    public float getFloatProperty(String name) throws JMSException {
        if (propertyExists(name)) {
            Object value = getObjectProperty(name);
            return Float.valueOf(value.toString());
        }
        return 0f;
    }

    @Override
    public double getDoubleProperty(String name) throws JMSException {
        if (propertyExists(name)) {
            Object value = getObjectProperty(name);
            return Double.valueOf(value.toString());
        }
        return 0d;
    }

    @Override
    public String getStringProperty(String name) throws JMSException {
        if (propertyExists(name)) {
            return getObjectProperty(name).toString();
        }
        return null;
    }

    @Override
    public Object getObjectProperty(String name) throws JMSException {
        return this.properties.get(name);
    }

    @Override
    public Enumeration<?> getPropertyNames() throws JMSException {
        return Collections.enumeration(this.properties.keySet());
    }

    @Override
    public void setBooleanProperty(String name, boolean value) {
        setObjectProperty(name, value);
    }

    @Override
    public void setByteProperty(String name, byte value) {
        setObjectProperty(name, value);
    }

    @Override
    public void setShortProperty(String name, short value) {
        setObjectProperty(name, value);
    }

    @Override
    public void setIntProperty(String name, int value) {
        setObjectProperty(name, value);
    }

    @Override
    public void setLongProperty(String name, long value) {
        setObjectProperty(name, value);
    }

    public void setFloatProperty(String name, float value) {
        setObjectProperty(name, value);
    }

    @Override
    public void setDoubleProperty(String name, double value) {
        setObjectProperty(name, value);
    }

    @Override
    public void setStringProperty(String name, String value) {
        setObjectProperty(name, value);
    }

    @Override
    public abstract boolean isBodyAssignableTo(Class c) throws JMSException;

    @Override
    public void setObjectProperty(String name, Object value) {
        if (value instanceof Number || value instanceof String || value instanceof Boolean) {
            this.properties.put(name, value);
        }
        else {
            throw new IllegalArgumentException(
                "Value should be boolean, byte, short, int, long, float, double, and String.");
        }
    }

    protected boolean isWriteOnly() {
        return writeOnly;
    }

    protected void checkIsWriteOnly() throws MessageNotWriteableException {
        if (!writeOnly) {
            throw new MessageNotWriteableException("Not writable");
        }
    }

}
