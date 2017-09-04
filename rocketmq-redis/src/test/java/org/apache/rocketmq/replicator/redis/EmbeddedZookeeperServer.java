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

package org.apache.rocketmq.replicator.redis;

import java.util.Properties;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

public class EmbeddedZookeeperServer {

    public static final Logger logger = LoggerFactory.getLogger(EmbeddedZookeeperServer.class);

    private static ZooKeeperServerMain server;

    private EmbeddedZookeeperServer() {
    }

    static {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override public void uncaughtException(Thread t, Throwable e) {
                logger.error("Thread [id={},name={}] occurs error :{}", t.getId(), t.getName(), getStackTrace(e));
            }
        });
    }

    public synchronized static void start() throws Exception {
        if (server != null) {
            return;
        }
        Properties properties = new Properties();
        QuorumPeerConfig quorumPeerConfig = new QuorumPeerConfig();
        try {
            properties.load(EmbeddedZookeeperServer.class.getResourceAsStream("/zoo.cfg"));
            quorumPeerConfig.parseProperties(properties);
        }
        catch (Exception e) {
            logger.error("Error during reading embedded zookeeper config file", e);
            throw e;
        }

        server = new ZooKeeperServerMain();
        final ServerConfig serverConfig = new ServerConfig();
        serverConfig.readFrom(quorumPeerConfig);

        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    server.runFromConfig(serverConfig);
                    Thread.sleep(3 * 1000);//等待server启动完毕
                    clean();
                }
                catch (Exception e) {
                    logger.error("Fail to start embedded zookeeper server", e);
                }
            }
        }).start();
    }

    public static void clean() {
    }
}
