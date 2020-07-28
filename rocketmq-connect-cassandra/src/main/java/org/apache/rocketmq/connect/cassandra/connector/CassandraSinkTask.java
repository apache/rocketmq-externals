
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

package org.apache.rocketmq.connect.cassandra.connector;

import com.alibaba.fastjson.JSONObject;

import com.datastax.oss.driver.api.core.CqlSession;
import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.common.QueueMetaData;
import io.openmessaging.connector.api.data.EntryType;
import io.openmessaging.connector.api.data.Field;
import io.openmessaging.connector.api.data.Schema;
import io.openmessaging.connector.api.data.SinkDataEntry;
import io.openmessaging.connector.api.sink.SinkTask;
import org.apache.rocketmq.connect.cassandra.config.Config;
import org.apache.rocketmq.connect.cassandra.common.DBUtils;
import org.apache.rocketmq.connect.cassandra.config.ConfigUtil;
import org.apache.rocketmq.connect.cassandra.sink.Updater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * In the naming, we are using database for "keyspaces" and table for "columnFamily"
 * This is because we kind of want the abstract data source to be aligned with SQL databases
 */
public class CassandraSinkTask extends SinkTask {
    private static final Logger log = LoggerFactory.getLogger(CassandraSinkTask.class);

    private Config config;

    private CqlSession cqlSession;
    private Updater updater;
    private BlockingQueue<Updater> tableQueue = new LinkedBlockingQueue<Updater>();

    public CassandraSinkTask() {
        this.config = new Config();
    }

    @Override
    public void put(Collection<SinkDataEntry> sinkDataEntries) {
        try {
            if (tableQueue.size() > 1) {
                updater = tableQueue.poll(1000, TimeUnit.MILLISECONDS);
            } else {
                updater = tableQueue.peek();
            }
            log.info("Cassandra Sink Task trying to put()");
            for (SinkDataEntry record : sinkDataEntries) {
                Map<Field, Object[]> fieldMap = new HashMap<>();
                Object[] payloads = record.getPayload();
                Schema schema = record.getSchema();
                EntryType entryType = record.getEntryType();
                String cfName = schema.getName();
                String keyspaceName = schema.getDataSource();
                List<Field> fields = schema.getFields();
                Boolean parseError = false;
                if (!fields.isEmpty()) {
                    for (Field field : fields) {
                        Object fieldValue = payloads[field.getIndex()];
                        Object[] value = JSONObject.parseArray((String)fieldValue).toArray();
                        if (value.length == 2) {
                            fieldMap.put(field, value);
                        } else {
                            log.error("parseArray error, fieldValue:{}", fieldValue);
                            parseError = true;
                        }
                    }
                }
                if (!parseError) {
                    log.info("Cassandra Sink Task trying to call updater.push()");
                    Boolean isSuccess = updater.push(keyspaceName, cfName, fieldMap, entryType);
                    if (!isSuccess) {
                        log.error("push data error, keyspaceName:{}, cfName:{}, entryType:{}, fieldMap:{}", keyspaceName, cfName, fieldMap, entryType);
                    }
                }
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
            ConfigUtil.load(props, this.config);
            cqlSession = DBUtils.initCqlSession(config);
            log.info("init data source success");
        } catch (Exception e) {
            log.error("Cannot start Cassandra Sink Task because of configuration error{}", e);
        }
        String mode = config.getMode();
        if (mode.equals("bulk")) {
            Updater updater = new Updater(config, cqlSession);
            try {
                updater.start();
                tableQueue.add(updater);
            } catch (Exception e) {
                log.error("fail to start updater{}", e);
            }
        }

    }

    @Override
    public void stop() {
        try {
            if (cqlSession != null){
                cqlSession.close();
                log.info("cassandra sink task connection is closed.");
            }
        } catch (Throwable e) {
            log.warn("sink task stop error while closing connection to {}", "cassandra", e);
        }
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }
}
