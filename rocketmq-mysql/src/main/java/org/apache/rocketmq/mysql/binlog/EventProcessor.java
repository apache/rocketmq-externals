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
import com.google.code.or.OpenReplicator;
import com.google.code.or.binlog.BinlogEventV4;
import com.google.code.or.binlog.impl.event.DeleteRowsEvent;
import com.google.code.or.binlog.impl.event.DeleteRowsEventV2;
import com.google.code.or.binlog.impl.event.QueryEvent;
import com.google.code.or.binlog.impl.event.TableMapEvent;
import com.google.code.or.binlog.impl.event.UpdateRowsEvent;
import com.google.code.or.binlog.impl.event.UpdateRowsEventV2;
import com.google.code.or.binlog.impl.event.WriteRowsEvent;
import com.google.code.or.binlog.impl.event.WriteRowsEventV2;
import com.google.code.or.binlog.impl.event.XidEvent;
import com.google.code.or.common.glossary.Pair;
import com.google.code.or.common.glossary.Row;
import com.google.code.or.common.util.MySQLConstants;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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

    private BlockingQueue<BinlogEventV4> queue = new LinkedBlockingQueue<>(100);

    private OpenReplicator openReplicator;

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
        openReplicator = new OpenReplicator();
        openReplicator.setBinlogEventListener(eventListener);
        openReplicator.setHost(config.mysqlAddr);
        openReplicator.setPort(config.mysqlPort);
        openReplicator.setUser(config.mysqlUsername);
        openReplicator.setPassword(config.mysqlPassword);
        openReplicator.setStopOnEOF(false);
        openReplicator.setHeartbeatPeriod(1f);
        openReplicator.setLevel2BufferSize(50 * 1024 * 1024);
        openReplicator.setServerId(1001);
        openReplicator.setBinlogFileName(binlogPositionManager.getBinlogFilename());
        openReplicator.setBinlogPosition(binlogPositionManager.getPosition());
        openReplicator.start();

        LOGGER.info("Started.");

        doProcess();
    }

    private void doProcess() {

        while (true) {

            try {
                BinlogEventV4 event = queue.take();

                switch (event.getHeader().getEventType()) {
                    case MySQLConstants.TABLE_MAP_EVENT:
                        processTableMapEvent(event);
                        break;

                    case MySQLConstants.WRITE_ROWS_EVENT:
                        processWriteEvent(event);
                        break;

                    case MySQLConstants.WRITE_ROWS_EVENT_V2:
                        processWriteEventV2(event);
                        break;

                    case MySQLConstants.UPDATE_ROWS_EVENT:
                        processUpdateEvent(event);
                        break;

                    case MySQLConstants.UPDATE_ROWS_EVENT_V2:
                        processUpdateEventV2(event);
                        break;

                    case MySQLConstants.DELETE_ROWS_EVENT:
                        processDeleteEvent(event);
                        break;

                    case MySQLConstants.DELETE_ROWS_EVENT_V2:
                        processDeleteEventV2(event);
                        break;

                    case MySQLConstants.QUERY_EVENT:
                        processQueryEvent(event);
                        break;

                    case MySQLConstants.XID_EVENT:
                        processXidEvent(event);
                        break;

                }
            } catch (Exception e) {
                LOGGER.error("Binlog process error.", e);
            }

        }
    }

    private void processTableMapEvent(BinlogEventV4 event) {

        TableMapEvent tableMapEvent = (TableMapEvent) event;
        String dbName = tableMapEvent.getDatabaseName().toString();
        String tableName = tableMapEvent.getTableName().toString();
        Long tableId = tableMapEvent.getTableId();

        Table table = schema.getTable(dbName, tableName);

        tableMap.put(tableId, table);
    }

    private void processWriteEvent(BinlogEventV4 event) {

        WriteRowsEvent writeRowsEvent = (WriteRowsEvent) event;
        Long tableId = writeRowsEvent.getTableId();
        List<Row> list = writeRowsEvent.getRows();

        for (Row row : list) {
            addRow("WRITE", tableId, row);
        }
    }

    private void processWriteEventV2(BinlogEventV4 event) {

        WriteRowsEventV2 writeRowsEventV2 = (WriteRowsEventV2) event;
        Long tableId = writeRowsEventV2.getTableId();
        List<Row> list = writeRowsEventV2.getRows();

        for (Row row : list) {
            addRow("WRITE", tableId, row);
        }

    }

    private void processUpdateEvent(BinlogEventV4 event) {

        UpdateRowsEvent updateRowsEvent = (UpdateRowsEvent) event;
        Long tableId = updateRowsEvent.getTableId();
        List<Pair<Row>> list = updateRowsEvent.getRows();

        for (Pair<Row> pair : list) {
            addRow("UPDATE", tableId, pair.getAfter());
        }
    }

    private void processUpdateEventV2(BinlogEventV4 event) {

        UpdateRowsEventV2 updateRowsEventV2 = (UpdateRowsEventV2) event;
        Long tableId = updateRowsEventV2.getTableId();
        List<Pair<Row>> list = updateRowsEventV2.getRows();

        for (Pair<Row> pair : list) {
            addRow("UPDATE", tableId, pair.getAfter());
        }
    }

    private void processDeleteEvent(BinlogEventV4 event) {

        DeleteRowsEvent deleteRowsEvent = (DeleteRowsEvent) event;
        Long tableId = deleteRowsEvent.getTableId();
        List<Row> list = deleteRowsEvent.getRows();

        for (Row row : list) {
            addRow("DELETE", tableId, row);
        }

    }

    private void processDeleteEventV2(BinlogEventV4 event) {

        DeleteRowsEventV2 deleteRowsEventV2 = (DeleteRowsEventV2) event;
        Long tableId = deleteRowsEventV2.getTableId();
        List<Row> list = deleteRowsEventV2.getRows();

        for (Row row : list) {
            addRow("DELETE", tableId, row);
        }

    }

    private static Pattern createTablePattern =
        Pattern.compile("^(CREATE|ALTER)\\s+TABLE", Pattern.CASE_INSENSITIVE);

    private void processQueryEvent(BinlogEventV4 event) {

        QueryEvent queryEvent = (QueryEvent) event;
        String sql = queryEvent.getSql().toString();

        if (createTablePattern.matcher(sql).find()) {
            schema.reset();
        }
    }

    private void processXidEvent(BinlogEventV4 event) {

        XidEvent xidEvent = (XidEvent) event;
        String binlogFilename = xidEvent.getBinlogFilename();
        Long position = xidEvent.getHeader().getNextPosition();
        Long xid = xidEvent.getXid();

        BinlogPosition binlogPosition = new BinlogPosition(binlogFilename, position);
        transaction.setNextBinlogPosition(binlogPosition);
        transaction.setXid(xid);

        replicator.commit(transaction, true);

        transaction = new Transaction(this);

    }

    private void addRow(String type, Long tableId, Row row) {

        if (transaction == null) {
            transaction = new Transaction(this);
        }

        Table t = tableMap.get(tableId);
        if (t != null) {

            while (true) {
                if (transaction.addRow(type, t, row)) {
                    break;

                } else {
                    transaction.setNextBinlogPosition(replicator.getNextBinlogPosition());
                    replicator.commit(transaction, false);
                    transaction = new Transaction(this);
                }
            }

        }
    }

    private void initDataSource() throws Exception {
        Map<String,String> map = new HashMap<>();
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
