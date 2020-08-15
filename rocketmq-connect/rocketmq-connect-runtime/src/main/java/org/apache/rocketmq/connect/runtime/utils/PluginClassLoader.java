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

import java.net.URL;
import java.net.URLClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginClassLoader extends URLClassLoader {
    private static final Logger log = LoggerFactory.getLogger(PluginClassLoader.class);
    private final URL pluginLocation;

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public PluginClassLoader(URL pluginLocation, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.pluginLocation = pluginLocation;
    }

    public PluginClassLoader(URL pluginLocation, URL[] urls) {
        super(urls);
        this.pluginLocation = pluginLocation;
    }

    public String location() {
        return pluginLocation.toString();
    }

    @Override
    public String toString() {
        return "PluginClassLoader{pluginLocation=" + pluginLocation + "}";
    }

    @Override
    public synchronized Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> klass = findLoadedClass(name);
            if (klass == null) {
                try {
                    if (!PluginUtils.shouldNotLoadInIsolation(name)) {
                        klass = findClass(name);
                    }
                } catch (ClassNotFoundException e) {
                    log.trace("Class '{}' not found. Delegating to parent", name);
                }
            }
            if (klass == null) {
                klass = super.loadClass(name, false);
            }
            if (resolve) {
                resolveClass(klass);
            }
            return klass;
        }
    }
}
