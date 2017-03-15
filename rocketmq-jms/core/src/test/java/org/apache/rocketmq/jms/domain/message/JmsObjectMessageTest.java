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

import java.io.Serializable;
import javax.jms.JMSException;
import org.junit.Assert;
import org.junit.Test;

public class JmsObjectMessageTest {

    @Test
    public void testGetObject() {
        JmsObjectMessage jmsObjectMessage = new JmsObjectMessage(new User("jack", 20));
        try {
            Assert.assertEquals(jmsObjectMessage.getObject(), new User("jack", 20));
        }
        catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetBody() {
        JmsObjectMessage jmsObjectMessage = new JmsObjectMessage(new User("jack", 20));

        try {
            User user = (User)jmsObjectMessage.getBody(Object.class);
            System.out.println(user.getName() + ": " + user.getAge());
            Assert.assertEquals(jmsObjectMessage.getBody(Object.class), jmsObjectMessage.getObject());
        }
        catch (JMSException e) {
            e.printStackTrace();
        }
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
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;

            User user = (User)obj;
            if (age != user.getAge())
                return false;
            if (name != null ? !name.equals(user.getName()) : user.getName() != null)
                return false;
            return true;
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