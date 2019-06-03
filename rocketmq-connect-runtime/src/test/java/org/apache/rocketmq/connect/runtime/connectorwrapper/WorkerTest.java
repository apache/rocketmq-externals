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


import io.openmessaging.MessagingAccessPoint;
import org.apache.rocketmq.connect.runtime.common.ConnectKeyValue;
import org.apache.rocketmq.connect.runtime.config.ConnectConfig;
import org.apache.rocketmq.connect.runtime.config.RuntimeConfigDefine;
import org.apache.rocketmq.connect.runtime.connectorwrapper.testimpl.TestConnector;
import org.apache.rocketmq.connect.runtime.connectorwrapper.testimpl.TestConverter;
import org.apache.rocketmq.connect.runtime.connectorwrapper.testimpl.TestPositionStorageReader;
import org.apache.rocketmq.connect.runtime.connectorwrapper.testimpl.TestSourceTask;
import org.apache.rocketmq.connect.runtime.service.MessagingAccessWrapper;
import org.apache.rocketmq.connect.runtime.service.PositionManagementService;
import org.apache.rocketmq.connect.runtime.utils.TestUtils;
import io.openmessaging.producer.Producer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class WorkerTest {

    @Mock
    private PositionManagementService positionManagementService;

    @Mock
    private MessagingAccessPoint messagingAccessPoint;

    @Mock
    private Producer producer;

    private ConnectConfig connectConfig;

    private Worker worker;

    private MessagingAccessWrapper messagingAccessWrapper;

    @Before
    public void init() {
        connectConfig = new ConnectConfig();
        connectConfig.setHttpPort(8081);
        connectConfig.setOmsDriverUrl("oms:rocketmq://localhost:9876/default:default");
        connectConfig.setWorkerId("DEFAULT_WORKER_1");
        connectConfig.setStorePathRootDir(System.getProperty("user.home") + File.separator + "testConnectorStore");
        messagingAccessWrapper = new MessagingAccessWrapper();
        worker = new Worker(connectConfig, positionManagementService, messagingAccessWrapper);

        Set<WorkerConnector> workingConnectors = new HashSet<>();
        for (int i=0; i<3; i++) {
            ConnectKeyValue connectKeyValue = new ConnectKeyValue();
            connectKeyValue.getProperties().put("key1", "TEST-CONN-" + i + "1");
            connectKeyValue.getProperties().put("key2", "TEST-CONN-" + i + "2");
            workingConnectors.add(new WorkerConnector("TEST-CONN-" + i, new TestConnector(), connectKeyValue));
        }
        worker.setWorkingConnectors(workingConnectors);
        assertThat(worker.getWorkingConnectors().size()).isEqualTo(3);

        Set<Runnable> runnables = new HashSet<>();
        for (int i=0; i<3; i++) {
            ConnectKeyValue connectKeyValue = new ConnectKeyValue();
            connectKeyValue.getProperties().put("key1", "TEST-TASK-" + i + "1");
            connectKeyValue.getProperties().put("key2", "TEST-TASK-" + i + "2");
            runnables.add(new WorkerSourceTask("TEST-CONN-" + i,
                    new TestSourceTask(),
                    connectKeyValue,
                    new TestPositionStorageReader(),
                    new TestConverter(),
                    producer
                    ));
        }
        worker.setWorkingTasks(runnables);
        assertThat(worker.getWorkingTasks().size()).isEqualTo(3);

        worker.start();
    }

    @After
    public void destory() {
        worker.stop();
        TestUtils.deleteFile(new File(System.getProperty("user.home") + File.separator + "testConnectorStore"));
    }

    @Test
    public void testStartConnectors() {
        Map<String, ConnectKeyValue> connectorConfigs = new HashMap<>();
        for (int i=1; i<4; i++) {
            ConnectKeyValue connectKeyValue = new ConnectKeyValue();
            connectKeyValue.getProperties().put("key1", "TEST-CONN-" + i + "1");
            connectKeyValue.getProperties().put("key2", "TEST-CONN-" + i + "2");
            connectKeyValue.getProperties().put(RuntimeConfigDefine.CONNECTOR_CLASS, TestConnector.class.getName());
            connectorConfigs.put("TEST-CONN-" + i, connectKeyValue);
        }

        try {
            worker.startConnectors(connectorConfigs);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Set<WorkerConnector> connectors = worker.getWorkingConnectors();
        assertThat(connectors.size()).isEqualTo(3);
        for (WorkerConnector wc: connectors) {
            assertThat(wc.getConnectorName()).isIn("TEST-CONN-1", "TEST-CONN-2", "TEST-CONN-3");
        }
    }

    @Test
    public void testStartTasks() {
        Map<String, List<ConnectKeyValue>> taskConfigs = new HashMap<>();
        for (int i=1; i<4; i++) {
            List<ConnectKeyValue> connectKeyValues = new ArrayList<>();
            ConnectKeyValue connectKeyValue = new ConnectKeyValue();
            connectKeyValue.getProperties().put("key1", "TEST-CONN-" + i + "1");
            connectKeyValue.getProperties().put("key2", "TEST-CONN-" + i + "2");
            connectKeyValue.getProperties().put(RuntimeConfigDefine.TASK_CLASS, TestSourceTask.class.getName());
            connectKeyValue.getProperties().put(RuntimeConfigDefine.SOURCE_RECORD_CONVERTER, TestConverter.class.getName());
            connectKeyValue.getProperties().put(RuntimeConfigDefine.OMS_DRIVER_URL, this.connectConfig.getOmsDriverUrl());
            connectKeyValues.add(connectKeyValue);
            taskConfigs.put("TEST-CONN-" + i, connectKeyValues);
        }

        try {
            worker.startTasks(taskConfigs);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Set<Runnable> sourceTasks = worker.getWorkingTasks();
        assertThat(sourceTasks.size()).isEqualTo(3);
        for (Runnable runnable : sourceTasks) {
            WorkerSourceTask workerSourceTask = null;
            WorkerSinkTask workerSinkTask = null;
            if (runnable instanceof WorkerSourceTask) {
                workerSourceTask = (WorkerSourceTask) runnable;
            } else {
                workerSinkTask = (WorkerSinkTask) runnable;
            }
            String connectorName = null != workerSourceTask ? workerSourceTask.getConnectorName() : workerSinkTask.getConnectorName();
            assertThat(connectorName).isIn("TEST-CONN-1", "TEST-CONN-2", "TEST-CONN-3");
        }
    }
}

