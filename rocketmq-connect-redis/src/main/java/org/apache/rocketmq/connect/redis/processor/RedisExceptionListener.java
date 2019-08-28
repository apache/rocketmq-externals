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

import com.moilioncircle.redis.replicator.ExceptionListener;
import com.moilioncircle.redis.replicator.Replicator;
import com.moilioncircle.redis.replicator.event.Event;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisExceptionListener implements ExceptionListener {
    protected final Logger LOGGER = LoggerFactory.getLogger(RedisExceptionListener.class);

    private RedisEventProcessor processor;

    public RedisExceptionListener(RedisEventProcessor processor){
        this.processor = processor;
    }


    @Override public void handle(Replicator replicator, Throwable throwable, Event event) {
        LOGGER.error("listen event error. {}", throwable);
        try {
            this.processor.stop();
        } catch (IOException e) {
        }
    }
}
