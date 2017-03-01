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

package org.apache.rocketmq.jms.integration.source.support;

import org.apache.commons.lang.time.StopWatch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TimeLimitAssert {

    public static void doAssert(ConditionMatcher conditionMatcher, int timeLimit) throws InterruptedException {
        StopWatch watch = new StopWatch();
        watch.start();

        while (!conditionMatcher.match()) {
            Thread.sleep(500);
            if (watch.getTime() > timeLimit * 1000) {
                assertFalse(String.format("Doesn't match assert condition in %s second", timeLimit), true);
            }
        }

        assertTrue(true);
    }
}
