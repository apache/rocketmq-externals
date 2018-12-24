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

import com.alibaba.fastjson.JSONObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.protobuf.generated.CellProtos;

/**
 * This class represents a transaction that contains a fixed amount of HBase rows that are to be pushed together to
 * RocketMQ.
 */
public class Transaction {

    private final int maxTransactionRows;

    private List<DataRow> rows = new LinkedList<>();

    /**
     * Constructor.
     *
     * @param maxTransactionRows number of maximum rows supported by this transaction
     */
    public Transaction(int maxTransactionRows) {
        this.maxTransactionRows = maxTransactionRows;
    }

    /**
     * Adds a row to this transaction.
     *
     * @param tableName the name of the HBase table
     * @param rowKey the row key
     * @param cells the cells
     * @return true if more rows can be added to this transaction; false otherwise.
     */
    public boolean addRow(String tableName, byte[] rowKey, List<Cell> cells) {

        final Cell cell = cells.get(0);
        final CellProtos.CellType type = CellProtos.CellType.valueOf(cell.getTypeByte());
        final String typeStr;
        switch (type) {
            case DELETE:
                typeStr = "DELETE";
                break;
            case DELETE_COLUMN:
                typeStr = "DELETE_COLUMN";
                break;
            case DELETE_FAMILY:
                typeStr = "DELETE_FAMILY";
                break;
            case PUT:
                typeStr = "PUT";
                break;
            default:
                typeStr = null;
        }

        DataRow dataRow = new DataRow(typeStr, tableName, rowKey, cells);
        rows.add(dataRow);

        return rows.size() < maxTransactionRows;
    }

//    private void toRowColumns(final List<Cell> cells) {
//
////        cells.stream().map(cell -> {
////            byte[] family = CellUtil.cloneFamily(cell);
////            byte[] qualifier = CellUtil.cloneQualifier(cell);
////            byte[] value = CellUtil.cloneValue(cell);
////            long timestamp = cell.getTimestamp();
////
////            final HRow.HColumn column = new HRow.HColumn(family, qualifier, value, timestamp);
////            return column;
////        }).collect(toList());
////
////        return columns;
//    }

    /**
     * Converts this transaction to json.
     *
     * @return a string with the json representation of this transaction
     */
    public String toJson() {
        Map<String, Object> map = new HashMap<>();
        List<Map<String, Object>> rowsMap = rows.stream().map(row -> row.toMap()).collect(Collectors.toList());
        map.put("rows", rowsMap);

        return JSONObject.toJSONString(map);
    }
}
