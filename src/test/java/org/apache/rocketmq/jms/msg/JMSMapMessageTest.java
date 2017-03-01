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

import javax.jms.JMSException;
import javax.jms.MessageNotWriteableException;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class JMSMapMessageTest {

    @Test
    public void testGetBoolean() throws Exception {
        JMSMapMessage msg = new JMSMapMessage();

        // get an empty value will return false
        assertThat(msg.getBoolean("man"), is(false));

        // get an not empty value
        msg.setBoolean("man", true);
        assertThat(msg.getBoolean("man"), is(true));

        // key is null
        try {
            msg.getBoolean(null);
            assertTrue(false);
        }
        catch (JMSException e) {
            assertTrue(true);
        }

        // in read-only model
        msg.setReadOnly(true);
        try {
            msg.setBoolean("man", true);
            assertTrue(false);
        }
        catch (MessageNotWriteableException e) {
            assertTrue(true);
        }

        // both read and write are allowed after clearBody()
        msg.clearBody();
        msg.setBoolean("man", false);
        msg.getBoolean("man");

        // map is empty after clearBody()
        msg.clearBody();
        assertThat(msg.getBoolean("man"), is(false));
    }

}