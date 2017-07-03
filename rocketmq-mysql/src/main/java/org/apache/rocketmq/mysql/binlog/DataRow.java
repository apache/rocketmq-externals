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

import com.google.code.or.common.glossary.Column;
import com.google.code.or.common.glossary.Row;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.rocketmq.mysql.schema.Table;
import org.apache.rocketmq.mysql.schema.column.ColumnParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataRow {
    private Logger logger = LoggerFactory.getLogger(DataRow.class);

    private String type;
    private Table table;
    private Row row;

    public DataRow(String type, Table table, Row row) {
        this.type = type;
        this.table = table;
        this.row = row;
    }

    public Map toMap() {

        try {
            if (table.getColList().size() == row.getColumns().size()) {
                Map<String, Object> dataMap = new HashMap<>();
                List<String> keyList = table.getColList();
                List<ColumnParser> parserList = table.getParserList();
                List<Column> valueList = row.getColumns();

                for (int i = 0; i < keyList.size(); i++) {
                    Object value = valueList.get(i).getValue();
                    ColumnParser parser = parserList.get(i);
                    dataMap.put(keyList.get(i), parser.getValue(value));
                }

                Map<String, Object> map = new HashMap<>();
                map.put("database", table.getDatabase());
                map.put("table", table.getName());
                map.put("type", type);
                map.put("data", dataMap);

                return map;
            } else {
                logger.error("Table schema changed,discard data: {} - {}, {}  {}",
                    table.getDatabase().toUpperCase(), table.getName().toUpperCase(), type, row.toString());

                return null;
            }
        } catch (Exception e) {
            logger.error("Row parse error,discard data: {} - {}, {}  {}",
                table.getDatabase().toUpperCase(), table.getName().toUpperCase(), type, row.toString());
        }

        return null;
    }
}
