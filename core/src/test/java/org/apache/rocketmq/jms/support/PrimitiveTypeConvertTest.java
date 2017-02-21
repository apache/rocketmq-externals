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

import static org.apache.rocketmq.jms.support.PrimitiveTypeConverter.convert2Boolean;
import static org.apache.rocketmq.jms.support.PrimitiveTypeConverter.convert2Byte;
import static org.apache.rocketmq.jms.support.PrimitiveTypeConverter.convert2ByteArray;
import static org.apache.rocketmq.jms.support.PrimitiveTypeConverter.convert2Char;
import static org.apache.rocketmq.jms.support.PrimitiveTypeConverter.convert2Double;
import static org.apache.rocketmq.jms.support.PrimitiveTypeConverter.convert2Float;
import static org.apache.rocketmq.jms.support.PrimitiveTypeConverter.convert2Int;
import static org.apache.rocketmq.jms.support.PrimitiveTypeConverter.convert2Long;
import static org.apache.rocketmq.jms.support.PrimitiveTypeConverter.convert2Short;
import static org.apache.rocketmq.jms.support.PrimitiveTypeConverter.convert2String;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PrimitiveTypeConvertTest {

    @Test
    public void testConvert2Boolean() throws Exception {
        assertThat(convert2Boolean(new Boolean(true)), is(true));
        assertThat(convert2Boolean(null), is(false));

        assertThat(convert2Boolean("true"), is(true));
        assertThat(convert2Boolean("hello"), is(false));

        try {
            convert2Boolean(new Date());
            assertTrue(false);
        }
        catch (JMSException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testConvert2Byte() throws Exception {
        final byte b = Byte.parseByte("101", 2);
        assertThat(convert2Byte(b), is(b));

        assertThat(convert2Byte(new String("5")), is(b));
        try {
            assertThat(convert2Byte(null), is(b));
            assertTrue(false);
        }
        catch (RuntimeException e) {
            assertTrue(true);
        }

        try {
            convert2Byte("abc");
            assertTrue(false);
        }
        catch (RuntimeException e) {
            assertTrue(true);
        }

        try {
            convert2Byte(new Date());
            assertTrue(false);
        }
        catch (JMSException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testConvert2Short() throws Exception {
        final Short s = new Short("12");
        assertThat(convert2Short(s), is(s));

        assertThat(convert2Short("3"), is(new Short("3")));

        try {
            convert2Short(new Date());
            assertTrue(false);
        }
        catch (JMSException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testConvert2Char() throws Exception {
        final char c = 'a';
        assertThat(convert2Char(c), is(c));

        try {
            convert2Char("a");
            assertTrue(false);
        }
        catch (JMSException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testConvert2Int() throws Exception {
        assertThat(convert2Int(12), is(12));

        assertThat(convert2Int("12"), is(12));
        assertThat(convert2Int(Byte.parseByte("11", 2)), is(3));

        try {
            convert2Int(new Date());
            assertTrue(false);
        }
        catch (JMSException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testConvert2Long() throws Exception {
        assertThat(convert2Long(12), is(12l));

        assertThat(convert2Long("12"), is(12l));

        try {
            convert2Int(new Date());
            assertTrue(false);
        }
        catch (JMSException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testConvert2Float() throws Exception {
        assertThat(convert2Float(12.00f), is(12f));

        assertThat(convert2Float("12.00"), is(12f));

        try {
            convert2Float(12);
            assertTrue(false);
        }
        catch (JMSException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testConvert2Double() throws Exception {
        assertThat(convert2Double(12.00d), is(12d));

        assertThat(convert2Double("12.00"), is(12d));
        assertThat(convert2Double(12.00f), is(12d));

        try {
            convert2Double(12);
            assertTrue(false);
        }
        catch (JMSException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testConvert2String() throws Exception {
        assertThat(convert2String(12.00d), is("12.0"));

        assertThat(convert2String("12.00"), is("12.00"));
        assertThat(convert2String(true), is("true"));

        try {
            convert2String(new Date());
            assertTrue(false);
        }
        catch (JMSException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testConvert2ByteArray() throws Exception {
        byte[] arr = new byte[] {Byte.parseByte("11", 2), Byte.parseByte("101", 2)};

        assertThat(convert2ByteArray(arr), is(arr));

        try {
            convert2ByteArray("10");
            assertTrue(false);
        }
        catch (JMSException e) {
            assertTrue(true);
        }
    }
}