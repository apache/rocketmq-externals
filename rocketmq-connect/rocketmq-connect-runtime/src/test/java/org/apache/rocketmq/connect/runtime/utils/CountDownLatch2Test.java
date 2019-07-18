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

package org.apache.rocketmq.connect.runtime.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CountDownLatch2Test {

    private CountDownLatch2 countDownLatch2 = new CountDownLatch2(1);

    @Test
    public void testAwaitResetCountDown() throws Exception {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    countDownLatch2.await();
                    Thread.currentThread().sleep(100);
                    countDownLatch2.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        Thread.currentThread().sleep(10);
        assertEquals(Thread.State.WAITING, thread.getState());
        countDownLatch2.countDown();
        Thread.currentThread().sleep(10);
        assertEquals(Thread.State.TIMED_WAITING, thread.getState());
        countDownLatch2.reset();
        Thread.sleep(200);
        assertEquals(Thread.State.WAITING, thread.getState());
        countDownLatch2.countDown();
        Thread.currentThread().sleep(100);
        assertEquals(Thread.State.TERMINATED, thread.getState());
    }
}