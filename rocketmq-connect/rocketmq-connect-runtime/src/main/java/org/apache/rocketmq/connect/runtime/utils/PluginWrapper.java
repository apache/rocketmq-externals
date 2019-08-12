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
package org.apache.rocketmq.connect.runtime.utils;

import io.openmessaging.connector.api.sink.SinkConnector;
import io.openmessaging.connector.api.source.SourceConnector;
import java.util.Locale;

public class PluginWrapper<T> {
    private final Class<? extends T> klass;
    private final String name;
    private final PluginType type;
    private final String typeName;
    private final String location;
    private final ClassLoader classLoader;

    public PluginWrapper(Class<? extends T> klass, ClassLoader loader) {
        this.klass = klass;
        this.name = klass.getName();
        this.type = PluginType.from(klass);
        this.typeName = type.toString();
        this.classLoader = loader;
        this.location = loader instanceof PluginClassLoader
            ? ((PluginClassLoader) loader).location()
            : "classpath";
    }

    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    public enum PluginType {
        SOURCE(SourceConnector.class),
        SINK(SinkConnector.class),
        UNKNOWN(Object.class);

        private Class<?> klass;

        PluginType(Class<?> klass) {
            this.klass = klass;
        }

        public static PluginType from(Class<?> klass) {
            for (PluginType type : PluginType.values()) {
                if (type.klass.isAssignableFrom(klass)) {
                    return type;
                }
            }
            return UNKNOWN;
        }

        public String simpleName() {
            return klass.getSimpleName();
        }

        @Override
        public String toString() {
            return super.toString().toLowerCase(Locale.ROOT);
        }
    }
}
