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

package org.apache.connect.mongo.connector;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.Task;
import io.openmessaging.connector.api.source.SourceConnector;
import java.util.ArrayList;
import java.util.List;
import org.apache.connect.mongo.SourceTaskConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoSourceConnector extends SourceConnector {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private KeyValue keyValueConfig;

    @Override
    public String verifyAndSetConfig(KeyValue config) {
        for (String requestKey : SourceTaskConfig.REQUEST_CONFIG) {
            if (!config.containsKey(requestKey)) {
                return "Request config key: " + requestKey;
            }
        }
        this.keyValueConfig = config;
        return "";
    }

    @Override
    public void start() {
        logger.info("start mongo source connector:{}", keyValueConfig);
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

    @Override
    public Class<? extends Task> taskClass() {
        return MongoSourceTask.class;
    }

    @Override
    public List<KeyValue> taskConfigs() {
        List<KeyValue> config = new ArrayList<>();
        config.add(this.keyValueConfig);
        return config;
    }
}
