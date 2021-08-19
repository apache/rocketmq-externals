/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.flink.sink.table;

import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.flink.legacy.RocketMQSink;

import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.table.data.RowData;

/** RocketMQRowDataSink helps for writing the converted row data of table to RocketMQ messages. */
public class RocketMQRowDataSink extends RichSinkFunction<RowData> {

    private static final long serialVersionUID = 1L;

    private final RocketMQSink sink;
    private final RocketMQRowDataConverter converter;

    public RocketMQRowDataSink(RocketMQSink sink, RocketMQRowDataConverter converter) {
        this.sink = sink;
        this.converter = converter;
    }

    @Override
    public void open(Configuration configuration) throws Exception {
        sink.open(configuration);
        converter.open();
    }

    @Override
    public void setRuntimeContext(RuntimeContext runtimeContext) {
        sink.setRuntimeContext(runtimeContext);
    }

    @Override
    public void invoke(RowData rowData, Context context) throws Exception {
        Message message = converter.convert(rowData);
        if (message != null) {
            sink.invoke(message, context);
        }
    }

    @Override
    public void close() {
        sink.close();
    }
}
