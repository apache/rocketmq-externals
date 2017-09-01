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

package org.apache.rocketmq.redis.replicator.rdb;

import org.apache.rocketmq.redis.replicator.Replicator;
import org.apache.rocketmq.redis.replicator.rdb.datatype.AuxField;
import org.apache.rocketmq.redis.replicator.rdb.datatype.KeyValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface RdbListener {
    void preFullSync(Replicator replicator);

    void auxField(Replicator replicator, AuxField auxField);

    void handle(Replicator replicator, KeyValuePair<?> kv);

    void postFullSync(Replicator replicator, long checksum);

    abstract class Adaptor implements RdbListener {

        private static final Logger LOGGER = LoggerFactory.getLogger(Adaptor.class);

        public void preFullSync(Replicator replicator) {
            LOGGER.info("pre full sync");
        }

        public void auxField(Replicator replicator, AuxField auxField) {
        }

        public void postFullSync(Replicator replicator, long checksum) {
            LOGGER.info("post full sync");
        }
    }
}
