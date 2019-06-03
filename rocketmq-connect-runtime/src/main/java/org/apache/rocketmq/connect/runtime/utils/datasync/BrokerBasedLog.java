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
import io.openmessaging.Future;
import io.openmessaging.Message;
import io.openmessaging.MessagingAccessPoint;
import io.openmessaging.OMS;
import io.openmessaging.OMSBuiltinKeys;
import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.apache.rocketmq.connect.runtime.config.RuntimeConfigDefine;
import io.openmessaging.connector.api.data.Converter;
import io.openmessaging.consumer.PushConsumer;
import io.openmessaging.producer.Producer;
import io.openmessaging.producer.SendResult;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Broker base data synchronizer, synchronize data between workers.
 * @param <K>
 * @param <V>
 */
public class BrokerBasedLog<K, V> implements DataSynchronizer<K, V>{

    private static final Logger log = LoggerFactory.getLogger(LoggerName.OMS_RUNTIME);

    /**
     * A callback to receive data from other workers.
     */
    private DataSynchronizerCallback<K, V> dataSynchronizerCallback;

    /**
     * Producer to send data to broker.
     */
    private Producer producer;

    /**
     * Consumer to receive synchronize data from broker.
     */
    private PushConsumer consumer;

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

    public BrokerBasedLog(MessagingAccessPoint messagingAccessPoint,
                          String queueName,
                          String consumerId,
                          DataSynchronizerCallback<K, V> dataSynchronizerCallback,
                          Converter keyConverter,
                          Converter valueConverter){

        this.queueName = queueName;
        this.dataSynchronizerCallback = dataSynchronizerCallback;
        producer = messagingAccessPoint.createProducer();
        consumer = messagingAccessPoint.createPushConsumer(
                                            OMS.newKeyValue().put(OMSBuiltinKeys.CONSUMER_ID, consumerId));
        this.keyConverter = keyConverter;
        this.valueConverter = valueConverter;
    }

    @Override
    public void start() {

        producer.startup();

        consumer.attachQueue(queueName, (message, context) -> {

            try {

                // Need openMessaging to support start consume message from tail.
                if(Long.parseLong(message.sysHeaders().getString(Message.BuiltinKeys.BORN_TIMESTAMP)) + 10000 < System.currentTimeMillis()){
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
            }catch(Exception e){
                log.error("BrokerBasedLog process message failed.", e);
            }
        });
        consumer.startup();
    }

    @Override
    public void stop(){

        producer.shutdown();
        consumer.shutdown();
    }

    @Override
    public void send(K key, V value){

        try {
            byte[] messageBody = encodeKeyValue(key, value);
            if (messageBody.length > RuntimeConfigDefine.MAX_MESSAGE_SIZE) {
                log.error("Message size is greater than {} bytes, key: {}, value {}", RuntimeConfigDefine.MAX_MESSAGE_SIZE, key, value);
                return;
            }
            Future<SendResult> result = producer.sendAsync(producer.createBytesMessage(queueName, messageBody));
            result.addListener((future) -> {

                if (future.getThrowable() != null) {
                    log.error("Send async message Failed, error: {}", future.getThrowable());
                } else {
                    log.info("Send async message OK, msgId: {}", future.get().messageId() + "\n");
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
        for(String key : map.keySet()){
            K decodeKey = (K)keyConverter.byteToObject(Base64.getDecoder().decode(key));
            V decodeValue = (V)valueConverter.byteToObject(Base64.getDecoder().decode(map.get(key)));
            resultMap.put(decodeKey, decodeValue);
        }
        return resultMap;
    }

}
