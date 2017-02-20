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

import javax.jms.MessageNotReadableException;
import javax.jms.MessageNotWriteableException;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class RocketMQBytesMessageTest {

    private byte[] receiveData = "receive data test".getBytes();
    private byte[] sendData = "send data test".getBytes();

    @Test
    public void testGetData() throws Exception {
        RocketMQBytesMessage readMessage = new RocketMQBytesMessage(receiveData);
        assertThat(new String(receiveData), is(new String(readMessage.getData())));

        RocketMQBytesMessage sendMessage = new RocketMQBytesMessage();
        sendMessage.writeBytes(sendData, 0, sendData.length);
        assertThat(new String(sendData), is(new String(sendMessage.getData())));
    }

    @Test
    public void testGetBodyLength() throws Exception {
        RocketMQBytesMessage msg = new RocketMQBytesMessage(receiveData);
        assertThat(msg.getBodyLength(), is(new Long(receiveData.length)));
    }

    @Test
    public void testReadBytes1() throws Exception {
        RocketMQBytesMessage msg = new RocketMQBytesMessage(receiveData);
        byte[] receiveValue = new byte[receiveData.length];
        msg.readBytes(receiveValue);
        assertThat(new String(receiveValue), is(new String(receiveData)));

    }

    @Test
    public void testReadBytes2() throws Exception {
        RocketMQBytesMessage msg = new RocketMQBytesMessage(receiveData);

        byte[] receiveValue1 = new byte[2];
        msg.readBytes(receiveValue1);
        assertThat(new String(receiveData).substring(0, 2), is(new String(receiveValue1)));

        byte[] receiveValue2 = new byte[2];
        msg.readBytes(receiveValue2);
        assertThat(new String(receiveData).substring(2, 4), is(new String(receiveValue2)));

    }

    @Test
    public void testWriteBytes() throws Exception {
        RocketMQBytesMessage msg = new RocketMQBytesMessage();
        msg.writeBytes(sendData);
        assertThat(new String(msg.getData()), is(new String(sendData)));
    }

    @Test(expected = MessageNotReadableException.class)
    public void testNotReadableException() throws Exception {
        RocketMQBytesMessage msg = new RocketMQBytesMessage();
        msg.writeBoolean(true);
        msg.readBoolean();
    }

    @Test(expected = MessageNotWriteableException.class)
    public void testNotWritableException() throws Exception {
        RocketMQBytesMessage msg = new RocketMQBytesMessage(receiveData);
        msg.writeBoolean(true);
    }

    @Test
    public void testClearBody() throws Exception {
        RocketMQBytesMessage msg = new RocketMQBytesMessage(receiveData);
        msg.clearBody();
        msg.writeBoolean(true);
    }

    @Test
    public void testReset() throws Exception {
        RocketMQBytesMessage msg = new RocketMQBytesMessage(receiveData);
        byte[] b = new byte[2];
        msg.readBytes(b);
        msg.reset();
        msg.readBytes(b);
        assertThat(new String(receiveData).substring(0, 2), is(new String(b)));
    }
}