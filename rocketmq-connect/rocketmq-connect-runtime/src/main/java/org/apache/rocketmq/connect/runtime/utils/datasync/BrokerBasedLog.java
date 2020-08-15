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
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.apache.rocketmq.connect.runtime.config.ConnectConfig;
import org.apache.rocketmq.connect.runtime.utils.ConnectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.rocketmq.connect.runtime.config.RuntimeConfigDefine.MAX_MESSAGE_SIZE;

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
    private String topicName;

    /**
     * Used to convert key to byte[].
     */
    private Converter keyConverter;

    /**
     * Used to convert value to byte[].
     */
    private Converter valueConverter;

    public BrokerBasedLog(ConnectConfig connectConfig,
        String topicName,
        String workId,
        DataSynchronizerCallback<K, V> dataSynchronizerCallback,
        Converter keyConverter,
        Converter valueConverter) {

        this.topicName = topicName;
        this.dataSynchronizerCallback = dataSynchronizerCallback;
        this.producer = new DefaultMQProducer();
        this.producer.setNamesrvAddr(connectConfig.getNamesrvAddr());
        this.producer.setInstanceName(ConnectUtil.createInstance(connectConfig.getNamesrvAddr()));
        this.producer.setProducerGroup(workId);
        this.producer.setSendMsgTimeout(connectConfig.getOperationTimeout());
        this.producer.setMaxMessageSize(MAX_MESSAGE_SIZE);

        this.consumer = new DefaultMQPushConsumer();
        this.consumer.setNamesrvAddr(connectConfig.getNamesrvAddr());
        this.consumer.setInstanceName(ConnectUtil.createInstance(connectConfig.getNamesrvAddr()));
        this.consumer.setConsumerGroup(workId);
        this.consumer.setMaxReconsumeTimes(connectConfig.getRmqMaxRedeliveryTimes());
        this.consumer.setConsumeTimeout((long) connectConfig.getRmqMessageConsumeTimeout());
        this.consumer.setConsumeThreadMin(connectConfig.getRmqMinConsumeThreadNums());
        this.consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        this.keyConverter = keyConverter;
        this.valueConverter = valueConverter;
    }

    @Override
    public void start() {
        try {
            producer.start();
            consumer.subscribe(topicName, "*");
            consumer.registerMessageListener(new MessageListenerImpl());
            consumer.start();
        } catch (MQClientException e) {
            log.error("Start error.", e);
        }
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
            if (messageBody.length > MAX_MESSAGE_SIZE) {
                log.error("Message size is greater than {} bytes, key: {}, value {}", MAX_MESSAGE_SIZE, key, value);
                return;
            }
            producer.send(new Message(topicName, messageBody), new SendCallback() {
                @Override public void onSuccess(org.apache.rocketmq.client.producer.SendResult result) {
                    log.info("Send async message OK, msgId: {}", result.getMsgId());
                }

                @Override public void onException(Throwable throwable) {
                    if (null != throwable) {
                        log.error("Send async message Failed, error: {}", throwable);
                    }
                }
            });
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
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        }
    }

}
