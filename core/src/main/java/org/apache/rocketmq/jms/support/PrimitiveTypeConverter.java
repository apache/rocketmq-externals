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

import javax.jms.JMSException;
import javax.jms.MapMessage;

/**
 * Primitive type converter, according to the conversion table in {@link MapMessage}.
 */
public class PrimitiveTypeConverter {

    public static boolean convert2Boolean(Object obj) throws JMSException {
        if (obj == null) {
            return Boolean.valueOf(null);
        }

        if (Boolean.class.isInstance(obj)) {
            return (Boolean) obj;
        }
        if (String.class.isInstance(obj)) {
            return Boolean.valueOf((String) obj);
        }

        throw new JMSException("Incorrect type[" + obj.getClass() + "] to convert");
    }

    public static byte convert2Byte(Object obj) throws JMSException {
        if (obj == null) {
            return Byte.valueOf(null);
        }

        if (Byte.class.isInstance(obj)) {
            return (Byte) obj;
        }
        if (String.class.isInstance(obj)) {
            return Byte.valueOf((String) obj);
        }

        throw new JMSException("Incorrect type[" + obj.getClass() + "] to convert");
    }

    public static short convert2Short(Object obj) throws JMSException {
        if (obj == null) {
            return Short.valueOf(null);
        }

        if (Byte.class.isInstance(obj)) {
            return ((Byte) obj).shortValue();
        }
        if (Short.class.isInstance(obj)) {
            return (Short) obj;
        }
        if (String.class.isInstance(obj)) {
            return Short.valueOf((String) obj);
        }

        throw new JMSException("Incorrect type[" + obj.getClass() + "] to convert");
    }

    public static char convert2Char(Object obj) throws JMSException {
        if (obj == null) {
            throw new NullPointerException("Obj is required");
        }

        if (Character.class.isInstance(obj)) {
            return (Character) obj;
        }

        throw new JMSException("Incorrect type[" + obj.getClass() + "] to convert");
    }

    public static int convert2Int(Object obj) throws JMSException {
        if (obj == null) {
            return Integer.valueOf(null);
        }

        if (Byte.class.isInstance(obj)) {
            return ((Byte) obj).intValue();
        }
        if (Short.class.isInstance(obj)) {
            return ((Short) obj).intValue();
        }
        if (Integer.class.isInstance(obj)) {
            return (Integer) obj;
        }
        if (String.class.isInstance(obj)) {
            return Integer.parseInt((String) obj);
        }

        throw new JMSException("Incorrect type[" + obj.getClass() + "] to convert");
    }

    public static long convert2Long(Object obj) throws JMSException {
        if (obj == null) {
            return Long.valueOf(null);
        }

        if (Byte.class.isInstance(obj)) {
            return ((Byte) obj).longValue();
        }
        if (Short.class.isInstance(obj)) {
            return ((Short) obj).longValue();
        }
        if (Integer.class.isInstance(obj)) {
            return ((Integer) obj).longValue();
        }
        if (Long.class.isInstance(obj)) {
            return (Long) obj;
        }
        if (String.class.isInstance(obj)) {
            return Long.parseLong((String) obj);
        }

        throw new JMSException("Incorrect type[" + obj.getClass() + "] to convert");
    }

    public static float convert2Float(Object obj) throws JMSException {
        if (obj == null) {
            return Float.valueOf(null);
        }

        if (Float.class.isInstance(obj)) {
            return (Float) obj;
        }
        if (String.class.isInstance(obj)) {
            return Float.parseFloat((String) obj);
        }

        throw new JMSException("Incorrect type[" + obj.getClass() + "] to convert");
    }

    public static double convert2Double(Object obj) throws JMSException {
        if (obj == null) {
            return Double.valueOf(null);
        }

        if (Float.class.isInstance(obj)) {
            return ((Float) obj).doubleValue();
        }
        if (Double.class.isInstance(obj)) {
            return (Double) obj;
        }
        if (String.class.isInstance(obj)) {
            return Double.parseDouble((String) obj);
        }

        throw new JMSException("Incorrect type[" + obj.getClass() + "] to convert");
    }

    public static String convert2String(Object obj) throws JMSException {
        if (obj == null) {
            return String.valueOf(null);
        }

        if (Boolean.class.isInstance(obj)
            || Byte.class.isInstance(obj)
            || Short.class.isInstance(obj)
            || Character.class.isInstance(obj)
            || Integer.class.isInstance(obj)
            || Long.class.isInstance(obj)
            || Float.class.isInstance(obj)
            || Double.class.isInstance(obj)
            || String.class.isInstance(obj)
            ) {
            return obj.toString();
        }

        throw new JMSException("Incorrect type[" + obj.getClass() + "] to convert");
    }

    public static byte[] convert2ByteArray(Object obj) throws JMSException {
        if (obj instanceof byte[]) {
            return (byte[]) obj;
        }

        throw new JMSException("Incorrect type[" + obj.getClass() + "] to convert");
    }
}
