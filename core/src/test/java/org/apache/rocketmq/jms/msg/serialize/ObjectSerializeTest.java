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

package org.apache.rocketmq.jms.msg.serialize;

import java.io.Serializable;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ObjectSerializeTest {

    @Test
    public void serializeAndDeserialize() throws Exception {
        Person person = new Person();
        person.setName("John");
        person.setAge(30);

        byte[] bytes = ObjectSerialize.instance().serialize(person);
        Person newPerson = (Person)ObjectSerialize.instance().deserialize(bytes);

        assertThat(newPerson.getName(), is(person.getName()));
        assertThat(newPerson.getAge(), is(person.getAge()));
    }

    private static class Person implements Serializable {
        private static final long serialVersionUID = -4981805070659153282L;

        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}