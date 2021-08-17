/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.flink.source.util;

import java.util.Base64;
import org.apache.flink.table.data.DecimalData;
import org.apache.flink.table.data.binary.BinaryStringData;
import org.apache.flink.table.data.util.DataFormatConverters;
import org.apache.flink.table.data.util.DataFormatConverters.TimestampConverter;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.DecimalType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Set;

/** String serializer. */
public class StringSerializer {

    public static TimestampConverter timestampConverter = new TimestampConverter(3);
    private static final Base64.Decoder decoder = Base64.getDecoder();

    public static Object deserialize(
            String value,
            ByteSerializer.ValueType type,
            DataType dataType,
            Set<String> nullValues) {
        return deserialize(value, type, dataType, nullValues, false);
    }

    public static Object deserialize(
            String value,
            ByteSerializer.ValueType type,
            DataType dataType,
            Set<String> nullValues,
            Boolean isRGData) {
        if (null != nullValues && nullValues.contains(value)) {
            return null;
        }
        switch (type) {
            case V_ByteArray: // byte[]
                if (isRGData) {
                    byte[] bytes = null;
                    try {
                        bytes = decoder.decode(value);
                    } catch (Exception e) {
                        //
                    }
                    return bytes;
                } else {
                    return value.getBytes();
                }
            case V_String:
                return BinaryStringData.fromString(value);
            case V_Byte: // byte
                return null == value ? null : Byte.parseByte(value);
            case V_Short:
                return null == value ? null : Short.parseShort(value);
            case V_Integer:
                return null == value ? null : Integer.parseInt(value);
            case V_Long:
                return null == value ? null : Long.parseLong(value);
            case V_Float:
                return null == value ? null : Float.parseFloat(value);
            case V_Double:
                return null == value ? null : Double.parseDouble(value);
            case V_Boolean:
                return null == value ? null : parseBoolean(value);
            case V_Timestamp: // sql.Timestamp encoded as long
                if (isRGData) {
                    return null == value ? null : Long.parseLong(value);
                }
                if (null == value) {
                    return null;
                } else {
                    try {
                        return timestampConverter.toInternal(new Timestamp(Long.parseLong(value)));
                    } catch (NumberFormatException e) {
                        return timestampConverter.toInternal(Timestamp.valueOf(value));
                    }
                }
            case V_Date: // sql.Date encoded as long
                if (isRGData) {
                    return null == value ? null : Long.parseLong(value);
                }
                return null == value
                        ? null
                        : DataFormatConverters.DateConverter.INSTANCE.toInternal(
                                Date.valueOf(value));
            case V_Time: // sql.Time encoded as long
                if (isRGData) {
                    return null == value ? null : Long.parseLong(value);
                }
                return null == value
                        ? null
                        : DataFormatConverters.TimeConverter.INSTANCE.toInternal(
                                new Time(Long.parseLong(value)));
            case V_BigDecimal:
                DecimalType decimalType = (DecimalType) dataType.getLogicalType();
                return value == null
                        ? null
                        : DecimalData.fromBigDecimal(
                                new BigDecimal(value),
                                decimalType.getPrecision(),
                                decimalType.getScale());
            case V_BigInteger:
                return null == value ? null : new BigInteger(value);

            default:
                throw new IllegalArgumentException();
        }
    }

    public static Object deserialize(
            String value, ByteSerializer.ValueType type, DataType dataType, Boolean isRGData) {
        return deserialize(value, type, dataType, null, isRGData);
    }

    public static Boolean parseBoolean(String s) {
        if (s != null) {
            if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")) {
                return Boolean.valueOf(s);
            }

            if (s.equals("1")) {
                return Boolean.TRUE;
            }

            if (s.equals("0")) {
                return Boolean.FALSE;
            }
        }

        throw new IllegalArgumentException();
    }
}
