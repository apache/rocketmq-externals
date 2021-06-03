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

/** Utility class to for operations related to bytes. */
public class ByteUtils {

    /**
     * Converts a byte array to an int value.
     *
     * @param bytes byte array
     * @return the int value
     */
    public static int toInt(byte[] bytes) {
        return toInt(bytes, 0);
    }

    /**
     * Converts a byte array to an int value.
     *
     * @param bytes byte array
     * @param offset offset into array
     * @return the int value
     * @throws IllegalArgumentException if there's not enough room in the array at the offset
     *     indicated.
     */
    public static int toInt(byte[] bytes, int offset) {
        if (offset + Integer.BYTES > bytes.length) {
            throw explainWrongLengthOrOffset(bytes, offset, Integer.BYTES, Integer.BYTES);
        }
        int n = 0;
        for (int i = offset; i < (offset + Integer.BYTES); i++) {
            n <<= 8;
            n ^= bytes[i] & 0xFF;
        }
        return n;
    }

    /**
     * Convert a byte array to a boolean.
     *
     * @param b array
     * @return True or false.
     */
    public static boolean toBoolean(final byte[] b) {
        return toBoolean(b, 0);
    }

    /**
     * Convert a byte array to a boolean.
     *
     * @param b array
     * @param offset offset into array
     * @return True or false.
     */
    public static boolean toBoolean(final byte[] b, final int offset) {
        if (offset + 1 > b.length) {
            throw explainWrongLengthOrOffset(b, offset, 1, 1);
        }
        return b[offset] != (byte) 0;
    }

    /**
     * Converts a byte array to a long value.
     *
     * @param bytes array
     * @return the long value
     */
    public static long toLong(byte[] bytes) {
        return toLong(bytes, 0);
    }

    /**
     * Converts a byte array to a long value.
     *
     * @param bytes array of bytes
     * @param offset offset into array
     * @return the long value
     * @throws IllegalArgumentException if there's not enough room in the array at the offset
     *     indicated.
     */
    public static long toLong(byte[] bytes, int offset) {
        if (offset + Long.BYTES > bytes.length) {
            throw explainWrongLengthOrOffset(bytes, offset, Long.BYTES, Long.BYTES);
        }
        long l = 0;
        for (int i = offset; i < offset + Long.BYTES; i++) {
            l <<= 8;
            l ^= bytes[i] & 0xFF;
        }
        return l;
    }

    /**
     * Presumes float encoded as IEEE 754 floating-point "single format".
     *
     * @param bytes byte array
     * @return Float made from passed byte array.
     */
    public static float toFloat(byte[] bytes) {
        return toFloat(bytes, 0);
    }

    /**
     * Presumes float encoded as IEEE 754 floating-point "single format".
     *
     * @param bytes array to convert
     * @param offset offset into array
     * @return Float made from passed byte array.
     */
    public static float toFloat(byte[] bytes, int offset) {
        return Float.intBitsToFloat(toInt(bytes, offset));
    }

    /**
     * Parse a byte array to double.
     *
     * @param bytes byte array
     * @return Return double made from passed bytes.
     */
    public static double toDouble(final byte[] bytes) {
        return toDouble(bytes, 0);
    }

    /**
     * Parse a byte array to double.
     *
     * @param bytes byte array
     * @param offset offset where double is
     * @return Return double made from passed bytes.
     */
    public static double toDouble(final byte[] bytes, final int offset) {
        return Double.longBitsToDouble(toLong(bytes, offset));
    }

    /**
     * Converts a byte array to a short value.
     *
     * @param bytes byte array
     * @return the short value
     */
    public static short toShort(byte[] bytes) {
        return toShort(bytes, 0);
    }

    /**
     * Converts a byte array to a short value.
     *
     * @param bytes byte array
     * @param offset offset into array
     * @return the short value
     * @throws IllegalArgumentException if there's not enough room in the array at the offset
     *     indicated.
     */
    public static short toShort(byte[] bytes, int offset) {
        if (offset + Short.BYTES > bytes.length) {
            throw explainWrongLengthOrOffset(bytes, offset, Short.BYTES, Short.BYTES);
        }
        short n = 0;
        n ^= bytes[offset] & 0xFF;
        n <<= 8;
        n ^= bytes[offset + 1] & 0xFF;
        return n;
    }

    // ---------------------------------------------------------------------------------------------------------

    private static IllegalArgumentException explainWrongLengthOrOffset(
            final byte[] bytes, final int offset, final int length, final int expectedLength) {
        String exceptionMessage;
        if (length != expectedLength) {
            exceptionMessage = "Wrong length: " + length + ", expected " + expectedLength;
        } else {
            exceptionMessage =
                    "offset ("
                            + offset
                            + ") + length ("
                            + length
                            + ") exceed the"
                            + " capacity of the array: "
                            + bytes.length;
        }
        return new IllegalArgumentException(exceptionMessage);
    }

    public static BigDecimal toBigDecimal(byte[] bytes) {
        return toBigDecimal(bytes, 0, bytes.length);
    }

    public static BigDecimal toBigDecimal(byte[] bytes, int offset, int length) {
        if (bytes != null && length >= 5 && offset + length <= bytes.length) {
            int scale = toInt(bytes, offset);
            byte[] tcBytes = new byte[length - 4];
            System.arraycopy(bytes, offset + 4, tcBytes, 0, length - 4);
            return new BigDecimal(new BigInteger(tcBytes), scale);
        } else {
            return null;
        }
    }
}
