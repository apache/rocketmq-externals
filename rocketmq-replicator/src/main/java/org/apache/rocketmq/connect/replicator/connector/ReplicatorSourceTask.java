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

package org.apache.rocketmq.connect.replicator.connector;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.data.SourceDataEntry;
import io.openmessaging.connector.api.exception.DataConnectException;
import io.openmessaging.connector.api.source.SourceTask;
import java.util.Collection;
import org.apache.rocketmq.connect.replicator.Config;
import org.apache.rocketmq.connect.replicator.ErrorCode;
import org.apache.rocketmq.connect.replicator.Replicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplicatorSourceTask extends SourceTask {

    private static final Logger log = LoggerFactory.getLogger(ReplicatorSourceTask.class);

    private Replicator replicator;

    private Config config;

    @Override
    public void start(KeyValue config) {
        try {
            this.config = new Config();
            this.config.load(config);

            this.replicator = new Replicator(this.config);
            this.replicator.start();
        } catch (Exception e) {
            log.error("activemq task start failed.", e);
            throw new DataConnectException(ErrorCode.START_ERROR_CODE, e.getMessage(), e);
        }

    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub

    }

    @Override
    public void pause() {
        // TODO Auto-generated method stub

    }

    @Override
    public void resume() {

    }

    @Override
    public Collection<SourceDataEntry> poll() {

        return null;
    }

}
