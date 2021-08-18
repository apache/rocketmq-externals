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

package org.apache.rocketmq.flink.sink.table;

import org.apache.rocketmq.common.message.Message;

import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.types.RowKind;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.flink.util.Preconditions.checkState;
import static org.apache.rocketmq.flink.sink.table.RocketMQDynamicTableSink.WritableMetadata;
import static org.apache.rocketmq.flink.sink.table.RocketMQDynamicTableSink.WritableMetadata.KEYS;
import static org.apache.rocketmq.flink.sink.table.RocketMQDynamicTableSink.WritableMetadata.TAGS;

/** RocketMQRowDataConverter converts the row data of table to RocketMQ message pattern. */
public class RocketMQRowDataConverter implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(RocketMQRowDataConverter.class);

    private final String topic;
    private final String tag;
    private final String dynamicColumn;
    private final String fieldDelimiter;
    private final String encoding;

    private final boolean isDynamicTag;
    private final boolean isDynamicTagIncluded;
    private final boolean writeKeysToBody;
    private boolean onlyVarbinary = false;

    private final String[] keyColumns;
    private final RowTypeInfo rowTypeInfo;
    private final DataType[] fieldDataTypes;

    private int[] keyFieldIndexes;
    private int[] tagFieldIndexes;
    private int[] bodyFieldIndexes;
    private DataType[] bodyFieldTypes;

    private final boolean hasMetadata;
    private final int[] metadataPositions;

    public RocketMQRowDataConverter(
            String topic,
            String tag,
            String dynamicColumn,
            String fieldDelimiter,
            String encoding,
            boolean isDynamicTag,
            boolean isDynamicTagIncluded,
            boolean writeKeysToBody,
            String[] keyColumns,
            RowTypeInfo rowTypeInfo,
            DataType[] fieldDataTypes,
            boolean hasMetadata,
            int[] metadataPositions) {
        this.topic = topic;
        this.tag = tag;
        this.dynamicColumn = dynamicColumn;
        this.fieldDelimiter = fieldDelimiter;
        this.encoding = encoding;
        this.isDynamicTag = isDynamicTag;
        this.isDynamicTagIncluded = isDynamicTagIncluded;
        this.writeKeysToBody = writeKeysToBody;
        this.keyColumns = keyColumns;
        this.rowTypeInfo = rowTypeInfo;
        this.fieldDataTypes = fieldDataTypes;
        this.hasMetadata = hasMetadata;
        this.metadataPositions = metadataPositions;
    }

    public void open() {
        if (rowTypeInfo.getArity() == 1
                && rowTypeInfo.getFieldTypes()[0].getTypeClass().equals(byte[].class)) {
            onlyVarbinary = true;
        }
        Set<Integer> excludedFields = new HashSet<>();
        if (keyColumns != null) {
            keyFieldIndexes = new int[keyColumns.length];
            for (int index = 0; index < keyColumns.length; index++) {
                int fieldIndex = rowTypeInfo.getFieldIndex(keyColumns[index]);
                checkState(
                        fieldIndex >= 0,
                        String.format(
                                "[MetaQConverter] Could not find the message-key column: %s.",
                                keyColumns[index]));
                keyFieldIndexes[index] = fieldIndex;
                if (!writeKeysToBody) {
                    excludedFields.add(fieldIndex);
                }
            }
        } else {
            keyFieldIndexes = new int[0];
        }
        if (isDynamicTag && dynamicColumn != null) {
            tagFieldIndexes = new int[1];
            int fieldIndex = rowTypeInfo.getFieldIndex(dynamicColumn);
            checkState(
                    fieldIndex >= 0,
                    String.format(
                            "[MetaQConverter] Could not find the tag column: %s.", dynamicColumn));
            tagFieldIndexes[0] = fieldIndex;
            if (!isDynamicTagIncluded) {
                excludedFields.add(fieldIndex);
            }
        } else {
            tagFieldIndexes = new int[0];
        }
        bodyFieldIndexes = new int[rowTypeInfo.getArity() - excludedFields.size()];
        bodyFieldTypes = new DataType[rowTypeInfo.getArity() - excludedFields.size()];
        int index = 0;
        for (int num = 0; num < rowTypeInfo.getArity(); num++) {
            if (!excludedFields.contains(num)) {
                bodyFieldIndexes[index] = num;
                bodyFieldTypes[index++] = fieldDataTypes[num];
            }
        }
    }

    public Message convert(RowData row) {
        if (row.getRowKind() != RowKind.INSERT && row.getRowKind() != RowKind.UPDATE_AFTER) {
            return null;
        }
        Message message = new Message();
        message.setTopic(topic);
        List<String> keys = new ArrayList<>();
        for (int fieldIndex : keyFieldIndexes) {
            keys.add(row.getString(fieldIndex).toString());
        }
        if (keys.size() > 0) {
            message.setKeys(keys);
        }
        if (!isDynamicTag) {
            if (tag != null && tag.length() > 0) {
                message.setTags(tag);
            }
        } else {
            checkState(tagFieldIndexes.length > 0, "No message tag column set.");
            message.setTags(row.getString(tagFieldIndexes[0]).toString());
        }
        if (onlyVarbinary) {
            message.setBody(row.getBinary(0));
            message.setWaitStoreMsgOK(true);
        } else {
            Object[] values = new Object[bodyFieldIndexes.length];
            for (int index = 0; index < bodyFieldIndexes.length; index++) {
                values[index] =
                        RowData.createFieldGetter(
                                        bodyFieldTypes[index].getLogicalType(),
                                        bodyFieldIndexes[index])
                                .getFieldOrNull(row);
            }
            try {
                message.setBody(StringUtils.join(values, fieldDelimiter).getBytes(encoding));
                message.setWaitStoreMsgOK(true);
            } catch (UnsupportedEncodingException e) {
                LOG.error(
                        String.format(
                                "Unsupported ''{%s}'' encoding charset. Check the encoding configItem in the DDL.",
                                encoding),
                        e);
            }
        }
        if (hasMetadata) {
            String messageKeys = readMetadata(row, KEYS);
            message.setKeys(messageKeys);
            message.setTags(readMetadata(row, TAGS));
        }
        return message;
    }

    @SuppressWarnings("unchecked")
    private <T> T readMetadata(RowData consumedRow, WritableMetadata metadata) {
        final int pos = metadataPositions[metadata.ordinal()];
        if (pos < 0) {
            return null;
        }
        return (T) metadata.converter.read(consumedRow, pos);
    }

    // --------------------------------------------------------------------------------------------

    interface MetadataConverter extends Serializable {
        Object read(RowData consumedRow, int pos);
    }
}
