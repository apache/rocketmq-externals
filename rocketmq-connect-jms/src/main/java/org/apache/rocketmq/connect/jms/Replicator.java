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

package org.apache.rocketmq.connect.jms;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.jms.Message;

import org.apache.rocketmq.connect.jms.connector.BaseJmsSourceTask;
import org.apache.rocketmq.connect.jms.pattern.PatternProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Replicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Replicator.class);

    private PatternProcessor processor;

    private Config config;
    private BlockingQueue<Message> queue = new LinkedBlockingQueue<>();
    
    private BaseJmsSourceTask baseJmsSourceTask;

    public Replicator(Config config , BaseJmsSourceTask baseJmsSourceTask) {
        this.config = config;
        this.baseJmsSourceTask = baseJmsSourceTask;
    }

    public void start() throws Exception {
        processor = baseJmsSourceTask.getPatternProcessor(this);
        processor.start();
        LOGGER.info("Replicator start succeed");
    }

    public void stop() throws Exception {
        processor.stop();
    }

    public void commit(Message message, boolean isComplete) {
        queue.add(message);
    }

    public Config getConfig() {
        return this.config;
    }

    public BlockingQueue<Message> getQueue() {
        return queue;
    }
}
