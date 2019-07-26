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

package org.apache.rocketmq.connect.runtime.connectorwrapper;

import io.openmessaging.connector.api.ConnectorContext;
import org.apache.rocketmq.connect.runtime.common.ConnectKeyValue;
import org.apache.rocketmq.connect.runtime.connectorwrapper.testimpl.TestConnector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class WorkerConnectorTest {

    private WorkerConnector workerConnector;

    @Mock
    private ConnectorContext connectorContext;

    @Before
    public void init() {
        ConnectKeyValue connectKeyValue = new ConnectKeyValue();
        connectKeyValue.put("key1", "value1");
        workerConnector = new WorkerConnector("TEST-CONN", new TestConnector(), connectKeyValue, connectorContext);
        workerConnector.start();
    }

    @After
    public void destroy() {
        workerConnector.stop();
    }

    @Test
    public void testReconfigure() {
        ConnectKeyValue connectKeyValue = new ConnectKeyValue();
        connectKeyValue.put("test2", "value2");
        workerConnector.reconfigure(connectKeyValue);
        assertThat(workerConnector.getKeyValue().equals(connectKeyValue)).isEqualTo(true);
    }

}