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
package org.apache.rocketmq.flink.legacy.common.serialization;

import org.apache.rocketmq.flink.source.reader.deserializer.DirtyDataStrategy;
import org.apache.rocketmq.flink.source.util.ByteSerializer;
import org.apache.rocketmq.flink.source.util.StringSerializer;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.descriptors.DescriptorProperties;
import org.apache.flink.table.runtime.typeutils.InternalTypeInfo;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.RowType;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * * The row based implementation of {@link KeyValueDeserializationSchema} for the deserialization
 * of message key and value..
 */
public class RowKeyValueDeserializationSchema implements KeyValueDeserializationSchema<RowData> {

    private static final long serialVersionUID = -1L;
    private static final Logger logger =
            LoggerFactory.getLogger(RowKeyValueDeserializationSchema.class);

    private transient TableSchema tableSchema;
    private final DirtyDataStrategy formatErrorStrategy;
    private final DirtyDataStrategy fieldMissingStrategy;
    private final DirtyDataStrategy fieldIncrementStrategy;
    private final String encoding;
    private final String fieldDelimiter;
    private final boolean columnErrorDebug;
    private final int columnSize;
    private final ByteSerializer.ValueType[] fieldTypes;
    private final transient DataType[] fieldDataTypes;
    private final Map<String, Integer> columnIndexMapping;
    private long lastLogExceptionTime;
    private long lastLogHandleFieldTime;

    private static final int DEFAULT_LOG_INTERVAL_MS = 60 * 1000;

    public RowKeyValueDeserializationSchema(
            TableSchema tableSchema,
            DirtyDataStrategy formatErrorStrategy,
            DirtyDataStrategy fieldMissingStrategy,
            DirtyDataStrategy fieldIncrementStrategy,
            String encoding,
            String fieldDelimiter,
            boolean columnErrorDebug,
            Map<String, String> properties) {
        this.tableSchema = tableSchema;
        this.formatErrorStrategy = formatErrorStrategy;
        this.fieldMissingStrategy = fieldMissingStrategy;
        this.fieldIncrementStrategy = fieldIncrementStrategy;
        this.columnErrorDebug = columnErrorDebug;
        this.encoding = encoding;
        this.fieldDelimiter = StringEscapeUtils.unescapeJava(fieldDelimiter);
        this.columnSize = tableSchema.getFieldNames().length;
        this.fieldTypes = new ByteSerializer.ValueType[columnSize];
        this.columnIndexMapping = new HashMap<>();
        for (int index = 0; index < columnSize; index++) {
            this.columnIndexMapping.put(tableSchema.getFieldNames()[index], index);
        }
        for (int index = 0; index < columnSize; index++) {
            ByteSerializer.ValueType type =
                    ByteSerializer.getTypeIndex(tableSchema.getFieldTypes()[index].getTypeClass());
            this.fieldTypes[index] = type;
        }

        DescriptorProperties descriptorProperties = new DescriptorProperties();
        descriptorProperties.putProperties(properties);
        this.fieldDataTypes = tableSchema.getFieldDataTypes();
        this.lastLogExceptionTime = System.currentTimeMillis();
        this.lastLogHandleFieldTime = System.currentTimeMillis();
    }

    @Override
    public RowData deserializeKeyAndValue(byte[] key, byte[] value) {
        if (isOnlyHaveVarbinaryDataField()) {
            GenericRowData rowData = new GenericRowData(columnSize);
            rowData.setField(0, value);
            return rowData;
        } else {
            if (value == null) {
                logger.info("Deserialize empty BytesMessage body, ignore the empty message.");
                return null;
            }
            return deserializeValue(value);
        }
    }

    @Override
    public TypeInformation<RowData> getProducedType() {
        return InternalTypeInfo.of((RowType) tableSchema.toRowDataType().getLogicalType());
    }

    private boolean isOnlyHaveVarbinaryDataField() {
        if (columnSize == 1) {
            return isByteArrayType(tableSchema.getFieldNames()[0]);
        }
        return false;
    }

    private RowData deserializeValue(byte[] value) {
        String body;
        try {
            body = new String(value, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        String[] data = StringUtils.splitPreserveAllTokens(body, fieldDelimiter);
        if (columnSize == 1) {
            data = new String[1];
            data[0] = body;
        }
        if (data.length < columnSize) {
            data = handleFieldMissing(data);
        } else if (data.length > columnSize) {
            data = handleFieldIncrement(data);
        }
        if (data == null) {
            return null;
        }
        GenericRowData rowData = new GenericRowData(columnSize);
        boolean skip = false;
        for (int index = 0; index < columnSize; index++) {
            try {
                String fieldValue = getValue(data, body, index);
                rowData.setField(
                        index,
                        StringSerializer.deserialize(
                                fieldValue,
                                fieldTypes[index],
                                fieldDataTypes[index],
                                new HashSet<>()));
            } catch (Exception e) {
                skip = handleException(rowData, index, data, e);
            }
        }
        if (skip) {
            return null;
        }
        return rowData;
    }

    private String getValue(String[] data, String line, int index) {
        String fieldValue = null;
        if (columnSize == 1) {
            fieldValue = line;
        } else {
            if (index < data.length) {
                fieldValue = data[index];
            }
        }
        return fieldValue;
    }

    private boolean isByteArrayType(String fieldName) {
        TypeInformation<?> typeInformation =
                tableSchema.getFieldTypes()[columnIndexMapping.get(fieldName)];
        if (typeInformation != null) {
            ByteSerializer.ValueType valueType =
                    ByteSerializer.getTypeIndex(typeInformation.getTypeClass());
            return valueType == ByteSerializer.ValueType.V_ByteArray;
        }
        return false;
    }

    private boolean handleException(GenericRowData row, int index, Object[] data, Exception e) {
        boolean skip = false;
        switch (formatErrorStrategy) {
            case SKIP:
                long now = System.currentTimeMillis();
                if (columnErrorDebug || now - lastLogExceptionTime > DEFAULT_LOG_INTERVAL_MS) {
                    logger.warn(
                            "Data format error, field type: "
                                    + fieldTypes[index]
                                    + "field data: "
                                    + data[index]
                                    + ", index: "
                                    + index
                                    + ", data: ["
                                    + StringUtils.join(data, ",")
                                    + "]",
                            e);
                    lastLogExceptionTime = now;
                }
                skip = true;
                break;
            case SKIP_SILENT:
                skip = true;
                break;
            default:
            case CUT:
            case NULL:
            case PAD:
                row.setField(index, null);
                break;
            case EXCEPTION:
                throw new RuntimeException(e);
        }

        return skip;
    }

    private String[] handleFieldMissing(String[] data) {
        String fieldMissingMessage =
                String.format(
                        "Field missing exception, table column number: %d, data column number: %d, data field number: %d, data: [%s].",
                        columnSize, columnSize, data.length, StringUtils.join(data, ","));
        switch (fieldMissingStrategy) {
            default:
            case SKIP:
                long now = System.currentTimeMillis();
                if (columnErrorDebug || now - lastLogHandleFieldTime > DEFAULT_LOG_INTERVAL_MS) {
                    logger.warn(fieldMissingMessage);
                    lastLogHandleFieldTime = now;
                }
                return null;
            case SKIP_SILENT:
                return null;
            case CUT:
            case NULL:
            case PAD:
                return data;
            case EXCEPTION:
                logger.error(fieldMissingMessage);
                throw new RuntimeException(fieldMissingMessage);
        }
    }

    private String[] handleFieldIncrement(String[] data) {
        String fieldIncrementMessage =
                String.format(
                        "Field increment exception, table column number: %d, data column number: %d, data field number: %d, data: [%s].",
                        columnSize, columnSize, data.length, StringUtils.join(data, ","));
        switch (fieldIncrementStrategy) {
            case SKIP:
                long now = System.currentTimeMillis();
                if (columnErrorDebug || now - lastLogHandleFieldTime > DEFAULT_LOG_INTERVAL_MS) {
                    logger.warn(fieldIncrementMessage);
                    lastLogHandleFieldTime = now;
                }
                return null;
            case SKIP_SILENT:
                return null;
            default:
            case CUT:
            case NULL:
            case PAD:
                return data;
            case EXCEPTION:
                logger.error(fieldIncrementMessage);
                throw new RuntimeException(fieldIncrementMessage);
        }
    }

    /** Builder of {@link RowKeyValueDeserializationSchema}. */
    public static class Builder {

        private TableSchema schema;
        private DirtyDataStrategy formatErrorStrategy = DirtyDataStrategy.SKIP;
        private DirtyDataStrategy fieldMissingStrategy = DirtyDataStrategy.SKIP;
        private DirtyDataStrategy fieldIncrementStrategy = DirtyDataStrategy.CUT;
        private String encoding = "UTF-8";
        private String fieldDelimiter = "\u0001";
        private boolean columnErrorDebug = false;
        private Map<String, String> properties;

        public Builder() {}

        public Builder setTableSchema(TableSchema tableSchema) {
            this.schema = tableSchema;
            return this;
        }

        public Builder setFormatErrorStrategy(DirtyDataStrategy formatErrorStrategy) {
            this.formatErrorStrategy = formatErrorStrategy;
            return this;
        }

        public Builder setFieldMissingStrategy(DirtyDataStrategy fieldMissingStrategy) {
            this.fieldMissingStrategy = fieldMissingStrategy;
            return this;
        }

        public Builder setFieldIncrementStrategy(DirtyDataStrategy fieldIncrementStrategy) {
            this.fieldIncrementStrategy = fieldIncrementStrategy;
            return this;
        }

        public Builder setEncoding(String encoding) {
            this.encoding = encoding;
            return this;
        }

        public Builder setFieldDelimiter(String fieldDelimiter) {
            this.fieldDelimiter = fieldDelimiter;
            return this;
        }

        public Builder setColumnErrorDebug(boolean columnErrorDebug) {
            this.columnErrorDebug = columnErrorDebug;
            return this;
        }

        public Builder setProperties(Map<String, String> properties) {
            this.properties = properties;
            if (null == properties) {
                return this;
            }
            Configuration configuration = new Configuration();
            for (String key : properties.keySet()) {
                configuration.setString(key, properties.get(key));
            }
            String lengthCheck = configuration.get(CollectorOption.LENGTH_CHECK);
            switch (lengthCheck.toUpperCase()) {
                case "SKIP":
                    {
                        this.setFormatErrorStrategy(DirtyDataStrategy.SKIP);
                        this.setFieldMissingStrategy(DirtyDataStrategy.SKIP);
                        this.setFieldIncrementStrategy(DirtyDataStrategy.SKIP);
                    }
                    break;
                case "PAD":
                    {
                        this.setFormatErrorStrategy(DirtyDataStrategy.SKIP);
                        this.setFieldMissingStrategy(DirtyDataStrategy.PAD);
                        this.setFieldIncrementStrategy(DirtyDataStrategy.CUT);
                    }
                    break;
                case "EXCEPTION":
                    {
                        this.setFormatErrorStrategy(DirtyDataStrategy.EXCEPTION);
                        this.setFieldMissingStrategy(DirtyDataStrategy.EXCEPTION);
                        this.setFieldIncrementStrategy(DirtyDataStrategy.EXCEPTION);
                    }
                    break;
                case "SKIP_SILENT":
                    {
                        this.setFormatErrorStrategy(DirtyDataStrategy.SKIP_SILENT);
                        this.setFieldMissingStrategy(DirtyDataStrategy.SKIP_SILENT);
                        this.setFieldIncrementStrategy(DirtyDataStrategy.SKIP_SILENT);
                    }
                    break;
                default:
            }
            this.setEncoding(configuration.getString(CollectorOption.ENCODING));
            this.setFieldDelimiter(configuration.getString(CollectorOption.FIELD_DELIMITER));
            this.setColumnErrorDebug(configuration.getBoolean(CollectorOption.COLUMN_ERROR_DEBUG));
            return this;
        }

        public RowKeyValueDeserializationSchema build() {
            return new RowKeyValueDeserializationSchema(
                    schema,
                    formatErrorStrategy,
                    fieldMissingStrategy,
                    fieldIncrementStrategy,
                    encoding,
                    fieldDelimiter,
                    columnErrorDebug,
                    properties);
        }
    }

    /** Options for {@link RowKeyValueDeserializationSchema}. */
    public static class CollectorOption {
        public static final ConfigOption<String> ENCODING =
                ConfigOptions.key("encoding".toLowerCase()).defaultValue("UTF-8");
        public static final ConfigOption<String> FIELD_DELIMITER =
                ConfigOptions.key("fieldDelimiter".toLowerCase()).defaultValue("\u0001");
        public static final ConfigOption<Boolean> COLUMN_ERROR_DEBUG =
                ConfigOptions.key("columnErrorDebug".toLowerCase()).defaultValue(true);
        public static final ConfigOption<String> LENGTH_CHECK =
                ConfigOptions.key("lengthCheck".toLowerCase()).defaultValue("NONE");
    }
}
