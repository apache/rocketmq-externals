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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import javax.jms.IllegalStateRuntimeException;
import javax.jms.JMSException;
import javax.jms.MessageEOFException;
import javax.jms.MessageFormatException;
import javax.jms.MessageNotReadableException;
import javax.jms.MessageNotWriteableException;
import org.apache.rocketmq.jms.support.PrimitiveTypeCast;

import static java.lang.String.format;

/**
 * RocketMQ ByteMessage.
 */
public class JMSBytesMessage extends AbstractJMSMessage implements javax.jms.BytesMessage {

    private byte[] bytesIn;
    private DataInputStream dataAsInput;

    private ByteArrayOutputStream bytesOut;
    private DataOutputStream dataAsOutput;

    protected boolean readOnly;

    /**
     * Message created for reading
     *
     * @param data to construct this object
     */
    public JMSBytesMessage(byte[] data) {
        this.bytesIn = data;
        this.dataAsInput = new DataInputStream(new ByteArrayInputStream(data, 0, data.length));
        this.readOnly = true;
        this.writeOnly = false;
    }

    /**
     * Message created to be sent
     */
    public JMSBytesMessage() {
        this.bytesOut = new ByteArrayOutputStream();
        this.dataAsOutput = new DataOutputStream(this.bytesOut);
        this.readOnly = false;
        this.writeOnly = true;
    }

    @Override public byte[] getBody(Class clazz) throws JMSException {
        byte[] result;
        if (isBodyAssignableTo(clazz)) {
            if (isWriteOnly()) {
                result = bytesOut.toByteArray();
                this.reset();
                return result;
            }
            else if (isReadOnly()) {
                result = Arrays.copyOf(bytesIn, bytesIn.length);
                this.reset();
                return result;
            }
            else {
                throw new IllegalStateRuntimeException("Message must be in write only or read only status");
            }
        }

        throw new MessageFormatException(format("The type[%s] can't be casted to byte[]", clazz.toString()));
    }

    @Override public byte[] getBody() throws JMSException {
        return getBody(byte[].class);
    }

    @Override public boolean isBodyAssignableTo(Class c) throws JMSException {
        return byte[].class.isAssignableFrom(c);
    }

    @Override public long getBodyLength() throws JMSException {
        if (isWriteOnly()) {
            return bytesOut.size();
        }
        else if (isReadOnly()) {
            return bytesIn.length;
        }
        else {
            throw new IllegalStateRuntimeException("Message must be in write only or read only status");
        }
    }

    public boolean readBoolean() throws JMSException {
        checkIsReadOnly();

        try {
            return dataAsInput.readBoolean();
        }
        catch (IOException e) {
            throw handleInputException(e);
        }
    }

    private void checkIsReadOnly() throws MessageNotReadableException {
        if (!isReadOnly()) {
            throw new MessageNotReadableException("Not readable");
        }
        if (dataAsInput == null) {
            throw new MessageNotReadableException("No data to read");
        }
    }

    public byte readByte() throws JMSException {
        checkIsReadOnly();

        try {
            return dataAsInput.readByte();
        }
        catch (IOException e) {
            throw handleInputException(e);
        }
    }

    public int readUnsignedByte() throws JMSException {
        checkIsReadOnly();

        try {
            return dataAsInput.readUnsignedByte();
        }
        catch (IOException e) {
            throw handleInputException(e);
        }
    }

    public short readShort() throws JMSException {
        checkIsReadOnly();

        try {
            return dataAsInput.readShort();
        }
        catch (IOException e) {
            throw handleInputException(e);
        }
    }

    public int readUnsignedShort() throws JMSException {
        checkIsReadOnly();

        try {
            return dataAsInput.readUnsignedShort();
        }
        catch (IOException e) {
            throw handleInputException(e);
        }
    }

    public char readChar() throws JMSException {
        checkIsReadOnly();

        try {
            return dataAsInput.readChar();
        }
        catch (IOException e) {
            throw handleInputException(e);
        }
    }

    public int readInt() throws JMSException {
        checkIsReadOnly();

        try {
            return dataAsInput.readInt();
        }
        catch (IOException e) {
            throw handleInputException(e);
        }
    }

    public long readLong() throws JMSException {
        checkIsReadOnly();

        try {
            return dataAsInput.readLong();
        }
        catch (IOException e) {
            throw handleInputException(e);
        }
    }

    public float readFloat() throws JMSException {
        checkIsReadOnly();

        try {
            return dataAsInput.readFloat();
        }
        catch (IOException e) {
            throw handleInputException(e);
        }
    }

    public double readDouble() throws JMSException {
        checkIsReadOnly();

        try {
            return dataAsInput.readDouble();
        }
        catch (IOException e) {
            throw handleInputException(e);
        }
    }

    public String readUTF() throws JMSException {
        checkIsReadOnly();

        try {
            return dataAsInput.readUTF();
        }
        catch (IOException e) {
            throw handleInputException(e);
        }
    }

    public int readBytes(byte[] value) throws JMSException {
        checkIsReadOnly();

        return readBytes(value, value.length);
    }

    public int readBytes(byte[] value, int length) throws JMSException {
        checkIsReadOnly();

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
        checkIsWriteOnly();
        initializeWriteIfNecessary();

        try {
            dataAsOutput.writeBoolean(value);
        }
        catch (IOException e) {
            throw handleOutputException(e);
        }
    }

    private void initializeWriteIfNecessary() {
        if (bytesOut == null) {
            bytesOut = new ByteArrayOutputStream();
        }
        if (dataAsOutput == null) {
            dataAsOutput = new DataOutputStream(bytesOut);
        }
    }

    public void writeByte(byte value) throws JMSException {
        checkIsWriteOnly();
        initializeWriteIfNecessary();

        try {
            dataAsOutput.writeByte(value);
        }
        catch (IOException e) {
            throw handleOutputException(e);
        }
    }

    public void writeShort(short value) throws JMSException {
        checkIsWriteOnly();
        initializeWriteIfNecessary();

        try {
            dataAsOutput.writeShort(value);
        }
        catch (IOException e) {
            throw handleOutputException(e);
        }
    }

    public void writeChar(char value) throws JMSException {
        checkIsWriteOnly();
        initializeWriteIfNecessary();

        try {
            dataAsOutput.writeChar(value);
        }
        catch (IOException e) {
            throw handleOutputException(e);
        }
    }

    public void writeInt(int value) throws JMSException {
        checkIsWriteOnly();
        initializeWriteIfNecessary();

        try {
            dataAsOutput.writeInt(value);
        }
        catch (IOException e) {
            throw handleOutputException(e);
        }
    }

    public void writeLong(long value) throws JMSException {
        checkIsWriteOnly();
        initializeWriteIfNecessary();

        try {
            dataAsOutput.writeLong(value);
        }
        catch (IOException e) {
            throw handleOutputException(e);
        }
    }

    public void writeFloat(float value) throws JMSException {
        checkIsWriteOnly();
        initializeWriteIfNecessary();

        try {
            dataAsOutput.writeFloat(value);
        }
        catch (IOException e) {
            throw handleOutputException(e);
        }
    }

    public void writeDouble(double value) throws JMSException {
        checkIsWriteOnly();
        initializeWriteIfNecessary();

        try {
            dataAsOutput.writeDouble(value);
        }
        catch (IOException e) {
            throw handleOutputException(e);
        }
    }

    public void writeUTF(String value) throws JMSException {
        checkIsWriteOnly();
        initializeWriteIfNecessary();

        try {
            dataAsOutput.writeUTF(value);
        }
        catch (IOException e) {
            throw handleOutputException(e);
        }
    }

    public void writeBytes(byte[] value) throws JMSException {
        checkIsWriteOnly();
        initializeWriteIfNecessary();

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
        checkIsWriteOnly();
        initializeWriteIfNecessary();

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
        checkIsWriteOnly();
        initializeWriteIfNecessary();

        if (!PrimitiveTypeCast.isPrimitiveType(value)) {
            throw new JMSException("Object must be primitive type");
        }

        try {
            dataAsOutput.writeBytes(String.valueOf(value));
        }
        catch (IOException e) {
            throw handleOutputException(e);
        }
    }

    public void reset() throws JMSException {
        try {
            if (bytesOut != null) {
                bytesOut.reset();
            }
            if (this.dataAsInput != null) {
                this.dataAsInput.reset();
            }

            this.readOnly = true;
        }
        catch (IOException e) {
            throw new JMSException(e.getMessage());
        }
    }

    @Override public void clearBody() {
        super.clearBody();
        this.bytesOut = null;
        this.dataAsOutput = null;
        this.dataAsInput = null;
        this.bytesIn = null;
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

    protected boolean isReadOnly() {
        return readOnly;
    }
}
