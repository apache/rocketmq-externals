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

package org.apache.rocketmq.connect.redis.processor;

import com.moilioncircle.redis.replicator.Replicator;
import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.event.EventListener;
import com.moilioncircle.redis.replicator.event.PostRdbSyncEvent;
import com.moilioncircle.redis.replicator.event.PreCommandSyncEvent;
import com.moilioncircle.redis.replicator.event.PreRdbSyncEvent;
import com.moilioncircle.redis.replicator.rdb.datatype.AuxField;
import java.io.IOException;
import org.apache.rocketmq.connect.redis.common.Config;
import org.apache.rocketmq.connect.redis.pojo.RedisEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Do simple event filtering to push event events to queues
 */
public class RedisEventListener implements EventListener {
    protected final Logger LOGGER = LoggerFactory.getLogger(RedisEventListener.class);

    private Config config;
    private RedisEventProcessor processor;

    public RedisEventListener(Config config, RedisEventProcessor processor) {
        if (config == null) {
            throw new IllegalArgumentException("config shouldn't be null.");
        }
        this.config = config;
        this.processor = processor;
    }

    @Override public void onEvent(Replicator replicator, Event event) {
        if (isUsefulEvent(event)) {
            LOGGER.info("receive event: {}", event.getClass());
            RedisEvent redisEvent = new RedisEvent();
            redisEvent.setEvent(event);
            if (replicator != null) {
                redisEvent.setReplId(replicator.getConfiguration().getReplId());
                redisEvent.setReplOffset(replicator.getConfiguration().getReplOffset());
                redisEvent.setStreamDB(replicator.getConfiguration().getReplStreamDB());
            }

            boolean commitSuccess = commitWithRetry(redisEvent,
                this.config.getEventCommitRetryTimes(),
                this.config.getEventCommitRetryInterval());
            if(!commitSuccess){
                LOGGER.error("redis listener commit event error.");
                try {
                    this.processor.stop();
                } catch (IOException e) {
                    LOGGER.error("processor stop error. {}", e);
                }
            }
        }
    }

    private boolean commitWithRetry(RedisEvent redisEvent, int retryTimes, long retryInterval) {
        if (retryTimes < 0) {
            return false;
        }
        retryTimes--;
        try {
            if (processor.commit(redisEvent)) {
                return true;
            }
            if(retryInterval > 0){
                Thread.sleep(retryInterval);
            }
        } catch (Exception e) {
            if(retryInterval > 0){
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException ie) {
                }
            }
            LOGGER.error("processor commit redisEvent with retry({}) error: {}", retryTimes, e);
        }
        return commitWithRetry(redisEvent, retryTimes, retryInterval);
    }

    /**
     * Check whether event is an event to be processed
     */
    private boolean isUsefulEvent(Event event) {
        if (event instanceof AuxField) {
            LOGGER.warn("skip AuxField event: {} - {}", ((AuxField) event).getAuxKey(), ((AuxField) event).getAuxValue());
            return false;
        }
        if (event instanceof PreRdbSyncEvent) {
            LOGGER.warn("skip PreRdbSync event: {}", event.getClass());
            return false;
        }
        if (event instanceof PreCommandSyncEvent) {
            LOGGER.warn("skip PreCommandSync event: {}", event.getClass());
            return false;
        }
        if (event instanceof PostRdbSyncEvent) {
            LOGGER.warn("skip PostRdbSync event: {}", event.getClass());
            return false;
        }
        return true;
    }
}
