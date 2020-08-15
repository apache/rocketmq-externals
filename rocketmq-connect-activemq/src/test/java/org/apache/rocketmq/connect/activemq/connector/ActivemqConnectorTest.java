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

package org.apache.rocketmq.connect.activemq.connector;

import static org.junit.Assert.assertEquals;

import org.apache.rocketmq.connect.activemq.Config;
import org.junit.Test;

import io.openmessaging.KeyValue;
import io.openmessaging.internal.DefaultKeyValue;

public class ActivemqConnectorTest {

    ActivemqSourceConnector connector = new ActivemqSourceConnector();

    @Test
    public void verifyAndSetConfigTest() {
        KeyValue keyValue = new DefaultKeyValue();

        for (String requestKey : Config.REQUEST_CONFIG) {
            assertEquals(connector.verifyAndSetConfig(keyValue), "Request config key: " + requestKey);
            keyValue.put(requestKey, requestKey);
        }
        assertEquals(connector.verifyAndSetConfig(keyValue), "");
    }

    @Test
    public void taskClassTest() {
        assertEquals(connector.taskClass(), ActivemqSourceTask.class);
    }

    @Test
    public void taskConfigsTest() {
        assertEquals(connector.taskConfigs().get(0), null);
        KeyValue keyValue = new DefaultKeyValue();
        for (String requestKey : Config.REQUEST_CONFIG) {
            keyValue.put(requestKey, requestKey);
        }
        connector.verifyAndSetConfig(keyValue);
        assertEquals(connector.taskConfigs().get(0), keyValue);
    }
}
