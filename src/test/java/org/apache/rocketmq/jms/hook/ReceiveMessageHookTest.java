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

package org.apache.rocketmq.jms.hook;

import java.text.SimpleDateFormat;
import java.util.Date;
import javax.jms.Message;
import org.apache.rocketmq.jms.exception.MessageExpiredException;
import org.apache.rocketmq.jms.msg.JMSTextMessage;
import org.apache.rocketmq.jms.msg.enums.JMSPropertiesEnum;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

public class ReceiveMessageHookTest {

    @Test(expected = MessageExpiredException.class)
    public void testValidateFail() throws Exception {
        ReceiveMessageHook hook = new ReceiveMessageHook();

        Message message = new JMSTextMessage("text");
        message.setJMSExpiration(new Date().getTime());
        Thread.sleep(100);
        hook.before(message);
    }

    @Test
    public void testValidateSuccess() throws Exception {
        ReceiveMessageHook hook = new ReceiveMessageHook();

        Message message = new JMSTextMessage("text");
        // never expired
        message.setJMSExpiration(0);
        hook.before(message);

        // expired in the future
        message.setJMSExpiration(new SimpleDateFormat("yyyy-MM-dd").parse("2999-01-01").getTime());
        hook.before(message);
    }

    @Test
    public void setProviderProperties() throws Exception {
        ReceiveMessageHook hook = new ReceiveMessageHook();

        Message message = new JMSTextMessage("text");
        hook.before(message);

        assertThat(message.getLongProperty(JMSPropertiesEnum.JMSXRcvTimestamp.name()), greaterThan(0L));
    }

}