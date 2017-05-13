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

package org.apache.rocketmq.replicator.redis.zk;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.FluentIterable.from;

public class ZookeeperOperation {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperOperation.class);

    public static boolean checkExists(String path) {
        Preconditions.checkNotNull(path, "Path is required");

        try {
            CuratorFramework client = ZookeeperClientFactory.get();
            return client.checkExists().forPath(path) != null;
        }
        catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            throw new ZookeeperException(String.format("Not exist the path[%s]", path), e);
        }
    }

    public static Stat getNodeStat(String nodePath) {
        Preconditions.checkNotNull(nodePath, "Path is required");

        try {
            CuratorFramework client = ZookeeperClientFactory.get();
            return client.checkExists().forPath(nodePath);
        }
        catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            throw new ZookeeperException(String.format("Fail to get info of node[%s]", nodePath), e);
        }
    }

    public static void mkdir(String dirPath) {
        Preconditions.checkNotNull(dirPath, "Path is required");

        try {
            CuratorFramework client = ZookeeperClientFactory.get();
            if (client.checkExists().forPath(dirPath) == null) {
                ZKPaths.mkdirs(client.getZookeeperClient().getZooKeeper(), dirPath);
            }
        }
        catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            throw new ZookeeperException(String.format("Fail to create dir[%s]", dirPath), e);
        }
    }

    public static String mkdir(String parentPath, String dirName) {
        Preconditions.checkNotNull(parentPath, "Parent Path is required");
        Preconditions.checkNotNull(dirName, "Directory name is required");

        try {
            CuratorFramework client = ZookeeperClientFactory.get();
            String path = ZKPaths.makePath(parentPath, dirName);
            if (client.checkExists().forPath(path) == null) {
                ZKPaths.mkdirs(client.getZookeeperClient().getZooKeeper(), path);
            }
            return path;
        }
        catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            throw new ZookeeperException(String.format("Fail to create directory[%s,parent dir is %s]", dirName, parentPath), e);
        }
    }

    public static void mkdirRecursive(String dirName) {
        Preconditions.checkNotNull(dirName, "Directory name is required");

        int pos = dirName.indexOf("/");
        if (pos == -1) {
            return;
        }

        String[] array = dirName.split("/");
        String path = "";
        for (int i = 0; i < array.length; i++) {
            if (StringUtils.isBlank(array[i])) {
                continue;
            }
            path = path + "/" + array[i];
            mkdir(path);
        }
    }

    public static void createNode(String nodePath) {
        createNode(nodePath, CreateMode.PERSISTENT);
    }

    public static void createNode(String nodePath, CreateMode mode) {
        Preconditions.checkNotNull(nodePath, "Path is required");

        try {
            CuratorFramework client = ZookeeperClientFactory.get();
            if (client.checkExists().forPath(nodePath) == null) {
                client.create().creatingParentsIfNeeded().withMode(mode).forPath(nodePath);
            }
        }
        catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            throw new ZookeeperException(String.format("Fail to create node[%s]", nodePath), e);
        }
    }

    public static void createNodeAndSetData(String parentPath, String nodeName, String data) {
        createNodeAndSetData(makePath(parentPath, nodeName), data);
    }

    public static void createNodeAndSetData(String path, String data) {
        Preconditions.checkNotNull(path, "Node name is required");
        Preconditions.checkNotNull(data, "Node data is required");

        try {
            CuratorFramework client = ZookeeperClientFactory.get();
            if (client.checkExists().forPath(path) == null) {
                client.create().creatingParentsIfNeeded().forPath(path, data.getBytes());
            }
            else {
                client.setData().forPath(path, data.getBytes());
            }
        }
        catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            throw new ZookeeperException(String.format("Fail to create and set the node[%s]", path), e);
        }
    }

    public static void setData(String nodePath, String data) {
        Preconditions.checkNotNull(nodePath, "Path is required");
        Preconditions.checkNotNull(data, "Node data is required");

        try {
            CuratorFramework client = ZookeeperClientFactory.get();
            if (client.checkExists().forPath(nodePath) == null) {
                client.create().creatingParentsIfNeeded().forPath(nodePath);
            }
            client.setData().forPath(nodePath, data.getBytes());
        }
        catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            throw new ZookeeperException(String.format("Fail to set the node[%s]", nodePath, data), e);
        }
    }

    public static void delete(String nodePath) {
        Preconditions.checkNotNull(nodePath, "Path is required");

        try {
            CuratorFramework client = ZookeeperClientFactory.get();
            if (checkExists(nodePath)) {
                client.delete().deletingChildrenIfNeeded().forPath(nodePath);
            }
        }
        catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            throw new ZookeeperException(String.format("Fail to delete node[%s]", nodePath), e);
        }
    }

    public static List<String> getChildrenWithSimplePath(String dirPath) {
        Preconditions.checkNotNull(dirPath, "Directory path is required");

        try {
            CuratorFramework client = ZookeeperClientFactory.get();
            if (client.checkExists().forPath(dirPath) == null) {
                return new ArrayList();
            }

            return client.getChildren().forPath(dirPath);
        }
        catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            throw new ZookeeperException(String.format("Fail to get children of the node[%s]", dirPath), e);
        }
    }

    public static List<String> getChildrenWithFullPath(final String dirPath) {
        List<String> childrenWithSimplePath = getChildrenWithSimplePath(dirPath);

        return from(childrenWithSimplePath).transform(
            new Function<String, String>() {
                @Override public String apply(String childPath) {
                    return makePath(dirPath, childPath);
                }
            }).toList();
    }

    public static String getDataString(String nodePath) {
        return new String(getDataByte(nodePath));
    }

    public static Integer getDataInteger(String nodePath) {
        return Integer.parseInt(getDataString(nodePath));
    }

    public static Long getDataLong(String nodePath) {
        return Long.parseLong(getDataString(nodePath));
    }

    public static Boolean getDataBoolean(String nodePath) {
        return Boolean.parseBoolean(getDataString(nodePath));
    }

    public static byte[] getDataByte(String nodePath) {
        Preconditions.checkNotNull(nodePath, "Path is required");

        try {
            CuratorFramework client = ZookeeperClientFactory.get();
            return client.getData().forPath(nodePath);
        }
        catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            throw new ZookeeperException(String.format("Fail to get data from the node[%s]", nodePath), e);
        }
    }

    public static String makePath(String... names) {
        Preconditions.checkNotNull(names.length, "Node name is required");

        try {
            CuratorFramework client = ZookeeperClientFactory.get();
            String totalPath = "";
            for (int i = 0; i < names.length; i++) {
                totalPath = ZKPaths.makePath(totalPath, names[i]);
            }
            return totalPath;
        }
        catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            throw new ZookeeperException(String.format("Fail to make path"), e);
        }
    }

    public static CuratorFramework getClient() {
        return ZookeeperClientFactory.get();
    }

    /**
     * Increase node value. Notice that this method is not thread-safe.
     *
     * @param nodePath node path
     * @param increment the value increased
     */
    public static void addLongValue(String nodePath, Long increment) {
        if (!checkExists(nodePath)) {
            createNodeAndSetData(nodePath, String.valueOf(increment));
        }
        else {
            long existed = getDataLong(nodePath);
            long newValue = existed + increment;
            setData(nodePath, String.valueOf(newValue));
        }
    }

}
