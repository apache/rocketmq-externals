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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.jms.msg;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.MessageNotWriteableException;
import org.apache.commons.lang.StringUtils;

import static org.apache.rocketmq.jms.support.PrimitiveTypeConverter.convert2Boolean;
import static org.apache.rocketmq.jms.support.PrimitiveTypeConverter.convert2Byte;
import static org.apache.rocketmq.jms.support.PrimitiveTypeConverter.convert2ByteArray;
import static org.apache.rocketmq.jms.support.PrimitiveTypeConverter.convert2Char;
import static org.apache.rocketmq.jms.support.PrimitiveTypeConverter.convert2Double;
import static org.apache.rocketmq.jms.support.PrimitiveTypeConverter.convert2Float;
import static org.apache.rocketmq.jms.support.PrimitiveTypeConverter.convert2Int;
import static org.apache.rocketmq.jms.support.PrimitiveTypeConverter.convert2Long;
import static org.apache.rocketmq.jms.support.PrimitiveTypeConverter.convert2Short;
import static org.apache.rocketmq.jms.support.PrimitiveTypeConverter.convert2String;

/**
 * Message can only be accessed by a thread at a time.
 */
public class RocketMQMapMessage extends RocketMQMessage implements MapMessage {

    private Map<String, Object> map;

    protected boolean readOnly;

    public RocketMQMapMessage(Map<String, Object> map) {
        this.map = map;
    }

    public RocketMQMapMessage() {
        this.map = new HashMap();
    }

    @Override public boolean getBoolean(String name) throws JMSException {
        checkName(name);

        return convert2Boolean(map.get(name));
    }

    private void checkName(String name) throws JMSException {
        if (StringUtils.isBlank(name)) {
            throw new JMSException("Name is required");
        }
    }

    @Override public byte getByte(String name) throws JMSException {
        checkName(name);

        return convert2Byte(map.get(name));
    }

    @Override public short getShort(String name) throws JMSException {
        checkName(name);

        return convert2Short(map.get(name));
    }

    @Override public char getChar(String name) throws JMSException {
        checkName(name);

        return convert2Char(map.get(name));
    }

    @Override public int getInt(String name) throws JMSException {
        checkName(name);

        return convert2Int(map.get(name));
    }

    @Override public long getLong(String name) throws JMSException {
        checkName(name);

        return convert2Long(map.get(name));
    }

    @Override public float getFloat(String name) throws JMSException {
        checkName(name);

        return convert2Float(map.get(name));
    }

    @Override public double getDouble(String name) throws JMSException {
        checkName(name);

        return convert2Double(map.get(name));
    }

    @Override public String getString(String name) throws JMSException {
        checkName(name);

        return convert2String(map.get(name));
    }

    @Override public byte[] getBytes(String name) throws JMSException {
        checkName(name);

        return convert2ByteArray(map.get(name));
    }

    @Override public Object getObject(String name) throws JMSException {
        checkName(name);

        return map.get(name);
    }

    @Override public Enumeration getMapNames() throws JMSException {
        return Collections.enumeration(map.keySet());
    }

    @Override public void setBoolean(String name, boolean value) throws JMSException {
        putProperty(name, value);
    }

    private void putProperty(String name, Object obj) throws JMSException {
        if (isReadOnly()) {
            throw new MessageNotWriteableException("Message is not writable");
        }

        checkName(name);

        map.put(name, obj);
    }

    @Override public void setByte(String name, byte value) throws JMSException {
        putProperty(name, value);
    }

    @Override public void setShort(String name, short value) throws JMSException {
        putProperty(name, value);
    }

    @Override public void setChar(String name, char value) throws JMSException {
        putProperty(name, value);
    }

    @Override public void setInt(String name, int value) throws JMSException {
        putProperty(name, value);
    }

    @Override public void setLong(String name, long value) throws JMSException {
        putProperty(name, value);
    }

    @Override public void setFloat(String name, float value) throws JMSException {
        putProperty(name, value);
    }

    @Override public void setDouble(String name, double value) throws JMSException {
        putProperty(name, value);
    }

    @Override public void setString(String name, String value) throws JMSException {
        putProperty(name, value);
    }

    @Override public void setBytes(String name, byte[] value) throws JMSException {
        putProperty(name, value);
    }

    @Override public void setBytes(String name, byte[] value, int offset, int length) throws JMSException {
        putProperty(name, value);
    }

    @Override public void setObject(String name, Object value) throws JMSException {
        putProperty(name, value);
    }

    @Override public boolean itemExists(String name) throws JMSException {
        checkName(name);

        return map.containsKey(name);
    }

    @Override public void clearBody() {
        super.clearBody();
        this.map.clear();
        this.readOnly = false;
    }

    protected boolean isReadOnly() {
        return this.readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }
}
