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

package org.apache.rocketmq.cloudevent;

import org.apache.rocketmq.cloudevent.impl.RocketMQBinaryMessageReader;
import org.apache.rocketmq.cloudevent.impl.RocketMQHeaders;
import org.apache.rocketmq.cloudevent.impl.RocketMQMessageWriter;
import io.cloudevents.core.message.MessageReader;
import io.cloudevents.core.message.MessageWriter;
import io.cloudevents.core.message.impl.GenericStructuredMessageReader;
import io.cloudevents.core.message.impl.MessageUtils;
import io.cloudevents.lang.Nullable;
import io.cloudevents.rw.CloudEventRWException;
import io.cloudevents.rw.CloudEventWriter;
import org.apache.rocketmq.common.message.Message;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;


@ParametersAreNonnullByDefault
public final class RocketMQMessageFactory {

    private RocketMQMessageFactory() {
        // prevent instantiation
    }

    public static MessageReader createReader(final Message message) throws CloudEventRWException {
        return createReader(message.getProperties(), message.getBody());
    }


    public static MessageReader createReader(final Map<String, String> props, @Nullable final byte[] body) throws CloudEventRWException {

        return MessageUtils.parseStructuredOrBinaryMessage(
                () -> props.get(RocketMQHeaders.CONTENT_TYPE),
                format -> new GenericStructuredMessageReader(format, body),
                () -> props.get(RocketMQHeaders.SPEC_VERSION),
                sv -> new RocketMQBinaryMessageReader(sv, props, body)
        );
    }


    public static MessageWriter<CloudEventWriter<Message>, Message> createWriter(String topic) {
        return new RocketMQMessageWriter<>(topic);
    }

    public static MessageWriter<CloudEventWriter<Message>, Message> createWriter(String topic, String keys) {
        return new RocketMQMessageWriter<>(topic, keys);
    }

    public static MessageWriter<CloudEventWriter<Message>, Message> createWriter(String topic, String keys, String tags) {
        return new RocketMQMessageWriter<>(topic, keys, tags);
    }

}
