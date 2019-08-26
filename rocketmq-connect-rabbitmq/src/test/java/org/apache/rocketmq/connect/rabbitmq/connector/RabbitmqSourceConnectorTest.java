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

package org.apache.rocketmq.connect.rabbitmq.connector;

import static org.junit.Assert.assertEquals;

import org.apache.rocketmq.connect.jms.Config;
import org.apache.rocketmq.connect.rabbitmq.RabbitmqConfig;
import org.junit.Test;

import io.openmessaging.KeyValue;
import io.openmessaging.internal.DefaultKeyValue;

public class RabbitmqSourceConnectorTest {

	RabbitmqSourceConnector rabbitmqSourceConnector = new RabbitmqSourceConnector();
	
	@Test
	public void taskClass() {
		 assertEquals( RabbitmqSourceTask.class, rabbitmqSourceConnector.taskClass());
	}

	@Test
	public void getRequiredConfig() {
		assertEquals( RabbitmqConfig.REQUEST_CONFIG , rabbitmqSourceConnector.getRequiredConfig());
	}
	
	
	@Test
	public void verifyAndSetConfig() {
        KeyValue keyValue = new DefaultKeyValue();

        for (String requestKey :RabbitmqConfig.REQUEST_CONFIG) {
            assertEquals(rabbitmqSourceConnector.verifyAndSetConfig(keyValue), "Request config key: " + requestKey);
            keyValue.put(requestKey, requestKey);
        }
        assertEquals(rabbitmqSourceConnector.verifyAndSetConfig(keyValue), "");
	}
}
