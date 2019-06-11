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

package org.apache.rocketmq.connect.runtime.utils.datasync;

import com.alibaba.fastjson.JSON;
import io.openmessaging.connector.api.data.Converter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.apache.rocketmq.connect.runtime.config.ConnectConfig;
import org.apache.rocketmq.connect.runtime.config.RuntimeConfigDefine;
import org.apache.rocketmq.connect.runtime.exception.RocketMQRuntimeException;
import org.apache.rocketmq.remoting.protocol.LanguageCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Broker base data synchronizer, synchronize data between workers.
 *
 * @param <K>
 * @param <V>
 */
public class BrokerBasedLog<K, V> implements DataSynchronizer<K, V> {

    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_RUNTIME);

    /**
     * A callback to receive data from other workers.
     */
    private DataSynchronizerCallback<K, V> dataSynchronizerCallback;

    /**
     * Producer to send data to broker.
     */
    private DefaultMQProducer producer;

    /**
     * Consumer to receive synchronize data from broker.
     */
    private DefaultMQPushConsumer consumer;

    /**
     * A queue to send or consume message.
     */
    private String queueName;

    /**
     * Used to convert key to byte[].
     */
    private Converter keyConverter;

    /**
     * Used to convert value to byte[].
     */
    private Converter valueConverter;

    public BrokerBasedLog(ConnectConfig connectConfig,
        String queueName,
        String consumerId,
        DataSynchronizerCallback<K, V> dataSynchronizerCallback,
        Converter keyConverter,
        Converter valueConverter) {

        this.queueName = queueName;
        this.dataSynchronizerCallback = dataSynchronizerCallback;
        producer = new DefaultMQProducer();
        this.producer.setNamesrvAddr(connectConfig.getNamesrvAddr());
        this.producer.setProducerGroup(connectConfig.getRmqProducerGroup());
        this.producer.setSendMsgTimeout(connectConfig.getOperationTimeout());
//        this.rocketmqProducer.setInstanceName(accessPoints);
        this.producer.setMaxMessageSize(4194304);
        this.producer.setLanguage(LanguageCode.JAVA);

        consumer = new DefaultMQPushConsumer();
        this.consumer.setNamesrvAddr(connectConfig.getNamesrvAddr());
        String consumerGroup = connectConfig.getRmqConsumerGroup();
        if (null != consumerGroup && !consumerGroup.isEmpty()) {
            this.consumer.setConsumerGroup(consumerGroup);
            this.consumer.setMaxReconsumeTimes(connectConfig.getRmqMaxRedeliveryTimes());
            this.consumer.setConsumeTimeout((long) connectConfig.getRmqMessageConsumeTimeout());
            this.consumer.setConsumeThreadMax(connectConfig.getRmqMaxConsumeThreadNums());
            this.consumer.setConsumeThreadMin(connectConfig.getRmqMinConsumeThreadNums());
//            String consumerId = OMSUtil.buildInstanceName();
//            this.consumer.setInstanceName(consumerId);
            this.consumer.setLanguage(LanguageCode.JAVA);
            consumer.registerMessageListener(new MessageListenerImpl());
//            this.consumer.registerMessageListener(new PushConsumerImpl.MessageListenerImpl());
        } else {
            throw new RocketMQRuntimeException(-1, "Consumer Group is necessary for RocketMQ, please set it.");
        }
        this.keyConverter = keyConverter;
        this.valueConverter = valueConverter;
    }

    @Override
    public void start() {
        try {
            producer.start();
            consumer.subscribe(queueName, "*");
            consumer.start();
        } catch (MQClientException e) {
            log.error("Start error.", e);
        }
/*        consumer.subscribe(queueName, (message, context) -> {

            try {

                // Need openMessaging to support start consume message from tail.
                if (Long.parseLong(message.sysHeaders().getString(Message.BuiltinKeys.BORN_TIMESTAMP)) + 10000 < System.currentTimeMillis()) {
                    context.ack();
                    return;
                }
                log.info("Received one message: {}", message.sysHeaders().getString(Message.BuiltinKeys.MESSAGE_ID) + "\n");
                byte[] bytes = message.getBody(byte[].class);
                Map<K, V> map = decodeKeyValue(bytes);
                for (K key : map.keySet()) {
                    dataSynchronizerCallback.onCompletion(null, key, map.get(key));
                }
                context.ack();
            } catch (Exception e) {
                log.error("BrokerBasedLog process message failed.", e);
            }
        });*/
    }

    @Override
    public void stop() {
        producer.shutdown();
        consumer.shutdown();
    }

    @Override
    public void send(K key, V value) {

        try {
            byte[] messageBody = encodeKeyValue(key, value);
            if (messageBody.length > RuntimeConfigDefine.MAX_MESSAGE_SIZE) {
                log.error("Message size is greater than {} bytes, key: {}, value {}", RuntimeConfigDefine.MAX_MESSAGE_SIZE, key, value);
                return;
            }
            producer.send(new Message(queueName, messageBody), new SendCallback() {
                @Override public void onSuccess(org.apache.rocketmq.client.producer.SendResult result) {
                    log.info("Send async message OK, msgId: {}", result.getMsgId());
                }

                @Override public void onException(Throwable throwable) {
                    if (null != throwable) {
                        log.error("Send async message Failed, error: {}", throwable);
                    }
                }
            });

/*            Future<SendResult> result = producer.sendAsync(producer.createBytesMessage(queueName, messageBody));
            result.addListener((future) -> {

                if (future.getThrowable() != null) {
                    log.error("Send async message Failed, error: {}", future.getThrowable());
                } else {
                    log.info("Send async message OK, msgId: {}", future.get().messageId() + "\n");
                }
            });*/
        } catch (Exception e) {
            log.error("BrokerBaseLog send async message Failed.", e);
        }
    }

    private byte[] encodeKeyValue(K key, V value) throws Exception {

        byte[] keyByte = keyConverter.objectToByte(key);
        byte[] valueByte = valueConverter.objectToByte(value);
        Map<String, String> map = new HashMap<>();
        map.put(Base64.getEncoder().encodeToString(keyByte), Base64.getEncoder().encodeToString(valueByte));

        return JSON.toJSONString(map).getBytes("UTF-8");
    }

    private Map<K, V> decodeKeyValue(byte[] bytes) throws Exception {

        Map<K, V> resultMap = new HashMap<>();
        String rawString = new String(bytes, "UTF-8");
        Map<String, String> map = JSON.parseObject(rawString, Map.class);
        for (String key : map.keySet()) {
            K decodeKey = (K) keyConverter.byteToObject(Base64.getDecoder().decode(key));
            V decodeValue = (V) valueConverter.byteToObject(Base64.getDecoder().decode(map.get(key)));
            resultMap.put(decodeKey, decodeValue);
        }
        return resultMap;
    }

    class MessageListenerImpl implements MessageListenerConcurrently {
        @Override
        public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> rmqMsgList,
            ConsumeConcurrentlyContext context) {
            for (MessageExt messageExt : rmqMsgList) {
/*                if (messageExt.getBornTimestamp() + 10000 < System.currentTimeMillis()) {
//                    contextRMQ.ack();
//                        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }*/
                log.info("Received one message: {}", messageExt.getMsgId() + "\n");
                byte[] bytes = messageExt.getBody();
                Map<K, V> map;
                try {
                    map = decodeKeyValue(bytes);
                } catch (Exception e) {
                    log.error("Decode message data error. message: {}, error info: {}", messageExt, e);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
                for (K key : map.keySet()) {
                    dataSynchronizerCallback.onCompletion(null, key, map.get(key));
                }
//                context.ack();
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        }
      /*      try {

                // Need openMessaging to support start consume message from tail.
                if (Long.parseLong(message.sysHeaders().getString(Message.BuiltinKeys.BORN_TIMESTAMP)) + 10000 < System.currentTimeMillis()) {
                    context.ack();
                    return;
                }
                log.info("Received one message: {}", message.sysHeaders().getString(Message.BuiltinKeys.MESSAGE_ID) + "\n");
                byte[] bytes = message.getBody(byte[].class);
                Map<K, V> map = decodeKeyValue(bytes);
                for (K key : map.keySet()) {
                    dataSynchronizerCallback.onCompletion(null, key, map.get(key));
                }
                context.ack();
            } catch (Exception e) {
                log.error("BrokerBasedLog process message failed.", e);
            }*/
    }

}
