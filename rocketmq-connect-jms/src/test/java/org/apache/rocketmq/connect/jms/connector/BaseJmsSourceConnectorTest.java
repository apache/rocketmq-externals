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

package org.apache.rocketmq.connect.jms.connector;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.apache.rocketmq.connect.jms.Config;
import org.junit.Test;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.Task;
import io.openmessaging.internal.DefaultKeyValue;

public class BaseJmsSourceConnectorTest {

	public static final Set<String> REQUEST_CONFIG = new HashSet<String>() {
        {
            add("activemqUrl");
            add("destinationType");
            add("destinationName");
        }
    };
	
	BaseJmsSourceConnector connector = new BaseJmsSourceConnector() {
	    
		
		@Override
		public Class<? extends Task> taskClass() {
			return BaseJmsSourceTask.class;
		}
		
		@Override
		Set<String> getRequiredConfig() {
			return REQUEST_CONFIG;
		}
	};

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
        assertEquals(connector.taskClass(), BaseJmsSourceTask.class);
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
