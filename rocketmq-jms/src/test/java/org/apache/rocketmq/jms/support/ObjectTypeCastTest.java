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

package org.apache.rocketmq.jms.support;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ObjectTypeCastTest {

    @Test
    public void testConvert2String() throws Exception {
        assertThat(ObjectTypeCast.cast2String("name"), is("name"));
    }

    @Test
    public void testConvert2Long() throws Exception {
        assertThat(ObjectTypeCast.cast2Long(100l), is(100l));
    }

    @Test
    public void testConvert2Integer() throws Exception {
        assertThat(ObjectTypeCast.cast2Integer(100), is(100));
    }

    @Test
    public void testConvert2Boolean() throws Exception {
        assertThat(ObjectTypeCast.cast2Boolean(true), is(true));
    }

    @Test
    public void testConvert2Object() throws Exception {
        final ObjectTypeCast obj = new ObjectTypeCast();
        assertThat(ObjectTypeCast.cast2Object(obj, ObjectTypeCast.class), is(obj));
    }
}