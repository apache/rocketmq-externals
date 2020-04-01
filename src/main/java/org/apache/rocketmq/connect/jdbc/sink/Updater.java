package org.apache.rocketmq.connect.jdbc.sink;

import io.openmessaging.connector.api.data.EntryType;
import io.openmessaging.connector.api.data.Field;
import io.openmessaging.connector.api.data.FieldType;
import org.apache.rocketmq.connect.jdbc.config.Config;
import org.apache.rocketmq.connect.jdbc.schema.Schema;
import org.apache.rocketmq.connect.jdbc.schema.column.DateTimeColumnParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Updater {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Queue<Connection> connections = new ConcurrentLinkedQueue<>();
    private Config config;
    private Schema schema;
    private Connection connection;

    public Updater(Config config, Connection connection) {
        this.config = config;
        this.connection = connection;
        this.schema = new Schema(connection);
    }

    public boolean push(String dbName, String tableName, Map<Field, Object[]> fieldMap, EntryType entryType) {
        Boolean isSuccess = false;
        int beforeUpdateId = 0;
        int afterUpdateId = 0;
        switch (entryType) {
            case CREATE:
                afterUpdateId = queryAfterUpdateRowId(dbName, tableName, fieldMap);
                if (afterUpdateId != 0){
                    isSuccess = true;
                    break;
                }
                isSuccess = updateRow(dbName, tableName, fieldMap, beforeUpdateId);
                break;
            case UPDATE:
                afterUpdateId = queryAfterUpdateRowId(dbName, tableName, fieldMap);
                if (afterUpdateId != 0){
                    isSuccess = true;
                    // 再查原有数据是否存在,存在则删除
                    beforeUpdateId = queryBeforeUpdateRowId(dbName, tableName, fieldMap);
                    if (beforeUpdateId != 0 && afterUpdateId != beforeUpdateId){
                       isSuccess = deleteRow(dbName, tableName, beforeUpdateId);
                    }
                    break;
                }

                beforeUpdateId = queryBeforeUpdateRowId(dbName, tableName, fieldMap);
                isSuccess = updateRow(dbName, tableName, fieldMap, beforeUpdateId);
                break;
            case DELETE:
                beforeUpdateId = queryBeforeUpdateRowId(dbName, tableName, fieldMap);
                isSuccess = deleteRow(dbName, tableName, beforeUpdateId);
                break;
            default:
                log.error("entryType {} is illegal.", entryType.toString());
        }
        return isSuccess;
    }

    public void start() throws Exception {
        schema.load();
        log.info("schema load success");
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    private String typeParser(FieldType fieldType, String fieldName, Object fieldValue, String sql) {
        switch (fieldType) {
            case STRING:
                sql += fieldName + " = " + "'" + fieldValue + "'";
                break;
            case DATETIME:
                sql += fieldName + " = " + "'" + new DateTimeColumnParser().getValue(fieldValue) + "'";
                break;
            case INT32:
            case INT64:
            case FLOAT32:
            case FLOAT64:
            case BIG_INTEGER:
                sql += fieldName + " = " + fieldValue;
                break;
            default:
                log.error("fieldType {} is illegal.", fieldType.toString());
        }
        return sql;
    }

    private Integer queryBeforeUpdateRowId(String dbName, String tableName, Map<Field, Object[]> fieldMap) {
        int count = 0, id = 0;
        ResultSet rs;
        PreparedStatement stmt;
        Boolean finishQuery = false;
        String query = "select id from " + dbName + "." + tableName + " where 1=1";

        for (Map.Entry<Field, Object[]> entry : fieldMap.entrySet()) {
            count ++;
            String fieldName = entry.getKey().getName();
            FieldType fieldType = entry.getKey().getType();
            Object fieldValue = entry.getValue()[0];
            if ("id".equals(fieldName))
                continue;
            if (count <=fieldMap.size()) {
                query += " and ";
            }
            if (fieldValue == null)
            {
                query += fieldName + " is NULL";
            } else {
                query = typeParser(fieldType, fieldName, fieldValue, query);
            }
        }

        try {
            while (!connection.isClosed() && !finishQuery){
                stmt = connection.prepareStatement(query);
                rs = stmt.executeQuery();
                if (rs != null) {
                    while (rs.next()) {
                        id = rs.getInt("id");
                    }
                    finishQuery = true;
                    rs.close();
                }
            }
        } catch (SQLException e) {
            log.error("query table error,{}", e);
        }
        return id;
    }

    private Integer queryAfterUpdateRowId(String dbName, String tableName, Map<Field, Object[]> fieldMap) {
        int count = 0, id = 0;
        ResultSet rs;
        PreparedStatement stmt;
        Boolean finishQuery = false;
        String query = "select id from " + dbName + "." + tableName + " where 1=1";

        for (Map.Entry<Field, Object[]> entry : fieldMap.entrySet()) {
            count ++;
            String fieldName = entry.getKey().getName();
            FieldType fieldType = entry.getKey().getType();
            Object fieldValue = entry.getValue()[1];
            if ("id".equals(fieldName))
                continue;
            if (count <=fieldMap.size()) {
                query += " and ";
            }
            if (fieldValue == null)
            {
                query += fieldName + " is NULL";
            } else {
                query = typeParser(fieldType, fieldName, fieldValue, query);
            }
        }

        try {
            while (!connection.isClosed() && !finishQuery){
                stmt = connection.prepareStatement(query);
                rs = stmt.executeQuery();
                if (rs != null) {
                    while (rs.next()) {
                        id = rs.getInt("id");
                    }
                    finishQuery = true;
                    rs.close();
                }
            }
        } catch (SQLException e) {
            log.error("query table error,{}", e);
        }
        return id;
    }

    private Boolean updateRow(String dbName, String tableName, Map<Field, Object[]> fieldMap, Integer id) {
        int count = 0;
        PreparedStatement stmt;
        boolean finishUpdate = false;
        String update = "replace into " + dbName + "." + tableName + " set ";

        for (Map.Entry<Field, Object[]> entry : fieldMap.entrySet()) {
            count++;
            String fieldName = entry.getKey().getName();
            FieldType fieldType = entry.getKey().getType();
            Object fieldValue = entry.getValue()[1];
            if ("id".equals(fieldName)) {
                if (id == 0){
                    if(count==fieldMap.size()) update = update.substring(0,update.length()-1);
                    continue;
                }else{
                    fieldValue = id;
                }
            }
            if (fieldValue == null) {
                update += fieldName + " = NULL";
            } else {
                update = typeParser(fieldType, fieldName, fieldValue, update);
            }
            if(count<fieldMap.size()){
                update += ",";
            }
        }

        try {
            while (!connection.isClosed() && !finishUpdate){
                stmt = connection.prepareStatement(update);
                int result = stmt.executeUpdate();
                if (result > 0) {
                    log.info("replace into table success");
                    return true;
                }
                finishUpdate = true;
                stmt.close();
            }
        } catch (SQLException e) {
            log.error("update table error,{}", e);
        }
        return false;
    }

    private Boolean deleteRow(String dbName, String tableName, Integer id) {
        PreparedStatement stmt;
        String delete = "delete from " + dbName + "." + tableName + " where id = " + id ;
        boolean finishDelete = false;
        try {
            while (!connection.isClosed() && !finishDelete){
                stmt = connection.prepareStatement(delete);
                int result = stmt.executeUpdate();
                if (result > 0) {
                    log.info("delete from table success");
                    return true;
                }
                finishDelete = true;
                stmt.close();
            }
        } catch (SQLException e) {
            log.error("delete from table error,{}", e);
        }
        return false;
    }

}
