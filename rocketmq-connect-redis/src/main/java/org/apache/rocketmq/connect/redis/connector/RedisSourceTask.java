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

package org.apache.rocketmq.connect.redis.connector;

import io.openmessaging.connector.api.data.EntryType;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.data.SourceDataEntry;
import io.openmessaging.connector.api.source.SourceTask;
import org.apache.rocketmq.connect.redis.common.Config;
import org.apache.rocketmq.connect.redis.common.Options;
import org.apache.rocketmq.connect.redis.converter.KVEntryConverter;
import org.apache.rocketmq.connect.redis.converter.RedisEntryConverter;
import org.apache.rocketmq.connect.redis.handler.DefaultRedisEventHandler;
import org.apache.rocketmq.connect.redis.handler.RedisEventHandler;
import org.apache.rocketmq.connect.redis.pojo.KVEntry;
import org.apache.rocketmq.connect.redis.processor.DefaultRedisEventProcessor;
import org.apache.rocketmq.connect.redis.processor.RedisEventProcessor;
import org.apache.rocketmq.connect.redis.converter.RedisPositionConverter;
import org.apache.rocketmq.connect.redis.processor.RedisEventProcessorCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisSourceTask extends SourceTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisSourceTask.class);
    /**
     * listening and handle Redis event.
     */
    private RedisEventProcessor eventProcessor;
    private Config config;
    /**
     * convert kVEntry to list of sourceDataEntry
     */
    private KVEntryConverter kvEntryConverter;

    public RedisEventProcessor getEventProcessor() {
        return eventProcessor;
    }

    public void setEventProcessor(RedisEventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
    }

    public Config getConfig() {
        return config;
    }


    @Override public Collection<SourceDataEntry> poll() {
        try {
            KVEntry event = this.eventProcessor.poll();
            if (event == null) {
                return null;
            }
            event.queueName(Options.REDIS_QEUEUE.name());
            event.entryType(EntryType.UPDATE);

            Collection<SourceDataEntry> res = this.kvEntryConverter.kVEntryToDataEntries(event);
            LOGGER.info("send data entries: {}", res);
            return res;
        } catch (InterruptedException e) {
            LOGGER.error("redis task interrupted. {}", e);
            this.stop();
        } catch (Exception e) {
            LOGGER.error("redis task error. {}", e);
            this.stop();
        }
        return null;
    }


    @Override public void start(KeyValue keyValue) {
        this.kvEntryConverter = new RedisEntryConverter();

        this.config = new Config();
        this.config.load(keyValue);
        LOGGER.info("task config msg: {}", this.config.toString());

        // get position info
        ByteBuffer byteBuffer = this.context.positionStorageReader().getPosition(
            this.config.getPositionPartitionKey()
        );
        Long position = RedisPositionConverter.jsonToLong(byteBuffer);
        if (position != null && position >= -1) {
            this.config.setPosition(position);
        }
        LOGGER.info("task load connector runtime position: {}", this.config.getPosition());

        this.eventProcessor = new DefaultRedisEventProcessor(config);
        RedisEventHandler eventHandler = new DefaultRedisEventHandler(this.config);
        this.eventProcessor.registEventHandler(eventHandler);
        this.eventProcessor.registProcessorCallback(new DefaultRedisEventProcessorCallback());
        try {
            this.eventProcessor.start();
            LOGGER.info("Redis task start.");
        } catch (IOException e) {
            LOGGER.error("processor start error: {}", e);
            this.stop();
        }
    }


    @Override public void stop() {
        if (this.eventProcessor != null) {
            try {
                this.eventProcessor.stop();
                LOGGER.info("Redis task is stopped.");
            } catch (IOException e) {
                LOGGER.error("processor stop error: {}", e);
            }
        }
    }


    @Override public void pause() {

    }


    @Override public void resume() {

    }

    private class DefaultRedisEventProcessorCallback implements RedisEventProcessorCallback {
        @Override public void onStop(RedisEventProcessor eventProcessor) {
            stop();
        }
    }

}
