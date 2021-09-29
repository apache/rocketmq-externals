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
import io.openmessaging.connector.api.data.Converter;
import io.openmessaging.connector.api.data.SourceDataEntry;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.connect.runtime.common.MessageWrapper;
import org.apache.rocketmq.connect.runtime.common.PositionWrapper;

public abstract class AbstractConverter<T> implements Converter<T> {

    public MessageWrapper converter(SourceDataEntry sourceDataEntry) {
        Message message = new Message();
        message.setTopic(sourceDataEntry.getQueueName());
        message.setBody(JSON.toJSONString(sourceDataEntry).getBytes());

        PositionWrapper positionWrapper = new PositionWrapper();
        positionWrapper.setPartition(sourceDataEntry.getSourcePartition());
        positionWrapper.setPosition(sourceDataEntry.getSourcePosition());

        MessageWrapper messageWrapper = new MessageWrapper();
        messageWrapper.setMessage(message);
        messageWrapper.setPositionWrapper(positionWrapper);
        return messageWrapper;
    }
}
