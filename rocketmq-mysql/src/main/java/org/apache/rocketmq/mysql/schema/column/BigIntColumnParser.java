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

import java.math.BigInteger;

public class BigIntColumnParser extends ColumnParser {

    private static BigInteger max = BigInteger.ONE.shiftLeft(64);

    private boolean signed;

    public BigIntColumnParser(String colType) {
        this.signed = !colType.matches(".* unsigned$");
    }

    @Override
    public Object getValue(Object value) {

        if (value == null) {
            return null;
        }

        if (value instanceof BigInteger) {
            return value;
        }

        Long l = (Long) value;
        if (!signed && l < 0) {
            return max.add(BigInteger.valueOf(l));
        } else {
            return l;
        }
    }
}
