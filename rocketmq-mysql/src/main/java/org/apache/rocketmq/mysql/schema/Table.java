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

import java.util.LinkedList;
import java.util.List;
import org.apache.rocketmq.mysql.schema.column.ColumnParser;

public class Table {
    private String database;
    private String name;
    private List<String> colList = new LinkedList<String>();
    private List<ColumnParser> parserList = new LinkedList<ColumnParser>();

    public Table(String database, String table) {
        this.database = database;
        this.name = table;
    }

    public void addCol(String column) {
        colList.add(column);
    }

    public void addParser(ColumnParser columnParser) {
        parserList.add(columnParser);
    }

    public List<String> getColList() {
        return colList;
    }

    public String getDatabase() {
        return database;
    }

    public String getName() {
        return name;
    }

    public List<ColumnParser> getParserList() {
        return parserList;
    }
}