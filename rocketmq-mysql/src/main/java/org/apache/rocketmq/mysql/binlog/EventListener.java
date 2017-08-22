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

package org.apache.rocketmq.mysql.binlog;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.Event;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class EventListener implements BinaryLogClient.EventListener, BinaryLogClient.LifecycleListener {

    private BlockingQueue<Event> queue;

    public EventListener(BlockingQueue<Event> queue) {
        this.queue = queue;
    }

    @Override
    public void onEvent(Event event) {
        try {
            while (true) {
                if (queue.offer(event, 100, TimeUnit.MILLISECONDS)) {
                    return;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnect(BinaryLogClient client) {

    }

    @Override
    public void onCommunicationFailure(BinaryLogClient client, Exception e) {

    }

    @Override
    public void onEventDeserializationFailure(BinaryLogClient client, Exception e) {

    }

    @Override
    public void onDisconnect(BinaryLogClient client) {

    }
}
