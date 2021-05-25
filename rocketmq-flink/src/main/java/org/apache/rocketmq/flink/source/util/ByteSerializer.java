/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.flink.source.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;

/** BytesSerializer is responsible to deserialize field from byte array. */
public class ByteSerializer {

    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    public static Object deserialize(byte[] value, ValueType type) {
        return deserialize(value, type, DEFAULT_CHARSET);
    }

    public static Object deserialize(byte[] value, ValueType type, Charset charset) {
        switch (type) {
            case V_String:
                return null == value ? "" : new String(value, charset);
            case V_Timestamp: // sql.Timestamp encoded as long
                return new Timestamp(ByteUtils.toLong(value));
            case V_Date: // sql.Date encoded as long
                return new Date(ByteUtils.toLong(value));
            case V_Time: // sql.Time encoded as long
                return new Time(ByteUtils.toLong(value));
            case V_BigDecimal:
                return ByteUtils.toBigDecimal(value);
            default:
                return commonDeserialize(value, type);
        }
    }

    private static Object commonDeserialize(byte[] value, ValueType type) {
        switch (type) {
            case V_ByteArray: // byte[]
                return value;
            case V_Byte: // byte
                return null == value ? (byte) '\0' : value[0];
            case V_Short:
                return ByteUtils.toShort(value);
            case V_Integer:
                return ByteUtils.toInt(value);
            case V_Long:
                return ByteUtils.toLong(value);
            case V_Float:
                return ByteUtils.toFloat(value);
            case V_Double:
                return ByteUtils.toDouble(value);
            case V_Boolean:
                return ByteUtils.toBoolean(value);
            case V_BigInteger:
                return new BigInteger(value);
            default:
                throw new IllegalArgumentException();
        }
    }

    public static ValueType getTypeIndex(Class<?> clazz) {
        if (byte[].class.equals(clazz)) {
            return ValueType.V_ByteArray;
        } else if (String.class.equals(clazz)) {
            return ValueType.V_String;
        } else if (Byte.class.equals(clazz)) {
            return ValueType.V_Byte;
        } else if (Short.class.equals(clazz)) {
            return ValueType.V_Short;
        } else if (Integer.class.equals(clazz)) {
            return ValueType.V_Integer;
        } else if (Long.class.equals(clazz)) {
            return ValueType.V_Long;
        } else if (Float.class.equals(clazz)) {
            return ValueType.V_Float;
        } else if (Double.class.equals(clazz)) {
            return ValueType.V_Double;
        } else if (Boolean.class.equals(clazz)) {
            return ValueType.V_Boolean;
        } else if (Timestamp.class.equals(clazz)) {
            return ValueType.V_Timestamp;
        } else if (Date.class.equals(clazz)) {
            return ValueType.V_Date;
        } else if (Time.class.equals(clazz)) {
            return ValueType.V_Time;
        } else if (BigDecimal.class.equals(clazz)) {
            return ValueType.V_BigDecimal;
        } else if (BigInteger.class.equals(clazz)) {
            return ValueType.V_BigInteger;
        } else if (LocalDateTime.class.equals(clazz)) {
            return ValueType.V_LocalDateTime;
        } else if (LocalDate.class.equals(clazz)) {
            return ValueType.V_LocalDate;
        } else if (Duration.class.equals(clazz)) {
            return ValueType.V_Duration;
        } else if (LocalTime.class.equals(clazz)) {
            return ValueType.V_LocalTime;
        } else if (Period.class.equals(clazz)) {
            return ValueType.V_Period;
        } else if (OffsetDateTime.class.equals(clazz)) {
            return ValueType.V_OffsetDateTime;
        } else {
            return ValueType.Unsupported;
        }
    }

    /** Value Type. */
    public enum ValueType {
        V_ByteArray,
        V_String,
        V_Byte,
        V_Short,
        V_Integer,
        V_Long,
        V_Float,
        V_Double,
        V_Boolean,
        V_Timestamp,
        V_Date,
        V_Time,
        V_BigDecimal,
        V_BigInteger,
        V_LocalDateTime,
        V_LocalDate,
        V_Duration,
        V_LocalTime,
        V_Period,
        V_OffsetDateTime,
        Unsupported
    }
}
