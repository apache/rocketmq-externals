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

import org.apache.rocketmq.redis.replicator.cmd.Command;
import org.apache.rocketmq.redis.replicator.cmd.CommandListener;
import org.apache.rocketmq.redis.replicator.conf.Configure;
import org.apache.rocketmq.redis.replicator.producer.RocketMQProducer;
import org.apache.rocketmq.redis.replicator.rdb.RdbListener;
import org.apache.rocketmq.redis.replicator.rdb.datatype.KeyValuePair;
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
    private String topic = null;

    @Before
    public void setUp() throws URISyntaxException {
        topic = initTopic();
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
        final RocketMQProducer producer = new RocketMQProducer(configure);
        final AtomicInteger test = new AtomicInteger();
        replicator.addRdbListener(new RdbListener.Adaptor() {
            @Override
            public void handle(Replicator replicator, KeyValuePair<?> kv) {
                try {
                    boolean success = producer.send(kv);
                    if (!success) {
                        LOGGER.error("Fail to send KeyValuePair[key={}]", kv.getKey());
                    } else {
                        test.incrementAndGet();
                    }
                } catch (Exception e) {
                    LOGGER.error(String.format("Fail to send KeyValuePair[key=%s]", kv.getKey()), e);
                }
            }
        });

        replicator.addCommandListener(new CommandListener() {
            @Override
            public void handle(Replicator replicator, Command command) {
                try {
                    boolean success = producer.send(command);
                    if (!success) {
                        LOGGER.error("Fail to send command[{}]", command);
                    } else {
                        test.incrementAndGet();
                    }
                } catch (Exception e) {
                    LOGGER.error(String.format("Fail to send command[%s]", command), e);
                }
            }
        });

        replicator.open();
        assertEquals(19, test.get());

    }

}