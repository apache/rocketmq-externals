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

package org.apache.rocketmq.connect.runtime.connectorwrapper.testimpl;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.data.EntryType;
import io.openmessaging.connector.api.data.Schema;
import io.openmessaging.connector.api.data.SourceDataEntry;
import io.openmessaging.connector.api.source.SourceTask;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TestSourceTask extends SourceTask {

    @Override
    public Collection<SourceDataEntry> poll() {
        Set<SourceDataEntry> sourceTasks = new HashSet<>();
        Object[] newPayload = new Object[1];
        newPayload[0] = Base64.getEncoder().encodeToString("test".getBytes());
        sourceTasks.add(new SourceDataEntry(
            ByteBuffer.wrap("1".getBytes()),
            ByteBuffer.wrap("2".getBytes()),
            System.currentTimeMillis(),
            EntryType.CREATE,
            "test-queue",
            new Schema(),
            newPayload
        ));
        return sourceTasks;
    }

    @Override
    public void start(KeyValue config) {

    }

    @Override
    public void stop() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }
}
