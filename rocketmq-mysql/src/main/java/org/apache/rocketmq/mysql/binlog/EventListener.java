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

import com.google.code.or.binlog.BinlogEventListener;
import com.google.code.or.binlog.BinlogEventV4;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class EventListener implements BinlogEventListener {
    private BlockingQueue<BinlogEventV4> queue;

    public EventListener(BlockingQueue<BinlogEventV4> queue) {
        this.queue = queue;
    }

    @Override
    public void onEvents(BinlogEventV4 event) {
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
}
