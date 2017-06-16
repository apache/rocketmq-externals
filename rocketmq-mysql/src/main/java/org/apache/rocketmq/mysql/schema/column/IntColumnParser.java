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

package org.apache.rocketmq.mysql.schema.column;

public class IntColumnParser extends ColumnParser {

    private int bits;
    private boolean signed;

    public IntColumnParser(String dataType, String colType) {

        switch (dataType) {
            case "tinyint":
                bits = 8;
                break;
            case "smallint":
                bits = 16;
                break;
            case "mediumint":
                bits = 24;
                break;
            case "int":
                bits = 32;
        }

        this.signed = !colType.matches(".* unsigned$");
    }

    @Override
    public Object getValue(Object value) {

        if (value == null) {
            return null;
        }

        if (value instanceof Long) {
            return value;
        }

        if (value instanceof Integer) {
            Integer i = (Integer) value;
            if (signed || i > 0) {
                return i;
            } else {
                return (1L << bits) + i;
            }
        }

        return value;
    }
}
