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

import org.junit.Assert;
import org.junit.Test;

public class JmsBytesMessageTest {

    private byte[] receiveData = "receive data test".getBytes();
    private byte[] sendData = "send data test".getBytes();

    @Test
    public void testGetData() throws Exception {
        JmsBytesMessage readMessage = new JmsBytesMessage(receiveData);

        System.out.println(new String(readMessage.getData()));
        Assert.assertEquals(new String(receiveData), new String(readMessage.getData()));

        JmsBytesMessage sendMessage = new JmsBytesMessage();
        sendMessage.writeBytes(sendData, 0, sendData.length);

        System.out.println(new String(sendMessage.getData()));
        Assert.assertEquals(new String(sendData), new String(sendMessage.getData()));

    }

    @Test
    public void testGetBodyLength() throws Exception {

        JmsBytesMessage bytesMessage = new JmsBytesMessage(receiveData);

        System.out.println(bytesMessage.getBodyLength());
        Assert.assertEquals(bytesMessage.getBodyLength(), receiveData.length);
    }

    @Test
    public void testReadBytes() throws Exception {
        JmsBytesMessage bytesMessage = new JmsBytesMessage(receiveData);

        Assert.assertEquals(bytesMessage.getBodyLength(), receiveData.length);
        byte[] receiveValue = new byte[receiveData.length];
        bytesMessage.readBytes(receiveValue);

        System.out.println(new String(receiveValue));
        Assert.assertEquals(new String(receiveValue), new String(receiveData));

    }

    @Test
    public void testReadBytes1() throws Exception {
        JmsBytesMessage bytesMessage = new JmsBytesMessage(receiveData);

        byte[] receiveValue1 = new byte[2];
        bytesMessage.readBytes(receiveValue1, 2);
        System.out.println(new String(receiveValue1));
        Assert.assertEquals(new String(receiveData).substring(0, 2), new String(receiveValue1));

        byte[] receiceValue2 = new byte[2];
        bytesMessage.readBytes(receiceValue2, 2);
        System.out.println(new String(receiceValue2));
        Assert.assertEquals(new String(receiveData).substring(2, 4), new String(receiceValue2));

    }

    @Test
    public void testWriteBytes() throws Exception {
        JmsBytesMessage jmsBytesMessage = new JmsBytesMessage();
        jmsBytesMessage.writeBytes(sendData);

        System.out.println(new String(jmsBytesMessage.getData()));
        Assert.assertEquals(new String(jmsBytesMessage.getData()), new String(sendData));

    }

    @Test
    public void testException() throws Exception {
        JmsBytesMessage jmsBytesMessage = new JmsBytesMessage();

        byte[] receiveValue = new byte[receiveData.length];
//        Throws out NullPointerException
//        jmsBytesMessage.readBytes(receiveValue);

        JmsBytesMessage sendMessage = new JmsBytesMessage(sendData);
//        Throws out NullPointerException
//        sendMessage.writeBytes("hello again".getBytes());
    }
}