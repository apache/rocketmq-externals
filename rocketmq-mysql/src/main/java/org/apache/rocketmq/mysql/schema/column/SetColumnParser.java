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

public class SetColumnParser extends ColumnParser {

    private String[] enumValues;

    public SetColumnParser(String colType) {
        enumValues = extractEnumValues(colType);
    }

    @Override
    public Object getValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            return value;
        }

        StringBuilder builder = new StringBuilder();
        long l = (Long) value;

        boolean needSplit = false;
        for (int i = 0; i < enumValues.length; i++) {
            if (((l >> i) & 1) == 1) {
                if (needSplit)
                    builder.append(",");

                builder.append(enumValues[i]);
                needSplit = true;
            }
        }

        return builder.toString();
    }
}
