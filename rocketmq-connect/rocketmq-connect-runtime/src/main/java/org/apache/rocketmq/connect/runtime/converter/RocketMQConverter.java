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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.rocketmq.connect.runtime.converter;

import com.alibaba.fastjson.JSON;
import io.openmessaging.connector.api.data.EntryType;
import io.openmessaging.connector.api.data.Schema;
import io.openmessaging.connector.api.data.SourceDataEntry;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageAccessor;
import org.apache.rocketmq.connect.runtime.common.MessageWrapper;
import org.apache.rocketmq.connect.runtime.common.PositionWrapper;
import org.apache.rocketmq.connect.runtime.config.RuntimeConfigDefine;

/**
 * This converter does nothing, only for marking
 */
public class RocketMQConverter extends AbstractConverter {

    @Override
    public MessageWrapper converter(SourceDataEntry sourceDataEntry) {
        final Message message = new Message();
        message.setTopic(sourceDataEntry.getQueueName());

        ByteBuffer partition = sourceDataEntry.getSourcePartition();
        Optional<ByteBuffer> opartition = Optional.ofNullable(partition);
        ByteBuffer position = sourceDataEntry.getSourcePosition();
        Optional<ByteBuffer> oposition = Optional.ofNullable(position);

        if (StringUtils.isNotEmpty(sourceDataEntry.getShardingKey())) {
            MessageAccessor.putProperty(message, RuntimeConfigDefine.CONNECT_SHARDINGKEY, sourceDataEntry.getShardingKey());
        }
        if (StringUtils.isNotEmpty(sourceDataEntry.getQueueName())) {
            MessageAccessor.putProperty(message, RuntimeConfigDefine.CONNECT_TOPICNAME, sourceDataEntry.getQueueName());
        }
        if (opartition.isPresent()) {
            MessageAccessor.putProperty(message, RuntimeConfigDefine.CONNECT_SOURCE_PARTITION, new String(opartition.get().array()));
        }
        if (oposition.isPresent()) {
            MessageAccessor.putProperty(message, RuntimeConfigDefine.CONNECT_SOURCE_POSITION, new String(oposition.get().array()));
        }
        EntryType entryType = sourceDataEntry.getEntryType();
        Optional<EntryType> oentryType = Optional.ofNullable(entryType);
        if (oentryType.isPresent()) {
            MessageAccessor.putProperty(message, RuntimeConfigDefine.CONNECT_ENTRYTYPE, oentryType.get().name());
        }
        Long timestamp = sourceDataEntry.getTimestamp();
        Optional<Long> otimestamp = Optional.ofNullable(timestamp);
        if (otimestamp.isPresent()) {
            MessageAccessor.putProperty(message, RuntimeConfigDefine.CONNECT_TIMESTAMP, otimestamp.get().toString());
        }
        Schema schema = sourceDataEntry.getSchema();
        Optional<Schema> oschema = Optional.ofNullable(schema);
        if (oschema.isPresent()) {
            MessageAccessor.putProperty(message, RuntimeConfigDefine.CONNECT_SCHEMA, JSON.toJSONString(oschema.get()));
        }
        Object[] payload = sourceDataEntry.getPayload();
        if (null != payload && null != payload[0]) {
            Object object = payload[0];
            final byte[] messageBody = (String.valueOf(object)).getBytes();
            message.setBody(messageBody);
        }

        PositionWrapper positionWrapper = new PositionWrapper();
        positionWrapper.setPartition(sourceDataEntry.getSourcePartition());
        positionWrapper.setPosition(sourceDataEntry.getSourcePosition());

        MessageWrapper messageWrapper = new MessageWrapper();
        messageWrapper.setMessage(message);
        messageWrapper.setPositionWrapper(positionWrapper);
        return messageWrapper;
    }

    @Override
    public byte[] objectToByte(Object o) {
        return null;
    }

    @Override
    public Object byteToObject(byte[] bytes) {
        return null;
    }
}
