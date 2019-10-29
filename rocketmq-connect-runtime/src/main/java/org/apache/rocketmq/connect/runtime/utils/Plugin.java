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

import io.openmessaging.connector.api.Connector;
import io.openmessaging.connector.api.Task;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Plugin extends URLClassLoader {
    private static final Logger log = LoggerFactory.getLogger(Plugin.class);

    private final List<String> pluginPaths;

    private Map<String, PluginWrapper> classLoaderMap = new HashMap<>();

    public Plugin(List<String> pluginPaths) {
        this(pluginPaths, Plugin.class.getClassLoader());
    }

    public Plugin(List<String> pluginPaths, ClassLoader parent) {
        super(new URL[0], parent);
        this.pluginPaths = pluginPaths;
    }

    public void initPlugin() {
        for (String configPath : pluginPaths) {
            loadPlugin(configPath);
        }
    }

    private void loadPlugin(String path) {
        Path pluginPath = Paths.get(path).toAbsolutePath();
        path = pluginPath.toString();
        try {
            if (Files.isDirectory(pluginPath)) {
                for (Path pluginLocation : PluginUtils.pluginLocations(pluginPath)) {
                    registerPlugin(pluginLocation);
                }
            } else if (PluginUtils.isArchive(pluginPath)) {
                registerPlugin(pluginPath);
            }
        } catch (IOException e) {
            log.error("register plugin error, path: {}, e: {}", path, e);
        }
    }

    private void doLoad(
        ClassLoader loader,
        URL[] urls
    ) {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setClassLoaders(new ClassLoader[] {loader});
        builder.addUrls(urls);
        builder.setScanners(new SubTypesScanner());
        builder.useParallelExecutor();
        Reflections reflections = new PluginReflections(builder);
        getPlugin(reflections, Connector.class, loader);
        getPlugin(reflections, Task.class, loader);
    }

    private <T> Collection<Class<? extends T>> getPlugin(
        Reflections reflections,
        Class<T> klass,
        ClassLoader loader
    ) {
        Set<Class<? extends T>> plugins = reflections.getSubTypesOf(klass);
        Collection<Class<? extends T>> result = new ArrayList<>();
        for (Class<? extends T> plugin : plugins) {
            classLoaderMap.put(plugin.getName(), new PluginWrapper(plugin, loader));
            result.add(plugin);

        }
        return result;
    }

    private static class PluginReflections extends Reflections {

        public PluginReflections(Configuration configuration) {
            super(configuration);
        }

        @Override
        protected void scan(URL url) {
            try {
                super.scan(url);
            } catch (ReflectionsException e) {
                Logger log = Reflections.log;
                if (log != null && log.isWarnEnabled()) {
                    log.warn("Scan url error. ignoring the exception and continuing", e);
                }
            }
        }
    }

    private static PluginClassLoader newPluginClassLoader(
        final URL pluginLocation,
        final URL[] urls,
        final ClassLoader parent
    ) {
        return AccessController.doPrivileged(
            (PrivilegedAction<PluginClassLoader>) () -> new PluginClassLoader(pluginLocation, urls, parent)
        );
    }

    private void registerPlugin(Path pluginLocation)
        throws IOException {
        log.info("Loading plugin from: {}", pluginLocation);
        List<URL> pluginUrls = new ArrayList<>();
        for (Path path : PluginUtils.pluginUrls(pluginLocation)) {
            pluginUrls.add(path.toUri().toURL());
        }
        URL[] urls = pluginUrls.toArray(new URL[0]);
        if (log.isDebugEnabled()) {
            log.debug("Loading plugin urls: {}", Arrays.toString(urls));
        }
        PluginClassLoader loader = newPluginClassLoader(
            pluginLocation.toUri().toURL(),
            urls,
            this
        );
        doLoad(loader, urls);
    }

    public ClassLoader getPluginClassLoader(String pluginName) {
        PluginWrapper pluginWrapper = classLoaderMap.get(pluginName);
        if (null != pluginWrapper) {
            return pluginWrapper.getClassLoader();
        }
        return null;
    }

    public ClassLoader currentThreadLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    public static ClassLoader compareAndSwapLoaders(ClassLoader loader) {
        ClassLoader current = Thread.currentThread().getContextClassLoader();
        if (null != current && !current.equals(loader)) {
            Thread.currentThread().setContextClassLoader(loader);
        }
        return current;
    }

}
