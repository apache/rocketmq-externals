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

package org.apache.rocketmq.jms.support;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.UUID;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;
import javax.jms.Queue;
import javax.jms.Topic;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.rocketmq.jms.RocketMQConsumer;

public class JMSUtils {

    public static final String  DEFAULT_CHARSET = "UTF-8";

    public static String getDestinationName(Destination destination) {
        try {
            String topicName;
            if (destination instanceof Topic) {
                topicName = ((Topic) destination).getTopicName();
            }
            else if (destination instanceof Queue) {
                topicName = ((Queue) destination).getQueueName();
            }
            else {
                throw new JMSException(String.format("Unsupported Destination type:", destination.getClass()));
            }
            return topicName;
        }
        catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    public static String getConsumerGroup(RocketMQConsumer consumer) {
        try {
            return getConsumerGroup(consumer.getSubscriptionName(),
                consumer.getSession().getConnection().getClientID(),
                consumer.isShared()
            );
        }
        catch (JMSException e) {
            throw new JMSRuntimeException(ExceptionUtils.getStackTrace(e));
        }
    }

    public static String getConsumerGroup(String subscriptionName, String clientID, boolean shared) {
        StringBuffer consumerGroup = new StringBuffer();

        if (StringUtils.isNotBlank(subscriptionName)) {
            consumerGroup.append(subscriptionName);
        }

        if (StringUtils.isNotBlank(clientID)) {
            if (consumerGroup.length() != 0) {
                consumerGroup.append("-");
            }
            consumerGroup.append(clientID);
        }

        if (shared) {
            if (consumerGroup.length() != 0) {
                consumerGroup.append("-");
            }
            consumerGroup.append(uuid());
        }

        if (consumerGroup.length() == 0) {
            consumerGroup.append(uuid());
        }

        return consumerGroup.toString();
    }

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    public static String bytes2String(byte[] bytes) {
        Prediction.checkNotNull(bytes, "bytes could not be null");
        return new String(bytes, Charset.forName(DEFAULT_CHARSET));
    }

    public static byte[] string2Bytes(String source) {
        Prediction.checkNotNull(source, "source could be null");
        try {
            return source.getBytes(DEFAULT_CHARSET);
        }
        catch (UnsupportedEncodingException e) {
            throw new JMSRuntimeException(ExceptionUtils.getStackTrace(e));
        }
    }
}
