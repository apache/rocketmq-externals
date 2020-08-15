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
package org.apache.rocketmq.connect.file;

import io.openmessaging.KeyValue;
import java.util.HashSet;
import java.util.Set;

public class FileConfig {

    public static final String TOPIC_CONFIG = "topic";
    public static final String FILE_CONFIG = "filename";
    public static final String TASK_BATCH_SIZE_CONFIG = "batch.size";
    public static final int DEFAULT_TASK_BATCH_SIZE = 2000;

    private String filename;
    private String topic;
    private int batchSize;
    public Long nextPosition;

    public static final Set<String> REQUEST_CONFIG = new HashSet<String>() {
        {
            add(FILE_CONFIG);
        }
    };

    public void load(KeyValue props) {
        FileUtils.properties2Object(props, this);
    }

    public static Set<String> getRequestConfig() {
        return REQUEST_CONFIG;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public Long getNextPosition() {
        return nextPosition;
    }

    public void setNextPosition(Long nextPosition) {
        this.nextPosition = nextPosition;
    }
}