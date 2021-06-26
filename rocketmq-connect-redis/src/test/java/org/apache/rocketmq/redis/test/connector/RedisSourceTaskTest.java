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

package org.apache.rocketmq.redis.test.connector;

import com.moilioncircle.redis.replicator.rdb.datatype.KeyStringValueString;
import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.PositionStorageReader;
import io.openmessaging.connector.api.data.FieldType;
import io.openmessaging.connector.api.data.SourceDataEntry;
import io.openmessaging.connector.api.source.SourceTaskContext;
import io.openmessaging.internal.DefaultKeyValue;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import org.apache.rocketmq.connect.redis.connector.RedisSourceTask;
import org.apache.rocketmq.connect.redis.pojo.RedisEvent;
import org.apache.rocketmq.connect.redis.processor.DefaultRedisEventProcessor;
import org.apache.rocketmq.connect.redis.processor.RedisEventProcessor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.exceptions.JedisConnectionException;

import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_STRING;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RedisSourceTaskTest {
    private KeyValue keyValue;
    private RedisSourceTask task;

    @Before
    public void initAndStartTask() {
        try {
            initKeyValue();
            this.task = new RedisSourceTask();
            this.task.initialize(new SourceTaskContext() {
                @Override
                public PositionStorageReader positionStorageReader() {
                    return new PositionStorageReader() {
                        @Override
                        public ByteBuffer getPosition(ByteBuffer byteBuffer) {
                            return null;
                        }

                        @Override
                        public Map<ByteBuffer, ByteBuffer> getPositions(Collection<ByteBuffer> collection) {
                            return null;
                        }
                    };
                }

                @Override
                public KeyValue configs() {
                    return keyValue;
                }
            });
            this.task.start(this.keyValue);
        } catch (JedisConnectionException e) {

        }
    }

    @Test
    public void testTask() throws Exception {
        if (this.task != null) {
            RedisEvent redisEvent = getRedisEvent();
            this.task.getEventProcessor().commit(redisEvent);
            Collection<SourceDataEntry> col = this.task.poll();
            Assert.assertNotNull(col);
            Assert.assertEquals(1, col.size());
            Assert.assertNotNull(this.task.getConfig());
        }
    }

    @Test
    public void testException() {
        RedisEventProcessor processor = mock(DefaultRedisEventProcessor.class);
        try {
            when(processor.poll()).thenThrow(new InterruptedException());
        } catch (Exception e) {
            e.printStackTrace();
        }

        RedisSourceTask redisSourceTask = new RedisSourceTask();
        redisSourceTask.setEventProcessor(processor);
        redisSourceTask.poll();



        RedisEventProcessor processor2 = mock(DefaultRedisEventProcessor.class);
        try {
            when(processor2.poll()).thenThrow(new Exception());
        } catch (Exception e) {
            e.printStackTrace();
        }

        RedisSourceTask redisSourceTask2 = new RedisSourceTask();
        redisSourceTask2.setEventProcessor(processor2);
        redisSourceTask2.poll();
    }

    @After
    public void stopTask() {
        if (this.task != null) {
            this.task.pause();
            this.task.resume();
            this.task.stop();
        }
    }

    private RedisEvent getRedisEvent() {
        RedisEvent redisEvent = new RedisEvent();

        KeyStringValueString event = new KeyStringValueString();
        event.setKey("key".getBytes());
        event.setValue("value".getBytes());
        event.setValueRdbType(RDB_TYPE_STRING);

        redisEvent.setEvent(event);
        redisEvent.setReplId("replId");
        redisEvent.setReplOffset(6000L);
        redisEvent.setStreamDB(0);
        return redisEvent;
    }

    private void initKeyValue() {
        this.keyValue = new DefaultKeyValue();
        this.keyValue.put("redisAddr", "127.0.0.1");
        this.keyValue.put("redisPort", "6379");
        this.keyValue.put("redisPassword", "");
    }

}
