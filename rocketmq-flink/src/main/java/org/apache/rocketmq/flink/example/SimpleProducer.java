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

package org.apache.rocketmq.flink.example;

import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.flink.RocketMQSource;
import org.apache.rocketmq.remoting.RPCHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleProducer {

    private static final Logger log = LoggerFactory.getLogger(SimpleProducer.class);

    private static final int MESSAGE_NUM = 10000;

    // Producer config
    private static final String NAME_SERVER_ADDR = "http://${instanceId}.${region}.mq-internal.aliyuncs.com:8080";
    private static final String PRODUCER_GROUP = "GID_SIMPLE_PRODUCER";
    private static final String TOPIC = "SOURCE_TOPIC";
    private static final String TAGS = "*";
    private static final String KEY_PREFIX = "KEY";

    private static RPCHook getAclRPCHook() {
        final String ACCESS_KEY = "${AccessKey}";
        final String SECRET_KEY = "${SecretKey}";
        return new AclClientRPCHook(new SessionCredentials(ACCESS_KEY, SECRET_KEY));
    }

    public static void main(String[] args) {
        DefaultMQProducer producer = new DefaultMQProducer(
                PRODUCER_GROUP, getAclRPCHook(), true, null);
        producer.setNamesrvAddr(NAME_SERVER_ADDR);

        // When using aliyun products, you need to set up channels
        producer.setAccessChannel(AccessChannel.CLOUD);

        try {
            producer.start();
        } catch (MQClientException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < MESSAGE_NUM; i++) {
            String content = "Test Message " + i;
            Message msg = new Message(TOPIC, TAGS, KEY_PREFIX + i, content.getBytes());
            try {
                SendResult sendResult = producer.send(msg);
                assert sendResult != null;
                System.out.printf("send result: %s %s\n",
                        sendResult.getMsgId(), sendResult.getMessageQueue().toString());
                Thread.sleep(50);
            } catch (Exception e) {
                log.info("send message failed. {}", e.toString());
            }
        }
    }
}
