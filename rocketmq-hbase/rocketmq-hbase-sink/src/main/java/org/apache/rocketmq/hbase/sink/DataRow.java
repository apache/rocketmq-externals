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
package org.apache.rocketmq.hbase.sink;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

/**
 * This class represents a HBase data row.
 */
public class DataRow {
    private static final Logger logger = LoggerFactory.getLogger(DataRow.class);

    private String type;

    private String table;

    private byte[] row;

    private List<Cell> cells;

    /**
     * Constructor.
     *
     * @param type the type of operation, put or delete
     * @param table the table
     * @param row the row key
     * @param cells the cells
     */
    public DataRow(String type, String table, byte[] row, List<Cell> cells) {
        this.type = type;
        this.table = table;
        this.row = row;
        this.cells = cells;
    }

    /**
     * Converts this data row to a map.
     *
     * @return a map structure containing a representation of this row
     */
    public Map<String, Object> toMap() {
        final Map<String, Object> dataMap = new HashMap<>();

        final Map<byte[], List<Cell>> columnsByFamily = cells.stream().collect(groupingBy(CellUtil::cloneFamily));

        for(Map.Entry<byte[], List<Cell>> entry : columnsByFamily.entrySet()) {
            final String columnFamily = Bytes.toString(entry.getKey());
            final List<Cell> cells = entry.getValue();

            final Map<String, Object> qualifiers = new HashMap<>();
            for (Cell cell : cells) {
                if (cell.getQualifierLength() > 0) {
                    final String columnQualifier = Bytes.toString(CellUtil.cloneQualifier(cell));
                    qualifiers.put(columnQualifier, CellUtil.cloneValue(cell));
                } else {
                    dataMap.put(columnFamily, CellUtil.cloneValue(cell));
                }
            }
            if (qualifiers.size() > 0) {
                dataMap.put(columnFamily, qualifiers);
            }
        }

        Map<String, Object> map = new HashMap<>();
        map.put("table", table);
        map.put("type", type);
        map.put("row", Bytes.toString(row));
        map.put("data", dataMap);

        return map;
    }
}
