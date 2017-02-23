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
import org.apache.commons.lang.builder.EqualsBuilder;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class RocketMQObjectMessageTest {

    @Test
    public void testGetObject() throws Exception {
        final User user = new User("jack", 20);
        JMSObjectMessage msg = new JMSObjectMessage(user);
        assertThat((User)msg.getObject(), is(user));
    }

    @Test
    public void testGetBody() throws Exception {
        final User user = new User("jack", 20);
        JMSObjectMessage msg = new JMSObjectMessage(user);
        assertThat((User)msg.getBody(Object.class), is((User)msg.getObject()));
    }

    private class User implements Serializable {
        private String name;
        private int age;

        private User(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj);
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}