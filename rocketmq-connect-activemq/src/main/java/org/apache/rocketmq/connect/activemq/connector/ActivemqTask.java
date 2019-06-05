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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.jms.Message;

import org.apache.rocketmq.connect.activemq.Config;
import org.apache.rocketmq.connect.activemq.Replicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.data.SourceDataEntry;
import io.openmessaging.connector.api.source.SourceTask;

public class ActivemqTask extends SourceTask {

    private static final Logger log = LoggerFactory.getLogger(ActivemqTask.class);

    private Replicator replicator;

    private Config config;

    @Override
    public Collection<SourceDataEntry> poll() {

        List<SourceDataEntry> res = new ArrayList<>();

        try {
        	Message message = replicator.getQueue().poll(1000, TimeUnit.MILLISECONDS);
            SourceDataEntry sourceDataEntry = null;
            
            res.add(sourceDataEntry);
        } catch (Exception e) {
            log.error("Mysql task poll error, current config:" + JSON.toJSONString(config), e);
        }
        return res;
    }

    @Override
    public void start(KeyValue props) {

        try {
            this.config = new Config();
            this.config.load(props);
            this.replicator = new Replicator(config);
            this.replicator.start();
        } catch (Exception e) {
            log.error("Mysql task start failed.", e);
        }
    }

    @Override
    public void stop() {
        replicator.stop();
    }

    @Override public void pause() {

    }

    @Override public void resume() {

    }
}
