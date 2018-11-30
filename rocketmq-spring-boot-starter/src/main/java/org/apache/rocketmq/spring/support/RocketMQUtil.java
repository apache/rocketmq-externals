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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.spring.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.Charset;
import java.util.Objects;

public class RocketMQUtil {
    private final static Logger log = LoggerFactory.getLogger(RocketMQUtil.class);

    public static TransactionListener convert(RocketMQLocalTransactionListener listener) {
        return new TransactionListener() {
            @Override
            public LocalTransactionState executeLocalTransaction(Message message, Object obj) {
                RocketMQLocalTransactionState state = listener.executeLocalTransaction(convertToSpringMsg(message), obj);
                return convertLocalTransactionState(state);
            }

            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt messageExt) {
                RocketMQLocalTransactionState state = listener.checkLocalTransaction(convertToSpringMsg(messageExt));
                return convertLocalTransactionState(state);
            }
        };
    }

    private static LocalTransactionState convertLocalTransactionState(RocketMQLocalTransactionState state) {
        switch (state) {
            case UNKNOW:
                return LocalTransactionState.UNKNOW;
            case COMMIT_MESSAGE:
                return LocalTransactionState.COMMIT_MESSAGE;
            case ROLLBACK_MESSAGE:
                return LocalTransactionState.ROLLBACK_MESSAGE;
        }

        // Never happen
        log.warn("Failed to covert enum type RocketMQLocalTransactionState.%s", state);
        return LocalTransactionState.UNKNOW;
    }

    public static MessagingException convert(MQClientException e) {
        return new MessagingException(e.getErrorMessage(), e);
    }

    public static org.springframework.messaging.Message convertToSpringMsg(
        org.apache.rocketmq.common.message.MessageExt message) {
        org.springframework.messaging.Message retMessage =
            MessageBuilder.withPayload(message.getBody()).
                setHeader(RocketMQMessageConst.KEYS, message.getKeys()).
                setHeader(RocketMQMessageConst.TAGS, message.getTags()).
                setHeader(RocketMQMessageConst.TOPIC, message.getTopic()).
                setHeader(RocketMQMessageConst.MESSAGE_ID, message.getMsgId()).
                setHeader(RocketMQMessageConst.BORN_TIMESTAMP, message.getBornTimestamp()).
                setHeader(RocketMQMessageConst.BORN_HOST, message.getBornHostString()).
                setHeader(RocketMQMessageConst.FLAG, message.getFlag()).
                setHeader(RocketMQMessageConst.QUEUE_ID, message.getQueueId()).
                setHeader(RocketMQMessageConst.SYS_FLAG, message.getSysFlag()).
                setHeader(RocketMQMessageConst.TRANSACTION_ID, message.getTransactionId()).
                setHeader(RocketMQMessageConst.PROPERTIES, message.getProperties()).
                build();

        return retMessage;
    }

    public static org.springframework.messaging.Message convertToSpringMsg(
        org.apache.rocketmq.common.message.Message message) {
        org.springframework.messaging.Message retMessage =
            MessageBuilder.withPayload(message.getBody()).
                setHeader(RocketMQMessageConst.KEYS, message.getKeys()).
                setHeader(RocketMQMessageConst.TAGS, message.getTags()).
                setHeader(RocketMQMessageConst.TOPIC, message.getTopic()).
                setHeader(RocketMQMessageConst.FLAG, message.getFlag()).
                setHeader(RocketMQMessageConst.TRANSACTION_ID, message.getTransactionId()).
                setHeader(RocketMQMessageConst.PROPERTIES, message.getProperties()).
                build();

        return retMessage;
    }

    /**
     * Convert spring message to rocketMQ message
     *
     * @param destination formats: `topicName:tags`
     * @param message     {@link org.springframework.messaging.Message}
     * @return instance of {@link org.apache.rocketmq.common.message.Message}
     */
    public static org.apache.rocketmq.common.message.Message convertToRocketMsg(
        ObjectMapper objectMapper, String charset,
        String destination, org.springframework.messaging.Message<?> message) {
        Object payloadObj = message.getPayload();
        byte[] payloads;

        if (payloadObj instanceof String) {
            payloads = ((String) payloadObj).getBytes(Charset.forName(charset));
        } else {
            try {
                String jsonObj = objectMapper.writeValueAsString(payloadObj);
                payloads = jsonObj.getBytes(Charset.forName(charset));
            } catch (Exception e) {
                throw new RuntimeException("convert to RocketMQ message failed.", e);
            }
        }

        String[] tempArr = destination.split(":", 2);
        String topic = tempArr[0];
        String tags = "";
        if (tempArr.length > 1) {
            tags = tempArr[1];
        }

        org.apache.rocketmq.common.message.Message rocketMsg = new org.apache.rocketmq.common.message.Message(topic, tags, payloads);

        MessageHeaders headers = message.getHeaders();
        if (Objects.nonNull(headers) && !headers.isEmpty()) {
            Object keys = headers.get(RocketMQMessageConst.KEYS);
            if (!StringUtils.isEmpty(keys)) { // if headers has 'KEYS', set rocketMQ message key
                rocketMsg.setKeys(keys.toString());
            }

            Object flagObj = headers.getOrDefault("FLAG", "0");
            int flag = 0;
            try {
                flag = Integer.parseInt(flagObj.toString());
            } catch (NumberFormatException e) {
                // Ignore it
                log.info("flag must be integer, flagObj:{}", flagObj);
            }
            rocketMsg.setFlag(flag);

            Object waitStoreMsgOkObj = headers.getOrDefault("WAIT_STORE_MSG_OK", "true");
            boolean waitStoreMsgOK = Boolean.TRUE.equals(waitStoreMsgOkObj);
            rocketMsg.setWaitStoreMsgOK(waitStoreMsgOK);

            headers.entrySet().stream()
                .filter(entry -> !Objects.equals(entry.getKey(), RocketMQMessageConst.KEYS)
                    && !Objects.equals(entry.getKey(), "FLAG")
                    && !Objects.equals(entry.getKey(), "WAIT_STORE_MSG_OK")) // exclude "KEYS", "FLAG", "WAIT_STORE_MSG_OK"
                .forEach(entry -> {
                    rocketMsg.putUserProperty("USERS_" + entry.getKey(), String.valueOf(entry.getValue())); // add other properties with prefix "USERS_"
                });

        }

        return rocketMsg;
    }
}
