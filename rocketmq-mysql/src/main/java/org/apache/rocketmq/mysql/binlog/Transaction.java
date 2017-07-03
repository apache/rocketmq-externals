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

import com.alibaba.fastjson.JSONObject;
import com.google.code.or.common.glossary.Row;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.rocketmq.mysql.Config;
import org.apache.rocketmq.mysql.position.BinlogPosition;
import org.apache.rocketmq.mysql.schema.Table;

public class Transaction {
    private BinlogPosition nextBinlogPosition;
    private Long xid;

    private EventProcessor eventProcessor;
    private Config config;

    private List<DataRow> list = new LinkedList<>();

    public Transaction(EventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
        this.config = eventProcessor.getConfig();
    }

    public boolean addRow(String type, Table table, Row row) {

        if (list.size() == config.maxTransactionRows) {
            return false;
        } else {
            DataRow dataRow = new DataRow(type, table, row);
            list.add(dataRow);
            return true;
        }

    }

    public String toJson() {

        List<Map> rows = new LinkedList<>();
        for (DataRow dataRow : list) {
            Map rowMap = dataRow.toMap();
            if (rowMap != null) {
                rows.add(rowMap);
            }
        }

        Map<String, Object> map = new HashMap<>();
        map.put("xid", xid);
        map.put("binlogFilename", nextBinlogPosition.getBinlogFilename());
        map.put("nextPosition", nextBinlogPosition.getPosition());
        map.put("rows", rows);

        return JSONObject.toJSONString(map);
    }

    public BinlogPosition getNextBinlogPosition() {
        return nextBinlogPosition;
    }

    public void setNextBinlogPosition(BinlogPosition nextBinlogPosition) {
        this.nextBinlogPosition = nextBinlogPosition;
    }

    public void setXid(Long xid) {
        this.xid = xid;
    }

    public Long getXid() {
        return xid;
    }
}