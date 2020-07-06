
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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.datastax.oss.driver.api.core.CqlSession;
import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.data.DataEntryBuilder;
import io.openmessaging.connector.api.data.EntryType;
import io.openmessaging.connector.api.data.Field;
import io.openmessaging.connector.api.data.Schema;
import io.openmessaging.connector.api.data.SourceDataEntry;
import io.openmessaging.connector.api.source.SourceTask;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;

import org.apache.rocketmq.connect.cassandra.common.ConstDefine;
import org.apache.rocketmq.connect.cassandra.common.DBUtils;
import org.apache.rocketmq.connect.cassandra.config.Config;
import org.apache.rocketmq.connect.cassandra.config.ConfigUtil;
import org.apache.rocketmq.connect.cassandra.schema.Table;
import org.apache.rocketmq.connect.cassandra.schema.column.ColumnParser;
import org.apache.rocketmq.connect.cassandra.source.Querier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraSourceTask extends SourceTask {

    private static final Logger log = LoggerFactory.getLogger(org.apache.rocketmq.connect.cassandra.connector.CassandraSourceTask.class);

    private Config config;

    private DataSource dataSource;

    private CqlSession cqlSession;

    BlockingQueue<Querier> tableQueue = new LinkedBlockingQueue<Querier>();
    static final String INCREMENTING_FIELD = "incrementing";
    static final String TIMESTAMP_FIELD = "timestamp";
    private Querier querier;

    public CassandraSourceTask() {
        this.config = new Config();
    }

    @Override
    public Collection<SourceDataEntry> poll() {
        List<SourceDataEntry> res = new ArrayList<>();
        try {
            if (tableQueue.size() > 1)
                querier = tableQueue.poll(1000, TimeUnit.MILLISECONDS);
            else
                querier = tableQueue.peek();
            Timer timer = new Timer();
            try {
                Thread.currentThread();
                Thread.sleep(1000);//毫秒
            } catch (Exception e) {
                throw e;
            }
            querier.poll();
            for (Table dataRow : querier.getList()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("nextQuery", "database");
                jsonObject.put("nextPosition", "table");
                Schema schema = new Schema();
                schema.setDataSource(dataRow.getDatabase());
                schema.setName(dataRow.getName());
                schema.setFields(new ArrayList<>());
                for (int i = 0; i < dataRow.getColList().size(); i++) {
                    String columnName = dataRow.getColList().get(i);
                    String rawDataType = dataRow.getRawDataTypeList().get(i);
                    Field field = new Field(i, columnName, ColumnParser.mapConnectorFieldType(rawDataType));
                    schema.getFields().add(field);
                }
                DataEntryBuilder dataEntryBuilder = new DataEntryBuilder(schema);
                dataEntryBuilder.timestamp(System.currentTimeMillis()).queue(dataRow.getName())
                        .entryType(EntryType.UPDATE);
                for (int i = 0; i < dataRow.getColList().size(); i++) {
                    Object[] value = new Object[2];
                    value[0] = value[1] = dataRow.getParserList().get(i).getValue(dataRow.getDataList().get(i));
                    dataEntryBuilder.putFiled(dataRow.getColList().get(i), JSONObject.toJSONString(value));
                }

                SourceDataEntry sourceDataEntry = dataEntryBuilder.buildSourceDataEntry(
                        ByteBuffer.wrap((ConstDefine.PREFIX + config.getDbUrl() + config.getDbPort()).getBytes(StandardCharsets.UTF_8)),
                        ByteBuffer.wrap(jsonObject.toJSONString().getBytes(StandardCharsets.UTF_8)));
                res.add(sourceDataEntry);
                log.debug("sourceDataEntry : {}", JSONObject.toJSONString(sourceDataEntry));
            }
        } catch (Exception e) {
            log.error("Cassandra task poll error, current config:" + JSON.toJSONString(config), e);
        }
        log.debug("dataEntry poll successfully,{}", JSONObject.toJSONString(res));
        return res;
    }

    @Override
    public void start(KeyValue props) {
        try {
            ConfigUtil.load(props, this.config);
            cqlSession = DBUtils.initCqlSession(config);
            log.info("init data source success");
        } catch (Exception e) {
            log.error("Cannot start Cassandra Source Task because of configuration error{}", e);
        }
        Map<Map<String, String>, Map<String, Object>> offsets = null;
        String mode = config.getMode();
        if (mode.equals("bulk")) {
            Querier querier = new Querier(config, cqlSession);
            try {
                querier.start();
                tableQueue.add(querier);
            } catch (Exception e) {
                log.error("start querier failed in bulk mode{}", e);
            }
        }

    }

    @Override
    public void stop() {
        try {
            if (cqlSession != null) {
                cqlSession.close();
                log.info("Cassandra source task connection is closed.");
            }
        } catch (Throwable e) {
            log.warn("source task stop error while closing connection to {}", "Cassandra", e);
        }
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }
}
