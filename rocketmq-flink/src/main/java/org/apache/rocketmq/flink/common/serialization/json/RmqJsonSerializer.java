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

import org.apache.flink.formats.json.JsonRowDataSerializationSchema;
import org.apache.flink.formats.json.TimestampFormat;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.RowType;
import org.apache.rocketmq.common.message.Message;

import java.util.Objects;

/**
 * @Author: gaobo07
 * @Date: 2020/10/7 3:12 PM
 */
public class RmqJsonSerializer implements RmqSerializationSchema<RowData> {

    private final JsonRowDataSerializationSchema jsonSerializer;

    private final TimestampFormat timestampFormat;

    private final Integer sinkKeyPos;

    public RmqJsonSerializer(RowType rowType, TimestampFormat timestampFormat, Integer sinkKeyPos) {
        this.timestampFormat = timestampFormat;
        this.jsonSerializer = new JsonRowDataSerializationSchema(rowType, timestampFormat);
        this.sinkKeyPos = sinkKeyPos;
    }

    @Override
    public Message serialize(RowData element) {
        String key = String.valueOf(((GenericRowData) element).getField(sinkKeyPos));
        Message message = new Message("", "", key, jsonSerializer.serialize(element));
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RmqJsonSerializer that = (RmqJsonSerializer) o;
        return Objects.equals(timestampFormat, that.timestampFormat) &&
                Objects.equals(jsonSerializer, that.jsonSerializer) &&
                sinkKeyPos.equals(that.sinkKeyPos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jsonSerializer, timestampFormat, sinkKeyPos);
    }

}
