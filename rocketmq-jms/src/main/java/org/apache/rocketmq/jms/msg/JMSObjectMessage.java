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

import java.io.Serializable;
import javax.jms.JMSException;
import org.apache.rocketmq.jms.msg.serialize.ObjectSerialize;

public class JMSObjectMessage extends AbstractJMSMessage implements javax.jms.ObjectMessage {

    private Serializable body;

    public JMSObjectMessage(Serializable object) {
        this.body = object;
    }

    public JMSObjectMessage() {

    }

    @Override public Serializable getBody(Class clazz) throws JMSException {
        return body;
    }

    @Override public byte[] getBody() throws JMSException {
        return ObjectSerialize.instance().serialize(body);
    }

    @Override public boolean isBodyAssignableTo(Class c) throws JMSException {
        return true;
    }

    public Serializable getObject() throws JMSException {
        return this.body;
    }

    public void setObject(Serializable object) throws JMSException {
        this.body = object;
    }
}
