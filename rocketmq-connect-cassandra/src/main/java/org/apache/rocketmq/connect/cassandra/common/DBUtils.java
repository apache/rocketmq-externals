
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

package org.apache.rocketmq.connect.cassandra.common;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.internal.core.auth.PlainTextAuthProvider;
import io.netty.util.concurrent.SingleThreadEventExecutor;
import java.io.File;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.rocketmq.connect.cassandra.config.Config;
import org.apache.rocketmq.connect.cassandra.connector.CassandraSinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class DBUtils {

    private static final Logger log = LoggerFactory.getLogger(CassandraSinkTask.class);

    public static CqlSession initCqlSession(Config config) throws Exception {
        log.info("Trying to init Cql Session ");
        Map<String, String> map = new HashMap<>();

        String dbUrl = config.getDbUrl();
        String dbPort = config.getDbPort();
        String localDataCenter = config.getLocalDataCenter();
        String username =  config.getDbUsername();
        String password =  config.getDbPassword();

//        sessionBuilder.addContactPoint(new InetSocketAddress(dbUrl, Integer.parseInt(dbPort)))
//                      .withAuthCredentials(username, password);


        log.info("Cassandra dbUrl: {}", dbUrl);
        log.info("Cassandra dbPort: {}", dbPort);
        log.info("Cassandra datacenter: {}", localDataCenter);
        log.info("Cassandra username: {}", username);
        log.info("Cassandra password: {}", password);

        CqlSession cqlSession = null;
        log.info("Using Program Config Loader");
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Future<CqlSession> handle = executorService.submit(new Callable<CqlSession>() {
                @Override
                public CqlSession call() {
                    return CqlSession.builder()
                            .addContactPoint(new InetSocketAddress(dbUrl, Integer.valueOf(dbPort)))
                            .withLocalDatacenter(localDataCenter)
                            .build();
                }
            });

            cqlSession = handle.get();

        } catch (Exception e) {
            log.info("error when creating cqlSession {}", e.getMessage());
            e.printStackTrace();
        }
        log.info("init Cql Session success");

        return cqlSession;
    }
}
