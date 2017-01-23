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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "rocketmq.namesrv")
public class RMQConfigure {

    private Logger logger = LoggerFactory.getLogger(RMQConfigure.class);

    private String addr;

    private String consoleCollectData;

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
        if (StringUtils.isNotBlank(addr)) {
            System.setProperty(MixAll.NAMESRV_ADDR_PROPERTY, addr);
            logger.info("setNameSrvAddrByProperty nameSrvAddr={}", addr);
        }
    }

    public String getConsoleCollectData() {
        if (!Strings.isNullOrEmpty(consoleCollectData)) {
            return consoleCollectData.trim();
        }
        return consoleCollectData;
    }

    public void setConsoleCollectData(String consoleCollectData) {
        this.consoleCollectData = consoleCollectData;
        if (!Strings.isNullOrEmpty(consoleCollectData)) {
            logger.info("setConsoleCollectData consoleCollectData={}", consoleCollectData);
        }
    }
}
