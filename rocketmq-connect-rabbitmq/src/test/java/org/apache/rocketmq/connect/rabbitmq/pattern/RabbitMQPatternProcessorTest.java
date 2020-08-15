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

package org.apache.rocketmq.connect.rabbitmq.pattern;

import static org.junit.Assert.assertEquals;

import org.apache.rocketmq.connect.jms.Replicator;
import org.apache.rocketmq.connect.rabbitmq.RabbitmqConfig;
import org.junit.Test;

import com.rabbitmq.jms.admin.RMQConnectionFactory;

public  class RabbitMQPatternProcessorTest{


	@Test
	public  void connectionFactory() {
		RabbitmqConfig rabbitmqConfig = new RabbitmqConfig();
		rabbitmqConfig.setRabbitmqUrl("amqp://112.74.48.251:5672");
		Replicator replicator = new Replicator(rabbitmqConfig, null);
		RabbitMQPatternProcessor patternProcessor = new RabbitMQPatternProcessor(replicator);
		assertEquals(RMQConnectionFactory.class, patternProcessor.connectionFactory().getClass());
    }
    

}
