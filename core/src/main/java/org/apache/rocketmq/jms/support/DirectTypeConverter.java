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

/**
 * Converter that convert object directly, which means Integer can only be
 * converted to Integer,rather than Integer and Long.
 */
public class DirectTypeConverter {

    public static String convert2String(Object obj) {
        if (obj == null) {
            return null;
        }
        if (String.class.isInstance(obj)) {
            return (String) obj;
        }
        throw new ClassCastException("To converted object is " + obj.getClass() + ", not String.class");
    }

    public static Long convert2Long(Object obj) {
        if (obj == null) {
            return null;
        }
        if (Long.class.isInstance(obj)) {
            return (Long) obj;
        }
        throw new ClassCastException("To converted object is " + obj.getClass() + ", not Long.class");
    }

    public static Integer convert2Integer(Object obj) {
        if (obj == null) {
            return null;
        }
        if (Integer.class.isInstance(obj)) {
            return (Integer) obj;
        }
        throw new ClassCastException("To converted object is " + obj.getClass() + ", not Integer.class");
    }

    public static Boolean convert2Boolean(Object obj) {
        if (obj == null) {
            return null;
        }
        if (Boolean.class.isInstance(obj)) {
            return (Boolean) obj;
        }
        throw new ClassCastException("To converted object is " + obj.getClass() + ", not Boolean.class");
    }

    public static <T> T convert2Object(Object obj, Class<T> target) {
        if (obj == null) {
            return null;
        }
        if (target.isInstance(obj)) {
            return (T) obj;
        }
        throw new ClassCastException("To converted object is " + obj.getClass() + ", not " + target.getSimpleName() + ".class");
    }
}
