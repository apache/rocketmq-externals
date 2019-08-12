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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginUtils {

    private static final Logger log = LoggerFactory.getLogger(PluginUtils.class);

    private static final DirectoryStream.Filter<Path> PLUGIN_PATH_FILTER = new DirectoryStream
        .Filter<Path>() {
        @Override
        public boolean accept(Path path) {
            return Files.isDirectory(path) || isArchive(path) || isClassFile(path);
        }
    };

    public static boolean isArchive(Path path) {
        String archivePath = path.toString().toLowerCase(Locale.ROOT);
        return archivePath.endsWith(".jar") || archivePath.endsWith(".zip");
    }

    public static boolean isClassFile(Path path) {
        return path.toString().toLowerCase(Locale.ROOT).endsWith(".class");
    }

    public static List<Path> pluginLocations(Path topPath) throws IOException {
        List<Path> locations = new ArrayList<>();
        try (
            DirectoryStream<Path> listing = Files.newDirectoryStream(
                topPath,
                PLUGIN_PATH_FILTER
            )
        ) {
            for (Path dir : listing) {
                locations.add(dir);
            }
        }
        return locations;
    }

    public static List<Path> pluginUrls(Path topPath) throws IOException {
        boolean containsClassFiles = false;
        Set<Path> archives = new TreeSet<>();
        LinkedList<DirectoryEntry> dfs = new LinkedList<>();
        Set<Path> visited = new HashSet<>();

        if (isArchive(topPath)) {
            return Collections.singletonList(topPath);
        }

        DirectoryStream<Path> topListing = Files.newDirectoryStream(
            topPath,
            PLUGIN_PATH_FILTER
        );
        dfs.push(new DirectoryEntry(topListing));
        visited.add(topPath);
        try {
            while (!dfs.isEmpty()) {
                Iterator<Path> neighbors = dfs.peek().iterator;
                if (!neighbors.hasNext()) {
                    dfs.pop().stream.close();
                    continue;
                }

                Path adjacent = neighbors.next();
                if (Files.isSymbolicLink(adjacent)) {
                    try {
                        Path symlink = Files.readSymbolicLink(adjacent);
                        Path parent = adjacent.getParent();
                        if (parent == null) {
                            continue;
                        }
                        Path absolute = parent.resolve(symlink).toRealPath();
                        if (Files.exists(absolute)) {
                            adjacent = absolute;
                        } else {
                            continue;
                        }
                    } catch (IOException e) {
                        log.warn(
                            "Resolving symbolic link '{}' failed. Ignoring this path.",
                            adjacent,
                            e
                        );
                        continue;
                    }
                }

                if (!visited.contains(adjacent)) {
                    visited.add(adjacent);
                    if (isArchive(adjacent)) {
                        archives.add(adjacent);
                    } else if (isClassFile(adjacent)) {
                        containsClassFiles = true;
                    } else {
                        DirectoryStream<Path> listing = Files.newDirectoryStream(
                            adjacent,
                            PLUGIN_PATH_FILTER
                        );
                        dfs.push(new DirectoryEntry(listing));
                    }
                }
            }
        } finally {
            while (!dfs.isEmpty()) {
                dfs.pop().stream.close();
            }
        }

        if (containsClassFiles) {
            if (archives.isEmpty()) {
                return Collections.singletonList(topPath);
            }
            log.warn("Plugin path contains both java archives and class files. Returning only the"
                + " archives");
        }
        return Arrays.asList(archives.toArray(new Path[0]));
    }

    private static class DirectoryEntry {
        final DirectoryStream<Path> stream;
        final Iterator<Path> iterator;

        DirectoryEntry(DirectoryStream<Path> stream) {
            this.stream = stream;
            this.iterator = stream.iterator();
        }
    }

}
