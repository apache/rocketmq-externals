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

package org.apache.rocketmq.console.util;

import com.google.common.base.Throwables;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

public class ReflectUtil {

    public static ReflectUtil on(Object object) {
        return new ReflectUtil(object);
    }

    public static <T extends AccessibleObject> T accessible(T accessible) {
        if (accessible == null) {
            return null;
        }

        if (accessible instanceof Member) {
            Member member = (Member) accessible;

            if (Modifier.isPublic(member.getModifiers()) &&
                Modifier.isPublic(member.getDeclaringClass().getModifiers())) {

                return accessible;
            }
        }

        if (!accessible.isAccessible()) {
            accessible.setAccessible(true);
        }

        return accessible;
    }

    private final Object object;

    private final boolean isClass;

    private ReflectUtil(Object object) {
        this.object = object;
        this.isClass = false;
    }

    @SuppressWarnings("unchecked")
    public <T> T get() {
        return (T) object;
    }

    public <T> T get(String name) {
        return field(name).<T>get();
    }

    public ReflectUtil field(String name) {
        try {
            Field field = getField(name);
            return on(field.get(object));
        }
        catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public Class<?> type() {
        if (isClass) {
            return (Class<?>) object;
        }
        else {
            return object.getClass();
        }
    }

    private Field getField(String name) {
        Class<?> type = type();
        try {
            return accessible(type.getField(name));
        }
        catch (NoSuchFieldException e) {
            do {
                try {
                    return accessible(type.getDeclaredField(name));
                }
                catch (NoSuchFieldException ignore) {
                }

                type = type.getSuperclass();
            }
            while (type != null);
            throw Throwables.propagate(e);
        }
    }

}
