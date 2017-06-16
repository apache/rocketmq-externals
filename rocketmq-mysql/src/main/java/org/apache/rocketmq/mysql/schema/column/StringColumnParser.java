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

import org.apache.commons.codec.Charsets;

public class StringColumnParser extends ColumnParser {

    private String charset;

    public StringColumnParser(String charset) {
        this.charset = charset.toLowerCase();
    }

    @Override
    public Object getValue(Object value) {

        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            return value;
        }

        byte[] bytes = (byte[]) value;

        switch (charset) {
            case "utf8":
            case "utf8mb4":
                return new String(bytes, Charsets.UTF_8);
            case "latin1":
            case "ascii":
                return new String(bytes, Charsets.ISO_8859_1);
            case "ucs2":
                return new String(bytes, Charsets.UTF_16);
            default:
                return new String(bytes, Charsets.toCharset(charset));

        }
    }
}
