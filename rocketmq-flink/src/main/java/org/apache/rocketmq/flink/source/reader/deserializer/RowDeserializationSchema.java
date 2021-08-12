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

package org.apache.rocketmq.flink.source.reader.deserializer;

import org.apache.rocketmq.flink.source.util.ByteSerializer;
import org.apache.rocketmq.flink.source.util.ByteSerializer.ValueType;
import org.apache.rocketmq.flink.source.util.StringSerializer;

import org.apache.flink.api.common.serialization.DeserializationSchema.InitializationContext;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.descriptors.DescriptorProperties;
import org.apache.flink.table.descriptors.SchemaValidator;
import org.apache.flink.table.runtime.typeutils.InternalTypeInfo;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.util.Collector;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The row based implementation of {@link DeserializationSchema} for the deserialization of records.
 */
public class RowDeserializationSchema
        implements DeserializationSchema<List<BytesMessage>, RowData> {

    private static final long serialVersionUID = -1L;
    private static final Logger logger = LoggerFactory.getLogger(RowDeserializationSchema.class);

    private transient TableSchema tableSchema;
    private final DirtyDataStrategy formatErrorStrategy;
    private final DirtyDataStrategy fieldMissingStrategy;
    private final DirtyDataStrategy fieldIncrementStrategy;
    private final String encoding;
    private final String fieldDelimiter;
    private final String lineDelimiter;
    private final boolean columnErrorDebug;
    private final MetadataCollector metadataCollector;
    private final int totalColumnSize;
    private final int dataColumnSize;
    private final ValueType[] fieldTypes;
    private transient DataType[] fieldDataTypes;
    private final Set<String> headerFields;
    private final Map<String, String> properties;
    private final Map<String, Integer> columnIndexMapping;
    private final Map<Integer, Integer> dataIndexMapping;
    private long lastLogExceptionTime;
    private long lastLogHandleFieldTime;

    private static final int DEFAULT_LOG_INTERVAL_MS = 60 * 1000;

    public RowDeserializationSchema(
            TableSchema tableSchema,
            DirtyDataStrategy formatErrorStrategy,
            DirtyDataStrategy fieldMissingStrategy,
            DirtyDataStrategy fieldIncrementStrategy,
            String encoding,
            String fieldDelimiter,
            String lineDelimiter,
            boolean columnErrorDebug,
            boolean hasMetadata,
            MetadataConverter[] metadataConverters,
            List<String> headerFields,
            Map<String, String> properties) {
        this.tableSchema = tableSchema;
        this.formatErrorStrategy = formatErrorStrategy;
        this.fieldMissingStrategy = fieldMissingStrategy;
        this.fieldIncrementStrategy = fieldIncrementStrategy;
        this.columnErrorDebug = columnErrorDebug;
        this.encoding = encoding;
        this.fieldDelimiter = StringEscapeUtils.unescapeJava(fieldDelimiter);
        this.lineDelimiter = StringEscapeUtils.unescapeJava(lineDelimiter);
        this.metadataCollector = new MetadataCollector(hasMetadata, metadataConverters);
        this.headerFields = headerFields == null ? null : new HashSet<>(headerFields);
        this.properties = properties;
        this.totalColumnSize = tableSchema.getFieldNames().length;
        int dataColumnSize = 0;
        this.fieldTypes = new ValueType[totalColumnSize];
        this.columnIndexMapping = new HashMap<>();
        this.dataIndexMapping = new HashMap<>();
        for (int index = 0; index < totalColumnSize; index++) {
            this.columnIndexMapping.put(tableSchema.getFieldNames()[index], index);
        }
        for (int index = 0; index < totalColumnSize; index++) {
            ValueType type =
                    ByteSerializer.getTypeIndex(tableSchema.getFieldTypes()[index].getTypeClass());
            this.fieldTypes[index] = type;
            if (!isHeaderField(index)) {
                dataIndexMapping.put(dataColumnSize, index);
                dataColumnSize++;
            }
        }
        this.dataColumnSize = dataColumnSize;
    }

    @Override
    public void open(InitializationContext context) {
        DescriptorProperties descriptorProperties = new DescriptorProperties();
        descriptorProperties.putProperties(properties);
        this.tableSchema = SchemaValidator.deriveTableSinkSchema(descriptorProperties);
        this.fieldDataTypes = tableSchema.getFieldDataTypes();
        this.lastLogExceptionTime = System.currentTimeMillis();
        this.lastLogHandleFieldTime = System.currentTimeMillis();
    }

    @Override
    public void deserialize(List<BytesMessage> messages, Collector<RowData> collector) {
        metadataCollector.collector = collector;
        deserialize(messages, metadataCollector);
    }

    private void deserialize(List<BytesMessage> messages, MetadataCollector collector) {
        if (null == messages || messages.size() == 0) {
            return;
        }
        for (BytesMessage message : messages) {
            collector.message = message;
            if (isOnlyHaveVarbinaryDataField()) {
                GenericRowData rowData = new GenericRowData(totalColumnSize);
                int dataIndex = dataIndexMapping.get(0);
                rowData.setField(dataIndex, message.getData());
                for (int index = 0; index < totalColumnSize; index++) {
                    if (index == dataIndex) {
                        continue;
                    }
                    String headerValue = getHeaderValue(message, index);
                    rowData.setField(
                            index,
                            StringSerializer.deserialize(
                                    headerValue,
                                    fieldTypes[index],
                                    fieldDataTypes[index],
                                    new HashSet<>()));
                }
                collector.collect(rowData);
            } else if (isAllHeaderField()) {
                GenericRowData rowData = new GenericRowData(totalColumnSize);
                for (int index = 0; index < totalColumnSize; index++) {
                    String headerValue = getHeaderValue(message, index);
                    rowData.setField(
                            index,
                            StringSerializer.deserialize(
                                    headerValue,
                                    fieldTypes[index],
                                    fieldDataTypes[index],
                                    new HashSet<>()));
                }
                collector.collect(rowData);
            } else {
                if (message.getData() == null) {
                    logger.info("Deserialize empty BytesMessage body, ignore the empty message.");
                    return;
                }
                deserializeBytesMessage(message, collector);
            }
        }
    }

    private boolean isOnlyHaveVarbinaryDataField() {
        if (dataColumnSize == 1 && dataIndexMapping.size() == 1) {
            int index = dataIndexMapping.get(0);
            return isByteArrayType(tableSchema.getFieldNames()[index]);
        }
        return false;
    }

    private boolean isAllHeaderField() {
        return null != headerFields && headerFields.size() == tableSchema.getFieldNames().length;
    }

    private void deserializeBytesMessage(BytesMessage message, Collector<RowData> collector) {
        String body;
        try {
            body = new String(message.getData(), encoding);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        String[] lines = StringUtils.split(body, lineDelimiter);
        for (String line : lines) {
            String[] data = StringUtils.splitPreserveAllTokens(line, fieldDelimiter);
            if (dataColumnSize == 1) {
                data = new String[1];
                data[0] = line;
            }
            if (data.length < dataColumnSize) {
                data = handleFieldMissing(data);
            } else if (data.length > dataColumnSize) {
                data = handleFieldIncrement(data);
            }
            if (data == null) {
                continue;
            }
            GenericRowData rowData = new GenericRowData(totalColumnSize);
            boolean skip = false;
            for (int index = 0; index < totalColumnSize; index++) {
                try {
                    String fieldValue = getValue(message, data, line, index);
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
                continue;
            }
            collector.collect(rowData);
        }
    }

    private boolean isHeaderField(int index) {
        return headerFields != null && headerFields.contains(tableSchema.getFieldNames()[index]);
    }

    private String getHeaderValue(BytesMessage message, int index) {
        Object object = message.getProperty(tableSchema.getFieldNames()[index]);
        return object == null ? "" : (String) object;
    }

    private String getValue(BytesMessage message, String[] data, String line, int index) {
        String fieldValue = null;
        if (isHeaderField(index)) {
            fieldValue = getHeaderValue(message, index);
        } else {
            if (dataColumnSize == 1) {
                fieldValue = line;
            } else {
                if (index < data.length) {
                    fieldValue = data[index];
                }
            }
        }

        return fieldValue;
    }

    private boolean isByteArrayType(String fieldName) {
        TypeInformation<?> typeInformation =
                tableSchema.getFieldTypes()[columnIndexMapping.get(fieldName)];
        if (typeInformation != null) {
            ValueType valueType = ByteSerializer.getTypeIndex(typeInformation.getTypeClass());
            return valueType == ValueType.V_ByteArray;
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
        switch (fieldMissingStrategy) {
            default:
            case SKIP:
                long now = System.currentTimeMillis();
                if (columnErrorDebug || now - lastLogHandleFieldTime > DEFAULT_LOG_INTERVAL_MS) {
                    logger.warn(
                            "Field missing error, table column number: "
                                    + totalColumnSize
                                    + ", data column number: "
                                    + dataColumnSize
                                    + ", data field number: "
                                    + data.length
                                    + ", data: ["
                                    + StringUtils.join(data, ",")
                                    + "]");
                    lastLogHandleFieldTime = now;
                }
                return null;
            case SKIP_SILENT:
                return null;
            case CUT:
            case NULL:
            case PAD:
                {
                    String[] res = new String[totalColumnSize];
                    for (int i = 0; i < data.length; ++i) {
                        Object dataIndex = dataIndexMapping.get(i);
                        if (dataIndex != null) {
                            res[(int) dataIndex] = data[i];
                        }
                    }
                    return res;
                }
            case EXCEPTION:
                throw new RuntimeException();
        }
    }

    private String[] handleFieldIncrement(String[] data) {
        switch (fieldIncrementStrategy) {
            case SKIP:
                long now = System.currentTimeMillis();
                if (columnErrorDebug || now - lastLogHandleFieldTime > DEFAULT_LOG_INTERVAL_MS) {
                    logger.warn(
                            "Field increment error, table column number: "
                                    + totalColumnSize
                                    + ", data column number: "
                                    + dataColumnSize
                                    + ", data field number: "
                                    + data.length
                                    + ", data: ["
                                    + StringUtils.join(data, ",")
                                    + "]");
                    lastLogHandleFieldTime = now;
                }
                return null;
            case SKIP_SILENT:
                return null;
            default:
            case CUT:
            case NULL:
            case PAD:
                {
                    String[] res = new String[totalColumnSize];
                    for (int i = 0; i < dataColumnSize; ++i) {
                        Object dataIndex = dataIndexMapping.get(i);
                        if (dataIndex != null) {
                            res[(int) dataIndex] = data[i];
                        }
                    }
                    return res;
                }
            case EXCEPTION:
                throw new RuntimeException();
        }
    }

    @Override
    public TypeInformation<RowData> getProducedType() {
        return InternalTypeInfo.of((RowType) tableSchema.toRowDataType().getLogicalType());
    }

    // --------------------------------------------------------------------------------------------

    /** Source metadata converter interface. */
    public interface MetadataConverter extends Serializable {
        Object read(BytesMessage message);
    }

    // --------------------------------------------------------------------------------------------

    /** Metadata of RowData collector. */
    public static final class MetadataCollector implements Collector<RowData>, Serializable {

        private static final long serialVersionUID = 1L;

        private final boolean hasMetadata;
        private final MetadataConverter[] metadataConverters;

        public transient BytesMessage message;
        public transient Collector<RowData> collector;

        public MetadataCollector(boolean hasMetadata, MetadataConverter[] metadataConverters) {
            this.hasMetadata = hasMetadata;
            this.metadataConverters = metadataConverters;
        }

        @Override
        public void collect(RowData physicalRow) {
            if (hasMetadata) {
                final int physicalArity = physicalRow.getArity();
                final int metadataArity = metadataConverters.length;
                final GenericRowData producedRow =
                        new GenericRowData(physicalRow.getRowKind(), physicalArity + metadataArity);
                final GenericRowData genericPhysicalRow = (GenericRowData) physicalRow;
                for (int index = 0; index < physicalArity; index++) {
                    producedRow.setField(index, genericPhysicalRow.getField(index));
                }
                for (int index = 0; index < metadataArity; index++) {
                    producedRow.setField(
                            index + physicalArity, metadataConverters[index].read(message));
                }
                collector.collect(producedRow);
            } else {
                collector.collect(physicalRow);
            }
        }

        @Override
        public void close() {
            // nothing to do
        }
    }

    /** Builder of {@link RowDeserializationSchema}. */
    public static class Builder {

        private TableSchema schema;
        private DirtyDataStrategy formatErrorStrategy = DirtyDataStrategy.SKIP;
        private DirtyDataStrategy fieldMissingStrategy = DirtyDataStrategy.SKIP;
        private DirtyDataStrategy fieldIncrementStrategy = DirtyDataStrategy.CUT;
        private String encoding = "UTF-8";
        private String lineDelimiter = "\n";
        private String fieldDelimiter = "\u0001";
        private boolean columnErrorDebug = false;
        private boolean hasMetadata;
        private MetadataConverter[] metadataConverters;
        private List<String> headerFields;
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

        public Builder setLineDelimiter(String lineDelimiter) {
            this.lineDelimiter = lineDelimiter;
            return this;
        }

        public Builder setColumnErrorDebug(boolean columnErrorDebug) {
            this.columnErrorDebug = columnErrorDebug;
            return this;
        }

        public Builder setHasMetadata(boolean hasMetadata) {
            this.hasMetadata = hasMetadata;
            return this;
        }

        public Builder setMetadataConverters(MetadataConverter[] metadataConverters) {
            this.metadataConverters = metadataConverters;
            return this;
        }

        public Builder setHeaderFields(List<String> headerFields) {
            this.headerFields = headerFields;
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
            this.setLineDelimiter(configuration.getString(CollectorOption.LINE_DELIMITER));
            this.setColumnErrorDebug(configuration.getBoolean(CollectorOption.COLUMN_ERROR_DEBUG));
            return this;
        }

        public RowDeserializationSchema build() {
            return new RowDeserializationSchema(
                    schema,
                    formatErrorStrategy,
                    fieldMissingStrategy,
                    fieldIncrementStrategy,
                    encoding,
                    fieldDelimiter,
                    lineDelimiter,
                    columnErrorDebug,
                    hasMetadata,
                    metadataConverters,
                    headerFields,
                    properties);
        }
    }

    /** Options for {@link RowDeserializationSchema}. */
    public static class CollectorOption {
        public static final ConfigOption<String> ENCODING =
                ConfigOptions.key("encoding".toLowerCase()).defaultValue("UTF-8");
        public static final ConfigOption<String> FIELD_DELIMITER =
                ConfigOptions.key("fieldDelimiter".toLowerCase()).defaultValue("\u0001");
        public static final ConfigOption<String> LINE_DELIMITER =
                ConfigOptions.key("lineDelimiter".toLowerCase()).defaultValue("\n");
        public static final ConfigOption<Boolean> COLUMN_ERROR_DEBUG =
                ConfigOptions.key("columnErrorDebug".toLowerCase()).defaultValue(true);
        public static final ConfigOption<String> LENGTH_CHECK =
                ConfigOptions.key("lengthCheck".toLowerCase()).defaultValue("NONE");
    }
}
