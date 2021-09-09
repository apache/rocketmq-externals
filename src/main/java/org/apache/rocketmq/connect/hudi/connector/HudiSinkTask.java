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

package org.apache.rocketmq.connect.hudi.connector;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.common.QueueMetaData;
import io.openmessaging.connector.api.data.SinkDataEntry;
import io.openmessaging.connector.api.sink.SinkTask;
import org.apache.rocketmq.connect.hudi.config.HudiConnectConfig;
import org.apache.rocketmq.connect.hudi.config.ConfigUtil;
import org.apache.rocketmq.connect.hudi.sink.Updater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;


/**
 * In the naming, we are using database for "keyspaces" and table for "columnFamily"
 * This is because we kind of want the abstract data source to be aligned with SQL databases
 */
public class HudiSinkTask extends SinkTask {
    private static final Logger log = LoggerFactory.getLogger(HudiSinkTask.class);

    private HudiConnectConfig hudiConnectConfig;
    private Updater updater;

    public HudiSinkTask() {
        this.hudiConnectConfig = new HudiConnectConfig();
    }

    @Override
    public void put(Collection<SinkDataEntry> sinkDataEntries) {
        try {
            log.info("Hudi Sink Task trying to put()");
            for (SinkDataEntry record : sinkDataEntries) {
                log.info("Hudi Sink Task trying to call updater.push()");
                Boolean isSuccess = updater.push(record);
                if (!isSuccess) {
                    log.error("Hudi sink push data error, record:{}", record);
                }
                log.debug("Hudi pushed data : " + record);
            }
        } catch (Exception e) {
            log.error("put sinkDataEntries error, {}", e);
        }
    }

    @Override
    public void commit(Map<QueueMetaData, Long> map) {

    }

    /**
     * Remember always close the CqlSession according to
     * https://docs.datastax.com/en/developer/java-driver/4.5/manual/core/
     * @param props
     */
    @Override
    public void start(KeyValue props) {
        try {
            ConfigUtil.load(props, this.hudiConnectConfig);
            log.info("init data source success");
        } catch (Exception e) {
            log.error("Cannot start Hudi Sink Task because of configuration error{}", e);
        }
        try {
            updater = new Updater(hudiConnectConfig);
            updater.start();
        } catch (Throwable e) {
            log.error("fail to start updater{}", e);
        }

    }

    @Override
    public void stop() {
        try {
            updater.stop();
            log.info("hudi sink task connection is closed.");
        } catch (Throwable e) {
            log.warn("sink task stop error while closing connection to {}", "hudi", e);
        }
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }
}
