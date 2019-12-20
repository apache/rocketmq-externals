package org.apache.rocketmq.connect.jdbc.connector;


import com.alibaba.fastjson.JSONObject;
import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.common.QueueMetaData;
import io.openmessaging.connector.api.data.EntryType;
import io.openmessaging.connector.api.data.Field;
import io.openmessaging.connector.api.data.Schema;
import io.openmessaging.connector.api.data.SinkDataEntry;
import io.openmessaging.connector.api.sink.SinkTask;
import org.apache.rocketmq.connect.jdbc.config.Config;
import org.apache.rocketmq.connect.jdbc.common.DBUtils;
import org.apache.rocketmq.connect.jdbc.config.ConfigUtil;
import org.apache.rocketmq.connect.jdbc.sink.Updater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class JdbcSinkTask extends SinkTask {

    private static final Logger log = LoggerFactory.getLogger(JdbcSinkTask.class);

    private Config config;
    private DataSource dataSource;
    private Connection connection;
    private Updater updater;
    private BlockingQueue<Updater> tableQueue = new LinkedBlockingQueue<Updater>();

    public JdbcSinkTask() {
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

            for (SinkDataEntry record : sinkDataEntries) {
                Map<Field, Object[]> fieldMap = new HashMap<>();
                Object[] payloads = record.getPayload();
                Schema schema = record.getSchema();
                EntryType entryType = record.getEntryType();
                String tableName = schema.getName();
                String dbName = schema.getDataSource();
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
                    Boolean isSuccess = updater.push(dbName, tableName, fieldMap, entryType);
                    if (!isSuccess) {
                        log.error("push data error, dbName:{}, tableName:{}, entryType:{}, fieldMap:{}", dbName, tableName, fieldMap, entryType);
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

    @Override
    public void start(KeyValue props) {
        try {
            ConfigUtil.load(props, this.config);
            dataSource = DBUtils.initDataSource(config);
            connection = dataSource.getConnection();
            log.info("init data source success");
        } catch (Exception e) {
            log.error("Cannot start Jdbc Sink Task because of configuration error{}", e);
        }
        String mode = config.getMode();
        if (mode.equals("bulk")) {
            Updater updater = new Updater(config, connection);
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
            if (connection != null){
                connection.close();
                log.info("jdbc sink task connection is closed.");
            }
        } catch (Throwable e) {
            log.warn("sink task stop error while closing connection to {}", "jdbc", e);
        }
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

}
