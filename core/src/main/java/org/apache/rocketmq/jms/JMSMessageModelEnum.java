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

package org.apache.rocketmq.jms;

import org.apache.rocketmq.jms.msg.JMSBytesMessage;
import org.apache.rocketmq.jms.msg.JMSMapMessage;
import org.apache.rocketmq.jms.msg.JMSObjectMessage;
import org.apache.rocketmq.jms.msg.JMSTextMessage;

public enum JMSMessageModelEnum {
    BYTE(JMSBytesMessage.class),
    MAP(JMSMapMessage.class),
    OBJECT(JMSObjectMessage.class),
    STRING(JMSTextMessage.class);

    public static final String MSG_MODEL_NAME = "MsgModel";

    private Class jmsClass;

    JMSMessageModelEnum(Class jmsClass) {
        this.jmsClass = jmsClass;
    }

    public static JMSMessageModelEnum toMsgModelEnum(Class clazz) {
        for (JMSMessageModelEnum e : values()) {
            if (e.getJmsClass() == clazz) {
                return e;
            }
        }

        throw new IllegalArgumentException(String.format("Not supported class[%s]", clazz));
    }

    public Class getJmsClass() {
        return jmsClass;
    }
}
