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

import javax.jms.JMSException;
import javax.jms.MessageFormatException;

import static java.lang.String.format;

public class JMSTextMessage extends AbstractJMSMessage implements javax.jms.TextMessage {

    private String text;

    public JMSTextMessage() {

    }

    public JMSTextMessage(String text) {
        setText(text);
    }

    @Override public String getBody(Class clazz) throws JMSException {
        if (isBodyAssignableTo(clazz)) {
            return text;
        }

        throw new MessageFormatException(format("The type[%s] can't be casted to byte[]", clazz.toString()));
    }

    @Override public byte[] getBody() throws JMSException {
        return new byte[0];
    }

    @Override public boolean isBodyAssignableTo(Class c) throws JMSException {
        return String.class.isAssignableFrom(c);
    }

    public void clearBody() {
        super.clearBody();
        this.text = null;
    }

    public String getText() throws JMSException {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }

}
