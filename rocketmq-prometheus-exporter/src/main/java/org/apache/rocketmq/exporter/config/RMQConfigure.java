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
package org.apache.rocketmq.exporter.config;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.MixAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;



import static org.apache.rocketmq.client.ClientConfig.SEND_MESSAGE_WITH_VIP_CHANNEL_PROPERTY;

@Configuration
@ConfigurationProperties(prefix = "rocketmq.config")
public class RMQConfigure {

    public static final String ROCKETMQ_CONFIG_WEB_TELEMETRY_PATH = "rocketmq.config.webTelemetryPath";
    public static final String ROCKETMQ_CONFIG_ROCKETMQ_VERSION = "rocketmq.config.rocketmqVersion";

    private Logger logger = LoggerFactory.getLogger(RMQConfigure.class);
    //use rocketmq.namesrv.addr first,if it is empty,than use system proerty or system env
    private volatile String namesrvAddr = System.getProperty(MixAll.NAMESRV_ADDR_PROPERTY, System.getenv(MixAll.NAMESRV_ADDR_ENV));

    private volatile String isVIPChannel = System.getProperty(SEND_MESSAGE_WITH_VIP_CHANNEL_PROPERTY, "true");

    private boolean enableCollect;

    private volatile String webTelemetryPath = System.getProperty(ROCKETMQ_CONFIG_WEB_TELEMETRY_PATH, "/metrics");

    private volatile String rocketmqVersion = System.getProperty(ROCKETMQ_CONFIG_ROCKETMQ_VERSION, "V4_3_2");

    public String getNamesrvAddr() {
        return namesrvAddr;
    }

    public void setNamesrvAddr(String namesrvAddr) {
        if (StringUtils.isNotBlank(namesrvAddr)) {
            this.namesrvAddr = namesrvAddr;
            System.setProperty(MixAll.NAMESRV_ADDR_PROPERTY, namesrvAddr);
            logger.info("setNameSrvAddrByProperty nameSrvAddr={}", namesrvAddr);
        }
    }

    public String getIsVIPChannel() {
        return isVIPChannel;
    }

    public void setIsVIPChannel(String isVIPChannel) {
        if (StringUtils.isNotBlank(isVIPChannel)) {
            this.isVIPChannel = isVIPChannel;
            System.setProperty(SEND_MESSAGE_WITH_VIP_CHANNEL_PROPERTY, isVIPChannel);
            logger.info("setIsVIPChannel isVIPChannel={}", isVIPChannel);
        }
    }
    public boolean isEnableCollect() {
        return enableCollect;
    }
    public void setEnableCollect(boolean enableCollect) {
        this.enableCollect = enableCollect;
    }

    public void setWebTelemetryPath(String webTelemetryPath) {
        if (StringUtils.isNotBlank(webTelemetryPath)) {
            this.webTelemetryPath = webTelemetryPath;
            System.setProperty(ROCKETMQ_CONFIG_WEB_TELEMETRY_PATH, webTelemetryPath);
            logger.info("setWebTelemetryPath webTelemetryPath={}", webTelemetryPath);
        }
    }

    public String getWebTelemetryPath() {
        return webTelemetryPath;
    }

    public void setRocketmqVersion(String rocketmqVersion) {
        if (StringUtils.isNotBlank(rocketmqVersion)) {
            this.rocketmqVersion = rocketmqVersion;
            System.setProperty(ROCKETMQ_CONFIG_ROCKETMQ_VERSION, rocketmqVersion);
            logger.info("setRocketmqVersion rocketmqVersion={}", rocketmqVersion);
        }
    }

    public String getRocketmqVersion() {
        return rocketmqVersion;
    }
}
