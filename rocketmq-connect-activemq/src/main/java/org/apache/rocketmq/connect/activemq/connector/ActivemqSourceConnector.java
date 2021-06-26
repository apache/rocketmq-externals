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

package org.apache.rocketmq.connect.activemq.connector;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.Task;
import io.openmessaging.connector.api.source.SourceConnector;
import java.util.ArrayList;
import java.util.List;
import org.apache.rocketmq.connect.activemq.Config;

public class ActivemqSourceConnector extends SourceConnector {

    private KeyValue config;

    @Override
    public String verifyAndSetConfig(KeyValue config) {

        for (String requestKey : Config.REQUEST_CONFIG) {
            if (!config.containsKey(requestKey)) {
                return "Request config key: " + requestKey;
            }
        }
        this.config = config;
        return "";
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override public void pause() {

    }

    @Override public void resume() {

    }

    @Override
    public Class<? extends Task> taskClass() {
        return ActivemqSourceTask.class;
    }

    @Override
    public List<KeyValue> taskConfigs() {
        List<KeyValue> config = new ArrayList<>();
        config.add(this.config);
        return config;
    }
}
