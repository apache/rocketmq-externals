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

package org.apache.rocketmq.connect.kafka.connector;

import io.openmessaging.KeyValue;
import io.openmessaging.internal.DefaultKeyValue;
import org.apache.rocketmq.connect.kafka.config.ConfigDefine;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KafkaSourceConnectorTest {
    KafkaSourceConnector connector = new KafkaSourceConnector();

    @Test
    public void verifyAndSetConfigTest() {
        KeyValue keyValue = new DefaultKeyValue();

        for (String requestKey : ConfigDefine.REQUEST_CONFIG) {
            assertEquals(connector.verifyAndSetConfig(keyValue), "Request Config key: " + requestKey);
            keyValue.put(requestKey, requestKey);
        }
        assertEquals(connector.verifyAndSetConfig(keyValue), "");
    }

    @Test
    public void taskClassTest() {
        assertEquals(connector.taskClass(), KafkaSourceTask.class);
    }

    @Test
    public void taskConfigsTest() {
        assertEquals(connector.taskConfigs().size(), 0);
        KeyValue keyValue = new DefaultKeyValue();
        for (String requestKey : ConfigDefine.REQUEST_CONFIG) {
            keyValue.put(requestKey, requestKey);
        }
        keyValue.put(ConfigDefine.TASK_NUM,1);
        connector.verifyAndSetConfig(keyValue);
        assertEquals(connector.taskConfigs().get(0).getString(ConfigDefine.TOPICS), keyValue.getString(ConfigDefine.TOPICS));
    }
}
