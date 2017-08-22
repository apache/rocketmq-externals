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

package org.apache.rocketmq.mysql.binlog;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.DeleteRowsEventData;
import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.EventHeaderV4;
import com.github.shyiko.mysql.binlog.event.QueryEventData;
import com.github.shyiko.mysql.binlog.event.TableMapEventData;
import com.github.shyiko.mysql.binlog.event.UpdateRowsEventData;
import com.github.shyiko.mysql.binlog.event.WriteRowsEventData;
import com.github.shyiko.mysql.binlog.event.XidEventData;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.apache.rocketmq.mysql.Config;
import org.apache.rocketmq.mysql.Replicator;
import org.apache.rocketmq.mysql.position.BinlogPosition;
import org.apache.rocketmq.mysql.position.BinlogPositionManager;
import org.apache.rocketmq.mysql.schema.Schema;
import org.apache.rocketmq.mysql.schema.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventProcessor.class);

    private Replicator replicator;
    private Config config;

    private DataSource dataSource;

    private BinlogPositionManager binlogPositionManager;

    private BlockingQueue<Event> queue = new LinkedBlockingQueue<>(100);

    private BinaryLogClient binaryLogClient;

    private EventListener eventListener;

    private Schema schema;

    private Map<Long, Table> tableMap = new HashMap<>();

    private Transaction transaction;

    public EventProcessor(Replicator replicator) {

        this.replicator = replicator;
        this.config = replicator.getConfig();
    }

    public void start() throws Exception {

        initDataSource();

        binlogPositionManager = new BinlogPositionManager(config, dataSource);
        binlogPositionManager.initBeginPosition();

        schema = new Schema(dataSource);
        schema.load();

        eventListener = new EventListener(queue);
        binaryLogClient = new BinaryLogClient(config.mysqlAddr,
            config.mysqlPort,
            config.mysqlUsername,
            config.mysqlPassword);
        binaryLogClient.setBlocking(true);
        binaryLogClient.setServerId(1001);

        EventDeserializer eventDeserializer = new EventDeserializer();
        eventDeserializer.setCompatibilityMode(EventDeserializer.CompatibilityMode.DATE_AND_TIME_AS_LONG,
            EventDeserializer.CompatibilityMode.CHAR_AND_BINARY_AS_BYTE_ARRAY);
        binaryLogClient.setEventDeserializer(eventDeserializer);
        binaryLogClient.registerEventListener(eventListener);
        binaryLogClient.setBinlogFilename(binlogPositionManager.getBinlogFilename());
        binaryLogClient.setBinlogPosition(binlogPositionManager.getPosition());

        binaryLogClient.connect(3000);

        LOGGER.info("Started.");

        doProcess();
    }

    private void doProcess() {

        while (true) {

            try {
                Event event = queue.poll(1000, TimeUnit.MILLISECONDS);
                if (event == null) {
                    checkConnection();
                    continue;
                }

                switch (event.getHeader().getEventType()) {
                    case TABLE_MAP:
                        processTableMapEvent(event);
                        break;

                    case WRITE_ROWS:
                    case EXT_WRITE_ROWS:
                        processWriteEvent(event);
                        break;

                    case UPDATE_ROWS:
                    case EXT_UPDATE_ROWS:
                        processUpdateEvent(event);
                        break;

                    case DELETE_ROWS:
                    case EXT_DELETE_ROWS:
                        processDeleteEvent(event);
                        break;

                    case QUERY:
                        processQueryEvent(event);
                        break;

                    case XID:
                        processXidEvent(event);
                        break;

                }
            } catch (Exception e) {
                LOGGER.error("Binlog process error.", e);
            }

        }
    }

    private void checkConnection() throws Exception {

        if (!binaryLogClient.isConnected()) {
            BinlogPosition binlogPosition = replicator.getNextBinlogPosition();
            if (binlogPosition != null) {
                binaryLogClient.setBinlogFilename(binlogPosition.getBinlogFilename());
                binaryLogClient.setBinlogPosition(binlogPosition.getPosition());
            }

            binaryLogClient.connect(3000);
        }
    }

    private void processTableMapEvent(Event event) {
        TableMapEventData data = event.getData();
        String dbName = data.getDatabase();
        String tableName = data.getTable();
        Long tableId = data.getTableId();

        Table table = schema.getTable(dbName, tableName);

        tableMap.put(tableId, table);
    }

    private void processWriteEvent(Event event) {
        WriteRowsEventData data = event.getData();
        Long tableId = data.getTableId();
        List<Serializable[]> list = data.getRows();

        for (Serializable[] row : list) {
            addRow("WRITE", tableId, row);
        }
    }

    private void processUpdateEvent(Event event) {
        UpdateRowsEventData data = event.getData();
        Long tableId = data.getTableId();
        List<Map.Entry<Serializable[], Serializable[]>> list = data.getRows();

        for (Map.Entry<Serializable[], Serializable[]> entry : list) {
            addRow("UPDATE", tableId, entry.getValue());
        }
    }

    private void processDeleteEvent(Event event) {
        DeleteRowsEventData data = event.getData();
        Long tableId = data.getTableId();
        List<Serializable[]> list = data.getRows();

        for (Serializable[] row : list) {
            addRow("DELETE", tableId, row);
        }

    }

    private static Pattern createTablePattern =
        Pattern.compile("^(CREATE|ALTER)\\s+TABLE", Pattern.CASE_INSENSITIVE);

    private void processQueryEvent(Event event) {
        QueryEventData data = event.getData();
        String sql = data.getSql();

        if (createTablePattern.matcher(sql).find()) {
            schema.reset();
        }
    }

    private void processXidEvent(Event event) {
        EventHeaderV4 header = event.getHeader();
        XidEventData data = event.getData();

        String binlogFilename = binaryLogClient.getBinlogFilename();
        Long position = header.getNextPosition();
        Long xid = data.getXid();

        BinlogPosition binlogPosition = new BinlogPosition(binlogFilename, position);
        transaction.setNextBinlogPosition(binlogPosition);
        transaction.setXid(xid);

        replicator.commit(transaction, true);

        transaction = new Transaction(config);
    }

    private void addRow(String type, Long tableId, Serializable[] row) {

        if (transaction == null) {
            transaction = new Transaction(config);
        }

        Table t = tableMap.get(tableId);
        if (t != null) {

            while (true) {
                if (transaction.addRow(type, t, row)) {
                    break;

                } else {
                    transaction.setNextBinlogPosition(replicator.getNextBinlogPosition());
                    replicator.commit(transaction, false);
                    transaction = new Transaction(config);
                }
            }

        }
    }

    private void initDataSource() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("driverClassName", "com.mysql.jdbc.Driver");
        map.put("url", "jdbc:mysql://" + config.mysqlAddr + ":" + config.mysqlPort + "?useSSL=true&verifyServerCertificate=false");
        map.put("username", config.mysqlUsername);
        map.put("password", config.mysqlPassword);
        map.put("initialSize", "2");
        map.put("maxActive", "2");
        map.put("maxWait", "60000");
        map.put("timeBetweenEvictionRunsMillis", "60000");
        map.put("minEvictableIdleTimeMillis", "300000");
        map.put("validationQuery", "SELECT 1 FROM DUAL");
        map.put("testWhileIdle", "true");

        dataSource = DruidDataSourceFactory.createDataSource(map);
    }

    public Config getConfig() {
        return config;
    }

}
