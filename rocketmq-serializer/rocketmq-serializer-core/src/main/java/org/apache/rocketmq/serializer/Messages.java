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
package org.apache.rocketmq.serializer;

import org.apache.commons.lang.Validate;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.serializer.impl.RocketMQStringDeserializer;
import org.apache.rocketmq.serializer.impl.RocketMQStringSerializer;

/**
 * Tool class for message body serializing and deserializing.
 * Using string Serializer and Deserializer by default.
 */
public final class Messages {
    public static RocketMQSerializer defaultSerializer = new RocketMQStringSerializer();
    public static RocketMQDeserializer defaultDeserializer = new RocketMQStringDeserializer();

    private Messages() {}

    public static Message newMessage(String topic, Object obj) {
        return newMessage(topic, obj, defaultSerializer);
    }

    public static Message newMessage(String topic, String tag, Object obj) {
        return newMessage(topic, tag, obj, defaultSerializer);
    }

    public static Message newMessage(String topic, String tag, String key, Object obj) {
        return newMessage(topic, tag, key, obj, defaultSerializer);
    }

    public static Message newMessage(String topic, Object obj, RocketMQSerializer serializer) {
        Validate.notNull(topic);
        Validate.notNull(obj);
        Validate.notNull(serializer);
        return new Message(topic, serializer.serialize(obj));
    }

    public static Message newMessage(String topic, String tag, Object obj, RocketMQSerializer serializer) {
        Validate.notNull(topic);
        Validate.notNull(tag);
        Validate.notNull(obj);
        Validate.notNull(serializer);
        return new Message(topic, tag, serializer.serialize(obj));
    }

    public static Message newMessage(String topic, String tag, String key, Object obj, RocketMQSerializer serializer) {
        Validate.notNull(topic);
        Validate.notNull(tag);
        Validate.notNull(key);
        Validate.notNull(obj);
        Validate.notNull(serializer);
        return new Message(topic, tag, key, serializer.serialize(obj));
    }

    public static <T> T getMessageBody(Message message) {
        return getMessageBody(message, defaultDeserializer);
    }

    public static <T> T getMessageBody(Message message, RocketMQDeserializer deserializer) {
        Validate.notNull(message);
        Validate.notNull(deserializer);
        return (T) deserializer.deserialize(message.getBody());
    }
}
