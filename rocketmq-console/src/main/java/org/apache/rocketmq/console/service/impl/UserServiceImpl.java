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

package org.apache.rocketmq.console.service.impl;

import org.apache.rocketmq.console.config.RMQConfigure;
import org.apache.rocketmq.console.exception.ServiceException;
import org.apache.rocketmq.console.model.User;
import org.apache.rocketmq.console.service.UserService;
import org.apache.rocketmq.srvutil.FileWatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserServiceImpl implements UserService, InitializingBean {
    @Resource
    RMQConfigure configure;

    FileBasedUserInfoStore fileBasedUserInfoStore;

    @Override
    public User queryByName(String name) {
        return fileBasedUserInfoStore.queryByName(name);
    }

    @Override
    public User queryByUsernameAndPassword(String username, String password) {
        return fileBasedUserInfoStore.queryByUsernameAndPassword(username, password);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (configure.isEnableDashBoardCollect()) {
            fileBasedUserInfoStore = new FileBasedUserInfoStore(configure);
        }
    }

    /*packaged*/ static class FileBasedUserInfoStore {
        private final Logger log = LoggerFactory.getLogger(this.getClass());
        private static final String FILE_NAME = "users.properties";

        private String filePath;
        private final Map<String, User> userMap = new ConcurrentHashMap<>();


        public FileBasedUserInfoStore(RMQConfigure configure) {
            filePath = configure.getRocketMqConsoleDataPath() + File.separator + FILE_NAME;
            if (!new File(filePath).exists()) {
                //Use the default path
                InputStream inputStream = getClass().getResourceAsStream("/" + FILE_NAME);
                if (inputStream == null) {
                    log.error(String.format("Can not found the file %s in Spring Boot jar", FILE_NAME));
                    System.out.printf(String.format("Can not found file %s in Spring Boot jar or %s, stop the  console starting",
                            FILE_NAME, configure.getRocketMqConsoleDataPath()));
                    System.exit(1);
                } else {
                    load(inputStream);
                }
            } else {
                log.info(String.format("Login Users configure file is %s", filePath));
                load();
                watch();
            }
        }

        private void load() {
            load(null);
        }

        private void load(InputStream inputStream) {

            Properties prop = new Properties();
            try {
                if (inputStream == null) {
                    prop.load(new FileReader(filePath));
                } else {
                    prop.load(inputStream);
                }
            } catch (Exception e) {
                log.error("load user.properties failed", e);
                throw new ServiceException(0, String.format("Failed to load loginUserInfo property file: %s", filePath));
            }

            Map<String, User> loadUserMap = new HashMap<>();
            String[] arrs;
            int role;
            for (String key : prop.stringPropertyNames()) {
                String v = prop.getProperty(key);
                if (v == null) continue;
                arrs = v.split(",", 2);
                if (arrs.length == 0) {
                    continue;
                } else if (arrs.length == 1) {
                    role = 0;
                } else {
                    role = Integer.parseInt(arrs[1].trim());
                }

                loadUserMap.put(key, new User(key, arrs[0].trim(), role));
            }


            userMap.clear();
            userMap.putAll(loadUserMap);
        }

        private boolean watch() {
            try {
                FileWatchService fileWatchService = new FileWatchService(new String[]{filePath}, new FileWatchService.Listener() {
                    @Override
                    public void onChanged(String path) {
                        log.info("The loginUserInfo property file changed, reload the context");
                        load();
                    }
                });
                fileWatchService.start();
                log.info("Succeed to start LoginUserWatcherService");
                return true;
            } catch (Exception e) {
                log.error("Failed to start LoginUserWatcherService", e);
            }
            return false;
        }


        public User queryByName(String name) {
            return userMap.get(name);
        }

        public User queryByUsernameAndPassword(@NotNull String username, @NotNull String password) {
            User user = queryByName(username);
            if (user != null && password.equals(user.getPassword())) {
                return user.cloneOne();
            }

            return null;
        }
    }
}
