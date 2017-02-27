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

import java.util.Date;
import javax.jms.JMSException;
import org.junit.Test;

import static org.apache.rocketmq.jms.support.PrimitiveTypeCast.cast2Boolean;
import static org.apache.rocketmq.jms.support.PrimitiveTypeCast.cast2Byte;
import static org.apache.rocketmq.jms.support.PrimitiveTypeCast.cast2ByteArray;
import static org.apache.rocketmq.jms.support.PrimitiveTypeCast.cast2Char;
import static org.apache.rocketmq.jms.support.PrimitiveTypeCast.cast2Double;
import static org.apache.rocketmq.jms.support.PrimitiveTypeCast.cast2Float;
import static org.apache.rocketmq.jms.support.PrimitiveTypeCast.cast2Int;
import static org.apache.rocketmq.jms.support.PrimitiveTypeCast.cast2Long;
import static org.apache.rocketmq.jms.support.PrimitiveTypeCast.cast2Short;
import static org.apache.rocketmq.jms.support.PrimitiveTypeCast.cast2String;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PrimitiveTypeConvertTest {

    @Test
    public void testConvert2Boolean() throws Exception {
        assertThat(cast2Boolean(new Boolean(true)), is(true));
        assertThat(cast2Boolean(null), is(false));

        assertThat(cast2Boolean("true"), is(true));
        assertThat(cast2Boolean("hello"), is(false));

        try {
            cast2Boolean(new Date());
            assertTrue(false);
        }
        catch (JMSException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testConvert2Byte() throws Exception {
        final byte b = Byte.parseByte("101", 2);
        assertThat(cast2Byte(b), is(b));

        assertThat(cast2Byte(new String("5")), is(b));
        try {
            assertThat(cast2Byte(null), is(b));
            assertTrue(false);
        }
        catch (RuntimeException e) {
            assertTrue(true);
        }

        try {
            cast2Byte("abc");
            assertTrue(false);
        }
        catch (RuntimeException e) {
            assertTrue(true);
        }

        try {
            cast2Byte(new Date());
            assertTrue(false);
        }
        catch (JMSException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testConvert2Short() throws Exception {
        final Short s = new Short("12");
        assertThat(cast2Short(s), is(s));

        assertThat(cast2Short("3"), is(new Short("3")));

        try {
            cast2Short(new Date());
            assertTrue(false);
        }
        catch (JMSException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testConvert2Char() throws Exception {
        final char c = 'a';
        assertThat(cast2Char(c), is(c));

        try {
            cast2Char("a");
            assertTrue(false);
        }
        catch (JMSException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testConvert2Int() throws Exception {
        assertThat(cast2Int(12), is(12));

        assertThat(cast2Int("12"), is(12));
        assertThat(cast2Int(Byte.parseByte("11", 2)), is(3));

        try {
            cast2Int(new Date());
            assertTrue(false);
        }
        catch (JMSException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testConvert2Long() throws Exception {
        assertThat(cast2Long(12), is(12l));

        assertThat(cast2Long("12"), is(12l));

        try {
            cast2Int(new Date());
            assertTrue(false);
        }
        catch (JMSException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testConvert2Float() throws Exception {
        assertThat(cast2Float(12.00f), is(12f));

        assertThat(cast2Float("12.00"), is(12f));

        try {
            cast2Float(12);
            assertTrue(false);
        }
        catch (JMSException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testConvert2Double() throws Exception {
        assertThat(cast2Double(12.00d), is(12d));

        assertThat(cast2Double("12.00"), is(12d));
        assertThat(cast2Double(12.00f), is(12d));

        try {
            cast2Double(12);
            assertTrue(false);
        }
        catch (JMSException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testConvert2String() throws Exception {
        assertThat(cast2String(12.00d), is("12.0"));

        assertThat(cast2String("12.00"), is("12.00"));
        assertThat(cast2String(true), is("true"));

        try {
            cast2String(new Date());
            assertTrue(false);
        }
        catch (JMSException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testConvert2ByteArray() throws Exception {
        byte[] arr = new byte[] {Byte.parseByte("11", 2), Byte.parseByte("101", 2)};

        assertThat(cast2ByteArray(arr), is(arr));

        try {
            cast2ByteArray("10");
            assertTrue(false);
        }
        catch (JMSException e) {
            assertTrue(true);
        }
    }
}