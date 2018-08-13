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
package org.apache.rocketmq.hbase.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the entry point for the RocketMQ source, which replicates RocketMQ topics to HBase tables. It assumes
 * that (i) each configured RocketMQ topic is maped to a HBase table with the same name; and (ii) the corresponding
 * HBase tables already exist.
 */
public class RocketMQSource {

    private Logger logger = LoggerFactory.getLogger(RocketMQSource.class);

    private Config config;

    public static void main(String[] args) {
        final RocketMQSource rocketMQSource = new RocketMQSource();
        rocketMQSource.execute();
    }

    /**
     * Executes this source indefinitely.
     */
    public void execute() {
        try {
            config = new Config();
            config.load();

            final MessageProcessor processor = new MessageProcessor(config);
            processor.start();

        } catch (Exception e) {
            logger.error("Error on starting RocketMQSource.", e);
        }
    }
}
