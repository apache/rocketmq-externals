
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

package org.apache.rocketmq.connect.jdbc.connector;

import io.openmessaging.connector.api.source.SourceTask;

import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.rocketmq.connect.jdbc.Config;
import org.apache.rocketmq.connect.jdbc.schema.Table;
import org.apache.rocketmq.connect.jdbc.source.Querier;
import org.apache.rocketmq.connect.jdbc.source.TimestampIncrementingQuerier;
import org.apache.rocketmq.connect.jdbc.schema.column.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.data.EntryType;
import io.openmessaging.connector.api.data.Schema;
import io.openmessaging.connector.api.data.SourceDataEntry;

import com.alibaba.fastjson.JSONObject;
import io.openmessaging.connector.api.data.DataEntryBuilder;
import io.openmessaging.connector.api.data.Field;

public class JdbcSourceTask extends SourceTask {

    private static final Logger log = LoggerFactory.getLogger(JdbcSourceTask.class);

    private Config config;

    BlockingQueue<Querier> tableQueue = new LinkedBlockingQueue<Querier>();
    static final String INCREMENTING_FIELD = "incrementing";
    static final String TIMESTAMP_FIELD = "timestamp";
    private Querier querier;

    @Override
    public Collection<SourceDataEntry> poll() {
        List<SourceDataEntry> res = new ArrayList<>();
        try {
            if (tableQueue.size() > 1)
                querier = tableQueue.poll(1000, TimeUnit.MILLISECONDS);
            else
                querier = tableQueue.peek();
            Timer timer = new java.util.Timer();
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
                        .entryType(EntryType.CREATE);
                for (int i = 0; i < dataRow.getColList().size(); i++) {
                    Object value = dataRow.getDataList().get(i);
                    // System.out.println(dataRow.getColList().get(i) + "|" + value);
                    dataEntryBuilder.putFiled(dataRow.getColList().get(i), value);
                }

                SourceDataEntry sourceDataEntry = dataEntryBuilder.buildSourceDataEntry(
                        ByteBuffer.wrap(config.jdbcUrl.getBytes("UTF-8")),
                        ByteBuffer.wrap(jsonObject.toJSONString().getBytes("UTF-8")));
                res.add(sourceDataEntry);

            }
        } catch (Exception e) {
            log.error("JDBC task poll error, current config:" + JSON.toJSONString(config), e);
        }
        log.info("dataEntry poll successfully,{}", res);
        return res;
    }

    @Override
    public void start(KeyValue props) {
        config = new Config();
        config.load(props);
        
        Map<Map<String, String>, Map<String, Object>> offsets = null;
        String mode = config.mode;
        if (mode.equals("bulk")) {
            Querier querier = new Querier();
            try {
                querier.setConfig(config);
                querier.start();
                tableQueue.add(querier);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                log.error("Start unsuccessfully Because of {}",e);
            }
        } else {
            TimestampIncrementingQuerier querier = new TimestampIncrementingQuerier();
            try {
                querier.setConfig(config);
                querier.start();
                tableQueue.add(querier);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                log.error("Start unsuccessfully Because of {}",e);
            }

        }

    }

    @Override
    public void stop() {
        querier.stop();
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }
}
