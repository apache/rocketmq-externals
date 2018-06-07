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

package org.apache.rocketmq.jms.domain.message;

import com.google.common.collect.Maps;
import com.google.common.io.BaseEncoding;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Map;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.rocketmq.jms.domain.JmsBaseConstant;
import org.apache.rocketmq.jms.util.ExceptionUtil;

public class JmsBaseMessage implements Message {
    /**
     * Message properties
     */
    protected Map<String, Object> properties = Maps.newHashMap();
    /**
     * Message headers
     */
    protected Map<String, Object> headers = Maps.newHashMap();
    /**
     * Message body
     */
    protected Serializable body;

    @Override
    public String getJMSMessageID() {
        return (String) headers.get(JmsBaseConstant.JMS_MESSAGE_ID);
    }

    /**
     * Sets the message ID.
     * <p/>
     * <P>JMS providers set this field when a message is sent. Do not allow User to set the message ID by yourself.
     *
     * @param id the ID of the message
     * @see javax.jms.Message#getJMSMessageID()
     */

    @Override
    public void setJMSMessageID(String id) {
        ExceptionUtil.handleUnSupportedException();
    }

    @Override
    public long getJMSTimestamp() {
        if (headers.containsKey(JmsBaseConstant.JMS_TIMESTAMP)) {
            return (Long) headers.get(JmsBaseConstant.JMS_TIMESTAMP);
        }
        return 0;
    }

    @Override
    public void setJMSTimestamp(long timestamp) {
        ExceptionUtil.handleUnSupportedException();
    }

    @Override
    public byte[] getJMSCorrelationIDAsBytes() {
        String jmsCorrelationID = getJMSCorrelationID();
        if (jmsCorrelationID != null) {
            try {
                return BaseEncoding.base64().decode(jmsCorrelationID);
            }
            catch (Exception e) {
                return jmsCorrelationID.getBytes();
            }
        }
        return null;
    }

    @Override
    public void setJMSCorrelationIDAsBytes(byte[] correlationID) {
        String encodedText = BaseEncoding.base64().encode(correlationID);
        setJMSCorrelationID(encodedText);
    }

    @Override
    public String getJMSCorrelationID() {
        if (headers.containsKey(JmsBaseConstant.JMS_CORRELATION_ID)) {
            return (String) headers.get(JmsBaseConstant.JMS_CORRELATION_ID);
        }
        return null;
    }

    @Override
    public void setJMSCorrelationID(String correlationID) {
        ExceptionUtil.handleUnSupportedException();
    }

    @Override
    public Destination getJMSReplyTo() {
        if (headers.containsKey(JmsBaseConstant.JMS_REPLY_TO)) {
            return (Destination) headers.get(JmsBaseConstant.JMS_REPLY_TO);
        }
        return null;
    }

    @Override
    public void setJMSReplyTo(Destination replyTo) {
        ExceptionUtil.handleUnSupportedException();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public Destination getJMSDestination() {
        if (headers.containsKey(JmsBaseConstant.JMS_DESTINATION)) {
            return (Destination) headers.get(JmsBaseConstant.JMS_DESTINATION);
        }
        return null;
    }

    @Override
    public void setJMSDestination(Destination destination) {
        ExceptionUtil.handleUnSupportedException();
    }

    @SuppressWarnings("unchecked")
    public <T> T getBody(Class<T> clazz) throws JMSException {
        if (clazz.isInstance(body)) {
            return (T) body;
        }
        else {
            throw new IllegalArgumentException("The class " + clazz
                + " is unknown to this implementation");
        }
    }

    @Override
    public int getJMSDeliveryMode() {
        if (headers.containsKey(JmsBaseConstant.JMS_DELIVERY_MODE)) {
            return (Integer) headers.get(JmsBaseConstant.JMS_DELIVERY_MODE);
        }
        return 0;
    }

    /**
     * Sets the <CODE>DeliveryMode</CODE> value for this message.
     * <p/>
     * <P>JMS providers set this field when a message is sent. ROCKETMQ only support DeliveryMode.PERSISTENT mode. So do not
     * allow User to set this by yourself, but you can get the default mode by <CODE>getJMSDeliveryMode</CODE> method.
     *
     * @param deliveryMode the delivery mode for this message
     * @see javax.jms.Message#getJMSDeliveryMode()
     * @see javax.jms.DeliveryMode
     */

    @Override
    public void setJMSDeliveryMode(int deliveryMode) {
        ExceptionUtil.handleUnSupportedException();
    }

    public boolean isBodyAssignableTo(Class<?> clazz) throws JMSException {
        return clazz.isInstance(body);
    }

    @Override
    public boolean getJMSRedelivered() {
        return headers.containsKey(JmsBaseConstant.JMS_REDELIVERED)
            && (Boolean) headers.get(JmsBaseConstant.JMS_REDELIVERED);
    }

    @Override
    public void setJMSRedelivered(boolean redelivered) {
        ExceptionUtil.handleUnSupportedException();
    }

    /**
     * copy meta data from source message
     *
     * @param sourceMessage source message
     */
    public void copyMetaData(JmsBaseMessage sourceMessage) {
        if (!sourceMessage.getHeaders().isEmpty()) {
            for (Map.Entry<String, Object> entry : sourceMessage.getHeaders().entrySet()) {
                if (!exists(entry.getKey())) {
                    setHeader(entry.getKey(), entry.getValue());
                }
            }
        }
        if (!sourceMessage.getProperties().isEmpty()) {
            for (Map.Entry<String, Object> entry : sourceMessage.getProperties().entrySet()) {
                if (!propertyExists(entry.getKey())) {
                    setObjectProperty(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    @Override
    public String getJMSType() {
        return (String) headers.get(JmsBaseConstant.JMS_TYPE);
    }

    @Override
    public void setJMSType(String type) {
        ExceptionUtil.handleUnSupportedException();
    }

    public Map<String, Object> getHeaders() {
        return this.headers;
    }

    @Override
    public long getJMSExpiration() {
        if (headers.containsKey(JmsBaseConstant.JMS_EXPIRATION)) {
            return (Long) headers.get(JmsBaseConstant.JMS_EXPIRATION);
        }
        return 0;
    }

    @Override
    public void setJMSExpiration(long expiration) {
        ExceptionUtil.handleUnSupportedException();
    }

    public boolean exists(String name) {
        return this.headers.containsKey(name);
    }

    @Override
    public int getJMSPriority() {
        if (headers.containsKey(JmsBaseConstant.JMS_PRIORITY)) {
            return (Integer) headers.get(JmsBaseConstant.JMS_PRIORITY);
        }
        return 5;
    }

    @Override
    public void setJMSPriority(int priority) {
        ExceptionUtil.handleUnSupportedException();
    }

    public void setHeader(String name, Object value) {
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
        throw new UnsupportedOperationException("Unsupported!");
    }

    @Override
    public void clearProperties() {
        this.properties.clear();
    }

    @Override
    public void clearBody() {
        this.body = null;
    }

    @Override
    public boolean propertyExists(String name) {
        return properties.containsKey(name);
    }

    @Override
    public boolean getBooleanProperty(String name) throws JMSException {
        if (propertyExists(name)) {
            Object value = getObjectProperty(name);
            return value instanceof Boolean ? (Boolean) value : Boolean.valueOf(value.toString());
        }
        return false;
    }

    @Override
    public byte getByteProperty(String name) throws JMSException {
        if (propertyExists(name)) {
            Object value = getObjectProperty(name);
            return value instanceof Byte ? (Byte) value : Byte.valueOf(value.toString());
        }
        return 0;
    }

    @Override
    public short getShortProperty(String name) throws JMSException {
        if (propertyExists(name)) {
            Object value = getObjectProperty(name);
            return value instanceof Short ? (Short) value : Short.valueOf(value.toString());
        }
        return 0;
    }

    @Override
    public int getIntProperty(String name) throws JMSException {
        if (propertyExists(name)) {
            Object value = getObjectProperty(name);
            return value instanceof Integer ? (Integer) value : Integer.valueOf(value.toString());
        }
        return 0;
    }

    @Override
    public long getLongProperty(String name) throws JMSException {
        if (propertyExists(name)) {
            Object value = getObjectProperty(name);
            return value instanceof Long ? (Long) value : Long.valueOf(value.toString());
        }
        return 0L;
    }

    @Override
    public float getFloatProperty(String name) throws JMSException {
        if (propertyExists(name)) {
            Object value = getObjectProperty(name);
            return value instanceof Float ? (Float) value : Float.valueOf(value.toString());
        }
        return 0f;
    }

    @Override
    public double getDoubleProperty(String name) throws JMSException {
        if (propertyExists(name)) {
            Object value = getObjectProperty(name);
            return value instanceof Double ? (Double) value : Double.valueOf(value.toString());
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
        final Object[] keys = this.properties.keySet().toArray();
        return new Enumeration<Object>() {
            int i;

            @Override
            public boolean hasMoreElements() {
                return i < keys.length;
            }

            @Override
            public Object nextElement() {
                return keys[i++];
            }
        };
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
    public void setObjectProperty(String name, Object value) {
        if (value instanceof Number || value instanceof String || value instanceof Boolean) {
            this.properties.put(name, value);
        }
        else {
            throw new IllegalArgumentException(
                "Value should be boolean, byte, short, int, long, float, double, and String.");
        }
    }

}
