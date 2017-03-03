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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MessageEOFException;
import javax.jms.MessageFormatException;
import javax.jms.MessageNotReadableException;
import javax.jms.MessageNotWriteableException;
import org.apache.rocketmq.jms.util.ExceptionUtil;

/**
 * The <CODE>BytesMessage</CODE> methods are based largely on those found in <CODE>java.io.DataInputStream</CODE> and
 * <CODE>java.io.DataOutputStream</CODE>. <P> Notice:Although the JMS API allows the use of message properties with byte
 * messages, they are typically not used, since the inclusion of properties may affect the format. <P>
 */
public class JmsBytesMessage extends JmsBaseMessage implements BytesMessage {
    private DataInputStream dataAsInput;
    private DataOutputStream dataAsOutput;
    private ByteArrayOutputStream bytesOut;
    private byte[] bytesIn;

    /**
     * Message created for reading
     *
     * @param data
     */
    public JmsBytesMessage(byte[] data) {
        this.bytesIn = data;
        dataAsInput = new DataInputStream(new ByteArrayInputStream(data, 0, data.length));
    }

    /**
     * Message created to be sent
     */
    public JmsBytesMessage() {
        bytesOut = new ByteArrayOutputStream();
        dataAsOutput = new DataOutputStream(bytesOut);
    }

    public long getBodyLength() throws JMSException {
        return getData().length;
    }

    /**
     * @return the data
     */
    public byte[] getData() {
        if (bytesOut != null) {
            return bytesOut.toByteArray();
        }
        else {
            return bytesIn;
        }

    }

    public boolean readBoolean() throws JMSException {
        throw new UnsupportedOperationException("Unsupported!");
    }

    public byte readByte() throws JMSException {
        throw new UnsupportedOperationException("Unsupported!");
    }

    public int readUnsignedByte() throws JMSException {
        throw new UnsupportedOperationException("Unsupported!");
    }

    public short readShort() throws JMSException {
        throw new UnsupportedOperationException("Unsupported!");
    }

    public int readUnsignedShort() throws JMSException {
        throw new UnsupportedOperationException("Unsupported!");
    }

    public char readChar() throws JMSException {
        throw new UnsupportedOperationException("Unsupported!");
    }

    public int readInt() throws JMSException {
        throw new UnsupportedOperationException("Unsupported!");
    }

    public long readLong() throws JMSException {
        throw new UnsupportedOperationException("Unsupported!");
    }

    public float readFloat() throws JMSException {
        throw new UnsupportedOperationException("Unsupported!");
    }

    public double readDouble() throws JMSException {
        throw new UnsupportedOperationException("Unsupported!");
    }

    public String readUTF() throws JMSException {
        throw new UnsupportedOperationException("Unsupported!");
    }

    public int readBytes(byte[] value) throws JMSException {
        return readBytes(value, value.length);
    }

    public int readBytes(byte[] value, int length) throws JMSException {
        if (length > value.length) {
            throw new IndexOutOfBoundsException("length must be smaller than the length of value");
        }
        if (dataAsInput == null) {
            throw new MessageNotReadableException("Message is not readable! ");
        }
        try {
            int offset = 0;
            while (offset < length) {
                int read = dataAsInput.read(value, offset, length - offset);
                if (read < 0) {
                    break;
                }
                offset += read;
            }

            if (offset == 0 && length != 0) {
                return -1;
            }
            else {
                return offset;
            }
        }
        catch (IOException e) {
            throw handleInputException(e);
        }

    }

    public void writeBoolean(boolean value) throws JMSException {
        ExceptionUtil.handleUnSupportedException();
    }

    public void writeByte(byte value) throws JMSException {
        ExceptionUtil.handleUnSupportedException();
    }

    public void writeShort(short value) throws JMSException {
        ExceptionUtil.handleUnSupportedException();
    }

    public void writeChar(char value) throws JMSException {
        ExceptionUtil.handleUnSupportedException();
    }

    public void writeInt(int value) throws JMSException {
        ExceptionUtil.handleUnSupportedException();
    }

    public void writeLong(long value) throws JMSException {
        ExceptionUtil.handleUnSupportedException();
    }

    public void writeFloat(float value) throws JMSException {
        ExceptionUtil.handleUnSupportedException();
    }

    public void writeDouble(double value) throws JMSException {
        ExceptionUtil.handleUnSupportedException();
    }

    public void writeUTF(String value) throws JMSException {
        ExceptionUtil.handleUnSupportedException();
    }

    public void writeBytes(byte[] value) throws JMSException {
        if (dataAsOutput == null) {
            throw new MessageNotWriteableException("Message is not writable! ");
        }
        try {
            dataAsOutput.write(value);
        }
        catch (IOException e) {
            throw handleOutputException(e);
        }
    }

    public void writeBytes(byte[] value, int offset, int length) throws JMSException {
        if (dataAsOutput == null) {
            throw new MessageNotWriteableException("Message is not writable! ");
        }
        try {
            dataAsOutput.write(value, offset, length);
        }
        catch (IOException e) {
            throw handleOutputException(e);
        }
    }

    public void writeObject(Object value) throws JMSException {
        ExceptionUtil.handleUnSupportedException();
    }

    public void reset() throws JMSException {
        ExceptionUtil.handleUnSupportedException();
    }

    private JMSException handleOutputException(final IOException e) {
        JMSException ex = new JMSException(e.getMessage());
        ex.initCause(e);
        ex.setLinkedException(e);
        return ex;
    }

    private JMSException handleInputException(final IOException e) {
        JMSException ex;
        if (e instanceof EOFException) {
            ex = new MessageEOFException(e.getMessage());
        }
        else {
            ex = new MessageFormatException(e.getMessage());
        }
        ex.initCause(e);
        ex.setLinkedException(e);
        return ex;
    }
}
