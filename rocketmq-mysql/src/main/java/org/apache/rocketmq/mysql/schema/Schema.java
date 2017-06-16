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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.rocketmq.mysql.binlog.EventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Schema {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventProcessor.class);

    private static final String SQL = "select schema_name from information_schema.schemata";

    private static final List<String> IGNORED_DATABASES = new ArrayList<>(
        Arrays.asList(new String[] {"information_schema", "mysql", "performance_schema", "sys"})
    );

    private DataSource dataSource;

    private Map<String, Database> dbMap;

    public Schema(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void load() throws SQLException {

        dbMap = new HashMap<>();

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();

            ps = conn.prepareStatement(SQL);
            rs = ps.executeQuery();

            while (rs.next()) {
                String dbName = rs.getString(1);
                if (!IGNORED_DATABASES.contains(dbName)) {
                    Database database = new Database(dbName, dataSource);
                    dbMap.put(dbName, database);
                }
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

        for (Database db : dbMap.values()) {
            db.init();
        }

    }

    public Table getTable(String dbName, String tableName) {

        if (dbMap == null) {
            reload();
        }

        Database database = dbMap.get(dbName);
        if (database == null) {
            return null;
        }

        Table table = database.getTable(tableName);
        if (table == null) {
            return null;
        }

        return table;
    }

    private void reload() {

        while (true) {
            try {
                load();
                break;
            } catch (Exception e) {
                LOGGER.error("Reload schema error.", e);
            }
        }
    }

    public void reset() {
        dbMap = null;
    }
}
