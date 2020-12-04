/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.flink.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public class RetryUtil {
    private static final Logger log = LoggerFactory.getLogger(RetryUtil.class);

    private static final long INITIAL_BACKOFF = 200;
    private static final long MAX_BACKOFF = 5000;
    private static final int MAX_ATTEMPTS = 5;

    private RetryUtil() {
    }

    public static void waitForMs(long sleepMs) {
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static <T> T call(Callable<T> callable, String errorMsg) throws RuntimeException {
        long backoff = INITIAL_BACKOFF;
        int retries = 0;
        do {
            try {
                return callable.call();
            } catch (Exception ex) {
                if (retries >= MAX_ATTEMPTS) {
                    throw new RuntimeException(ex);
                }
                log.error("{}, retry {}/{}", errorMsg, retries, MAX_ATTEMPTS, ex);
                retries++;
            }
            waitForMs(backoff);
            backoff = Math.min(backoff * 2, MAX_BACKOFF);
        } while (true);
    }
}
