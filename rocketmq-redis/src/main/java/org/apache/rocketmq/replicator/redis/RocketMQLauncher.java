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

package org.apache.rocketmq.replicator.redis;

import org.apache.rocketmq.replicator.redis.cmd.Command;
import org.apache.rocketmq.replicator.redis.cmd.CommandListener;
import org.apache.rocketmq.replicator.redis.conf.Configure;
import org.apache.rocketmq.replicator.redis.mq.RedisDataProducer;
import org.apache.rocketmq.replicator.redis.rdb.RdbListener;
import org.apache.rocketmq.replicator.redis.rdb.datatype.KeyValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.rocketmq.replicator.redis.conf.ReplicatorConstants.REDIS_MASTER_IP;
import static org.apache.rocketmq.replicator.redis.conf.ReplicatorConstants.REDIS_MASTER_PORT;

public class RocketMQLauncher {

    private static final Logger logger = LoggerFactory.getLogger(RocketMQLauncher.class);

    public static void main(String[] args) throws Exception {
        String redisMasterIp = Configure.get(REDIS_MASTER_IP, true);
        int redisMasterPort = Integer.parseInt(Configure.get(REDIS_MASTER_PORT, true));

        RedisSocketReplicator replicator = new RedisSocketReplicator(redisMasterIp, redisMasterPort, Configuration.defaultSetting());
        final RedisDataProducer redisDataProducer = new RedisDataProducer();

        replicator.addRdbListener(new RdbListener.Adaptor() {
            @Override public void handle(Replicator replicator, KeyValuePair<?> kv) {
                try {
                    boolean success = redisDataProducer.sendRdbKeyValuePair(kv);
                    if (!success) {
                        logger.error("Fail to send KeyValuePair[key={}]", kv.getKey());
                    }
                }
                catch (Exception e) {
                    logger.error(String.format("Fail to send KeyValuePair[key=%s]", kv.getKey()), e);
                }
            }

        });

        replicator.addCommandListener(new CommandListener() {
            @Override public void handle(Replicator replicator, Command command) {
                try {
                    boolean success = redisDataProducer.sendCommand(command);
                }
                catch (Exception e) {
                    logger.error(String.format("Fail to send command[%s]", command), e);
                }
            }
        });

        replicator.doOpen();
    }
}
