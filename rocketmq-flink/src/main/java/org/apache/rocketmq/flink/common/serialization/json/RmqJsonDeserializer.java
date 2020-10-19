/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.flink.common.serialization.json;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.formats.json.JsonRowDataDeserializationSchema;
import org.apache.flink.formats.json.TimestampFormat;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.RowKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * @Author: gaobo07
 * @Date: 2020/9/28 5:58 PM
 */
public class RmqJsonDeserializer implements DeserializationSchema<RowData> {

    private static final Logger log = LoggerFactory.getLogger(RmqJsonDeserializer.class);

    private JsonRowDataDeserializationSchema jsonDeserializationSchema;
    private final TypeInformation<RowData> producedTypeInfo;
    private final boolean ignoreParseErrors;

    public RmqJsonDeserializer(
            RowType rowType,
            TypeInformation<RowData> producedTypeInfo,
            boolean ignoreParseErrors,
            TimestampFormat timestampFormatOption) {
        this.producedTypeInfo = producedTypeInfo;
        this.ignoreParseErrors = ignoreParseErrors;
        this.jsonDeserializationSchema = new JsonRowDataDeserializationSchema(
                rowType,
                // the result type is never used, so it's fine to pass in Canal's result type
                producedTypeInfo,
                // ignoreParseErrors already contains the functionality of failOnMissingField
                false,
                ignoreParseErrors,
                timestampFormatOption);
    }

    @Override
    public TypeInformation<RowData> getProducedType() {
        // return the type information required by Flink's core interfaces
        return producedTypeInfo;
    }

    @Override
    public RowData deserialize(byte[] message) throws IOException {
        try {
            RowData row = jsonDeserializationSchema.deserialize(message);
            //it's only insert, don't consider delete update
            RowKind kind = RowKind.INSERT;
            row.setRowKind(kind);
            // convert to internal data structure
            return row;
        } catch (Exception e) {
            if (!ignoreParseErrors) {
                throw new IOException(String.format("deserialize json message error, the json message is '%s'",
                        message));
            }
            log.error("deserialize json message error:{}", e);
        }
        return null;
    }

    @Override
    public boolean isEndOfStream(RowData nextElement) {
        return false;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RmqJsonDeserializer that = (RmqJsonDeserializer) o;
        return ignoreParseErrors == that.ignoreParseErrors &&
                Objects.equals(jsonDeserializationSchema, that.jsonDeserializationSchema) &&
                Objects.equals(producedTypeInfo, that.producedTypeInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jsonDeserializationSchema, producedTypeInfo, ignoreParseErrors);
    }

}
