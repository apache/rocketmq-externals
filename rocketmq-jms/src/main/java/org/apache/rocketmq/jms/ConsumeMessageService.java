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

package org.apache.rocketmq.jms;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import javax.jms.JMSRuntimeException;
import org.apache.rocketmq.common.ServiceThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumeMessageService extends ServiceThread {

    private static final Logger log = LoggerFactory.getLogger(DeliverMessageService.class);
    private static final AtomicLong COUNTER = new AtomicLong(0L);

    private BlockingQueue<MessageWrapper> queue = new ArrayBlockingQueue(1000);
    private RocketMQSession session;
    private final long index = COUNTER.incrementAndGet();

    public ConsumeMessageService(RocketMQSession session) {
        this.session = session;
    }

    @Override public String getServiceName() {
        return ConsumeMessageService.class.getSimpleName() + "-" + this.index;
    }

    @Override public void run() {
        while (!this.isStopped()) {
            try {
                MessageWrapper wrapper = queue.take();
                RocketMQConsumer consumer = wrapper.getConsumer();
                consumer.getMessageListener().onMessage(wrapper.getMessage());
                consumer.getDeliverMessageService().ack(wrapper.getMq(), wrapper.getOffset());
            }
            catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public void put(MessageWrapper wrapper) {
        try {
            this.queue.put(wrapper);
        }
        catch (InterruptedException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    public RocketMQSession getSession() {
        return session;
    }
}
