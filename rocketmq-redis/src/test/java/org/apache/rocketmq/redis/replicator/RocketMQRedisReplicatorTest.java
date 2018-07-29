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

package org.apache.rocketmq.redis.replicator;

import com.moilioncircle.redis.replicator.CloseListener;
import com.moilioncircle.redis.replicator.Replicator;
import com.moilioncircle.redis.replicator.cmd.Command;
import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.event.EventListener;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyValuePair;
import org.apache.rocketmq.redis.replicator.conf.Configure;
import org.apache.rocketmq.redis.replicator.mq.RocketMQRedisProducer;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.TestCase.assertEquals;

public class RocketMQRedisReplicatorTest extends BaseConf {
    
    protected static final Logger LOGGER = LoggerFactory.getLogger(RocketMQRedisReplicatorTest.class);
    
    private static Properties properties = new Properties();
    
    @Before
    public void setUp() throws URISyntaxException {
        String topic = initTopic();
        URL url = RocketMQRedisReplicatorTest.class.getClassLoader().getResource("dumpV7.rdb");
        URI uri = url.toURI();
        URI redisURI = new URI("redis", uri.getRawAuthority(), uri.getRawPath(), uri.getRawQuery(), uri.getRawFragment());
        properties.setProperty("redis.uri", redisURI.toString());
        properties.setProperty("rocketmq.nameserver.address", nsAddr);
        properties.setProperty("rocketmq.producer.groupname", "REDIS_REPLICATOR_PRODUCER_GROUP");
        properties.setProperty("rocketmq.data.topic", topic);
    }
    
    @Test
    public void open() throws Exception {
        Configure configure = new Configure(properties);
        Replicator replicator = new RocketMQRedisReplicator(configure);
        final RocketMQRedisProducer producer = new RocketMQRedisProducer(configure);
        producer.open();
        final AtomicInteger test = new AtomicInteger();
        replicator.addEventListener(new EventListener() {
            @Override
            public void onEvent(Replicator replicator, Event event) {
                if (event instanceof KeyValuePair<?, ?>) {
                    try {
                        boolean success = producer.send(event);
                        if (success) {
                            test.incrementAndGet();
                        }
                    } catch (Exception e) {
                        LOGGER.error("Fail to send KeyValuePair", e);
                    }
                } else if (event instanceof Command) {
                    try {
                        boolean success = producer.send(event);
                        if (success) {
                            test.incrementAndGet();
                        }
                    } catch (Exception e) {
                        LOGGER.error("Fail to send command", e);
                    }
                }
            }
        });
        
        replicator.addCloseListener(new CloseListener() {
            @Override
            public void handle(Replicator replicator) {
                producer.close();
            }
        });
        
        replicator.open();
        assertEquals(19, test.get());
    }
    
}