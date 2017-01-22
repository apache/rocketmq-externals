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
package org.apache.rocketmq.console.config;

import com.alibaba.rocketmq.common.MixAll;
import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigureInitializer {
    private Logger logger = LoggerFactory.getLogger(ConfigureInitializer.class);

    private String nameSrvAddr;

    private String consoleCollectData;

    public String getNameSrvAddr() {
        return nameSrvAddr;
    }

    public void setNameSrvAddr(String nameSrvAddr) {
        this.nameSrvAddr = nameSrvAddr;
    }

    public String getConsoleCollectData() {
        return consoleCollectData;
    }

    public void setConsoleCollectData(String consoleCollectData) {
        this.consoleCollectData = consoleCollectData;
    }

    public void init() {
        if (StringUtils.isNotBlank(nameSrvAddr)) {
            System.setProperty(MixAll.NAMESRV_ADDR_PROPERTY, nameSrvAddr);
            logger.info("setNameSrvAddrByProperty nameSrvAddr={}", nameSrvAddr);
        }

        if (!Strings.isNullOrEmpty(consoleCollectData)) {
            logger.info("setConsoleCollectData consoleCollectData={}", consoleCollectData);
        }
    }
}
