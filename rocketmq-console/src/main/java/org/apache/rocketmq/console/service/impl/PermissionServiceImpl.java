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

import com.alibaba.fastjson.JSONObject;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Resource;
import org.apache.rocketmq.console.config.RMQConfigure;
import org.apache.rocketmq.console.exception.ServiceException;
import org.apache.rocketmq.console.service.PermissionService;
import org.apache.rocketmq.srvutil.FileWatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

@Service
public class PermissionServiceImpl implements PermissionService, InitializingBean {

    @Resource
    RMQConfigure configure;

    FileBasedPermissionStore fileBasedPermissionStore;

    @Override
    public Map<String, List<String>> queryRolePerms() {
        return fileBasedPermissionStore.rolePerms;
    }

    @Override
    public void afterPropertiesSet() {
        if (configure.isLoginRequired()) {
            fileBasedPermissionStore = new FileBasedPermissionStore(configure);
        }
    }

    public static class FileBasedPermissionStore {
        private final Logger log = LoggerFactory.getLogger(this.getClass());
        private static final String FILE_NAME = "role-permission.yml";

        private String filePath;
        private Map<String/**role**/, List<String>/**accessUrls**/> rolePerms = new ConcurrentHashMap<>();

        public FileBasedPermissionStore(RMQConfigure configure) {
            filePath = configure.getRocketMqConsoleDataPath() + File.separator + FILE_NAME;
            if (!new File(filePath).exists()) {
                InputStream inputStream = getClass().getResourceAsStream("/" + FILE_NAME);
                if (inputStream == null) {
                    log.error(String.format("Can not found the file %s in Spring Boot jar", FILE_NAME));
                    System.exit(1);
                } else {
                    load(inputStream);
                }
            } else {
                log.info(String.format("User Permission configure file is %s", filePath));
                load();
                watch();
            }
        }

        private void load() {
            load(null);
        }

        public void load(InputStream inputStream) {
            Yaml yaml = new Yaml();
            JSONObject rolePermsData = null;
            try {
                if (inputStream == null) {
                    rolePermsData = yaml.loadAs(new FileReader(filePath), JSONObject.class);
                } else {
                    rolePermsData = yaml.loadAs(inputStream, JSONObject.class);
                }
            } catch (Exception e) {
                log.error("load user-permission.yml failed", e);
                throw new ServiceException(0, String.format("Failed to load rolePermission property file: %s", filePath));
            }
            rolePerms = rolePermsData.getObject("rolePerms", Map.class);
        }

        private boolean watch() {
            try {
                FileWatchService fileWatchService = new FileWatchService(new String[] {filePath}, new FileWatchService.Listener() {
                    @Override
                    public void onChanged(String path) {
                        log.info("The userPermInfoMap property file changed, reload the context");
                        load();
                    }
                });
                fileWatchService.start();
                log.info("Succeed to start rolePermissionWatcherService");
                return true;
            } catch (Exception e) {
                log.error("Failed to start rolePermissionWatcherService", e);
            }
            return false;
        }
    }
}
