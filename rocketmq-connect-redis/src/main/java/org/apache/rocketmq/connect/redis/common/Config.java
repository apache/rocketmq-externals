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

package org.apache.rocketmq.connect.redis.common;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.moilioncircle.redis.replicator.RedisURI;
import org.apache.commons.lang.StringUtils;
import org.apache.rocketmq.connect.redis.util.PropertyToObjectUtils;
import io.openmessaging.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Config info.
 */
public class Config {
    private final Logger LOGGER = LoggerFactory.getLogger(Config.class);
    /**
     * Redis base info.
     */
    private String redisAddr;
    private Integer redisPort;
    private String redisPassword;
    /**
     * Timeout for jedis pool.
     */
    private Integer timeout = 3000;
    private SyncMod syncMod = SyncMod.CUSTOM_OFFSET;
    private Long offset = -1L;
    /**
     * Redis master_replid
     */
    private String replId;
    /**
     * Specify synchronized redis commands that are useful
     * only for increment redis data.
     *
     * Multiple commands are separated by commas.
     *
     */
    private String commands = RedisConstants.ALL_COMMAND;

    /**
     * Position info from connector runtime.
     */
    private Long position;

    private Integer eventCommitRetryTimes = RedisConstants.EVENT_COMMIT_RETRY_TIMES;
    private Long eventCommitRetryInterval = RedisConstants.EVENT_COMMIT_RETRY_INTERVAL;

    public static final Set<String> REQUEST_CONFIG = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "redisAddr",
        "redisPort",
        "redisPassword"
    )));

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("redisAddr: ")
            .append(redisPassword)
            .append(",")
            .append("redisPort: ")
            .append(redisPort)
            .append(",")
            .append("redisPassword: ")
            .append(redisPassword)
            .append(",")
            .append("syncMod: ")
            .append(syncMod)
            .append(",")
            .append("offset: ")
            .append(offset)
            .append(",")
            .append("replId: ")
            .append(replId)
            .append(",")
            .append("commands: ")
            .append(commands)
        ;
        return sb.toString();
    }

    public static String checkConfig(KeyValue config) {
        for (String requestKey : Config.REQUEST_CONFIG) {
            if (!config.containsKey(requestKey)) {
                return "Request lost config key: " + requestKey;
            }
        }
        return null;
    }

    public ByteBuffer getPositionPartitionKey() {
        if (StringUtils.isBlank(redisAddr) || redisPort == null) {
            return null;
        }
        return ByteBuffer.wrap((this.getRedisAddr() + this.getRedisPort()).getBytes());
    }

    public RedisURI getRedisUri() {
        StringBuilder sb = new StringBuilder("redis://");
        if (StringUtils.isBlank(redisAddr) || redisPort == null) {
            return null;
        }
        sb.append(redisAddr).append(":").append(redisPort);
        try {
            return new RedisURI(sb.toString());
        } catch (URISyntaxException e) {
            LOGGER.error("redis uri error. {}", e);
        }
        return null;
    }

    public String load(KeyValue pros) {
        try {
            PropertyToObjectUtils.properties2Object(pros, this);
        } catch (Exception e) {
            LOGGER.error("load config failed. {}", e);
            return "load config failed.";
        }
        return null;
    }

    public static Set<String> getRequestConfig() {
        return REQUEST_CONFIG;
    }

    public String getRedisAddr() {
        return redisAddr;
    }

    public void setRedisAddr(String redisAddr) {
        this.redisAddr = redisAddr;
    }

    public Integer getRedisPort() {
        return redisPort;
    }

    public void setRedisPort(Integer redisPort) {
        this.redisPort = redisPort;
    }

    public String getRedisPassword() {
        return redisPassword;
    }

    public void setRedisPassword(String redisPassword) {
        this.redisPassword = redisPassword;
    }

    public Long getPosition() {
        return position;
    }

    public void setPosition(Long position) {
        this.position = position;
    }

    public SyncMod getSyncMod() {
        return syncMod;
    }

    public void setSyncMod(String syncMod) {
        this.syncMod = SyncMod.valueOf(syncMod);
    }

    public Long getOffset() {
        return offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    public String getReplId() {
        return replId;
    }

    public void setReplId(String replId) {
        this.replId = replId;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public String getCommands() {
        return commands;
    }

    public void setCommands(String commands) {
        this.commands = commands;
    }

    public Integer getEventCommitRetryTimes() {
        return eventCommitRetryTimes;
    }

    public void setEventCommitRetryTimes(Integer eventCommitRetryTimes) {
        this.eventCommitRetryTimes = eventCommitRetryTimes;
    }

    public Long getEventCommitRetryInterval() {
        return eventCommitRetryInterval;
    }

    public void setEventCommitRetryInterval(Long eventCommitRetryInterval) {
        this.eventCommitRetryInterval = eventCommitRetryInterval;
    }

}
