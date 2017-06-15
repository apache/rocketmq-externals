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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.rocketmq.jms;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import javax.jms.Message;
import javax.jms.MessageListener;
import org.junit.Assert;

public class JmsTestListener implements MessageListener {

    private int expectd;
    private CountDownLatch latch;
    private AtomicInteger consumedNum = new AtomicInteger(0);

    public JmsTestListener() {
        this.expectd = 10;
    }
    public JmsTestListener(int expectd) {
        this.expectd = expectd;
    }
    public JmsTestListener(int expected, CountDownLatch latch) {
        this.expectd = expected;
        this.latch = latch;
    }
    @Override
    public void onMessage(Message message) {
        try {
            Assert.assertNotNull(message);
            Assert.assertNotNull(message.getJMSMessageID());
            if (consumedNum.incrementAndGet() == expectd && latch != null) {
                latch.countDown();
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getConsumedNum() {
        return consumedNum.get();
    }

    public void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }

    public void setExpectd(int expectd) {
        this.expectd = expectd;
    }
}
