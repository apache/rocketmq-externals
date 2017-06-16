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

package org.apache.rocketmq.mysql.schema;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.rocketmq.mysql.binlog.EventProcessor;
import org.apache.rocketmq.mysql.schema.column.ColumnParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Database {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventProcessor.class);

    private static final String SQL = "select table_name,column_name,data_type,column_type,character_set_name " +
        "from information_schema.columns " +
        "where table_schema = ?";
    private String name;

    private DataSource dataSource;

    private Map<String, Table> tableMap = new HashMap<String, Table>();

    public Database(String name, DataSource dataSource) {
        this.name = name;
        this.dataSource = dataSource;
    }

    public void init() throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();

            ps = conn.prepareStatement(SQL);
            ps.setString(1, name);
            rs = ps.executeQuery();

            while (rs.next()) {
                String tableName = rs.getString(1);
                String colName = rs.getString(2);
                String dataType = rs.getString(3);
                String colType = rs.getString(4);
                String charset = rs.getString(5);

                ColumnParser columnParser = ColumnParser.getColumnParser(dataType, colType, charset);

                if (!tableMap.containsKey(tableName)) {
                    addTable(tableName);
                }
                Table table = tableMap.get(tableName);
                table.addCol(colName);
                table.addParser(columnParser);
            }

        } finally {
            if (conn != null) {
                conn.close();
            }
            if (ps != null) {
                ps.close();
            }
            if (rs != null) {
                rs.close();
            }
        }

    }

    private void addTable(String tableName) {

        LOGGER.info("Schema load -- DATABASE:{},\tTABLE:{}", name, tableName);

        Table table = new Table(name, tableName);
        tableMap.put(tableName, table);
    }

    public Table getTable(String tableName) {

        return tableMap.get(tableName);
    }
}
