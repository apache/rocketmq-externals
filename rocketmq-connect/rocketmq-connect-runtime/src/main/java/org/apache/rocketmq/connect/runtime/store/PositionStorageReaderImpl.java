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

package org.apache.rocketmq.connect.runtime.store;

import io.openmessaging.connector.api.PositionStorageReader;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.apache.rocketmq.connect.runtime.common.PositionValue;
import org.apache.rocketmq.connect.runtime.service.PositionManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PositionStorageReaderImpl implements PositionStorageReader {

    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_RUNTIME);

    private PositionManagementService positionManagementService;

    private final String taskId;

    public PositionStorageReaderImpl(PositionManagementService positionManagementService, String taskId) {

        this.positionManagementService = positionManagementService;
        this.taskId = taskId;
    }

    @Override
    public ByteBuffer getPosition(ByteBuffer partition) {
        PositionValue positionValue = positionManagementService.getPositionTable().get(taskId);
        if (positionValue != null && positionValue.getPartition().equals(partition)) {
            return positionValue.getPosition();
        } else if (positionValue == null) {       // when the exist task restart
            String newConnector = taskId.split("-")[0];
            Map<String, PositionValue> positionTable = positionManagementService.getPositionTable();
            for (Map.Entry<String, PositionValue> entry : positionTable.entrySet()) {
                String[] key = entry.getKey().split("-");
                String existConnector = key[0];
                ByteBuffer existPartition = entry.getValue().getPartition();
                if (newConnector.equals(existConnector) && existPartition == partition) {
                    return entry.getValue().getPosition();
                }
            }
            return null;
        } else {
            log.error("can't get a partition-position");
            return null;
        }
    }

    @Override
    public Map<ByteBuffer, ByteBuffer> getPositions(Collection<ByteBuffer> partitions) {

        Map<ByteBuffer, ByteBuffer> result = new HashMap<>();
        Map<String, PositionValue> allData = positionManagementService.getPositionTable();
        for (ByteBuffer key : partitions) {
            // TODO When do we need to use this method
            for (Map.Entry<String, PositionValue> map : allData.entrySet()) {
                if (map.getValue().getPartition().equals(key)) {
                    result.put(key, map.getValue().getPosition());
                }
            }
        }
        return result;
    }
}
