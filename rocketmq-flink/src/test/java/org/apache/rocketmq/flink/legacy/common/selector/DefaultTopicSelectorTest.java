/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.flink.legacy.common.selector;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DefaultTopicSelectorTest {
    @Test
    public void getTopic() throws Exception {
        DefaultTopicSelector selector = new DefaultTopicSelector("rocket");
        assertEquals("rocket", selector.getTopic(null));
        assertEquals("", selector.getTag(null));

        selector = new DefaultTopicSelector("rocket", "tg");
        assertEquals("rocket", selector.getTopic(null));
        assertEquals("tg", selector.getTag(null));
    }
}
