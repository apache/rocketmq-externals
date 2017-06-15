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
package org.apache.rocketmq.console.controller;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;
import java.util.List;
import javax.annotation.Resource;
import org.apache.rocketmq.console.config.RMQConfigure;
import org.apache.rocketmq.console.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/test")
public class TestController {
    private Logger logger = LoggerFactory.getLogger(TestController.class);
    private String testTopic = "TestTopic";

    @Resource
    private RMQConfigure rMQConfigure;

    @RequestMapping(value = "/runTask.do", method = RequestMethod.GET)
    @ResponseBody
    public Object list() throws MQClientException, RemotingException, InterruptedException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(testTopic + "Group");
        consumer.setNamesrvAddr(rMQConfigure.getNamesrvAddr());
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        consumer.subscribe(testTopic, "*");
        consumer.registerMessageListener(new MessageListenerConcurrently() {

            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                ConsumeConcurrentlyContext context) {
                logger.info("receiveMessage msgSize={}", msgs.size());
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        consumer.start();
        final DefaultMQProducer producer = new DefaultMQProducer(testTopic + "Group");
        producer.setInstanceName(String.valueOf(System.currentTimeMillis()));
        producer.setNamesrvAddr(rMQConfigure.getNamesrvAddr());
        producer.start();

        new Thread(new Runnable() {

            @Override public void run() {

                int i = 0;
                while (true) {
                    try {
                        Message msg = new Message(testTopic,
                            "TagA" + i,
                            "KEYS" + i,
                            ("Hello RocketMQ " + i).getBytes()
                        );
                        Thread.sleep(1000L);
                        SendResult sendResult = producer.send(msg);
                        logger.info("sendMessage={}", JsonUtil.obj2String(sendResult));
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        try {
                            Thread.sleep(1000);
                        }
                        catch (Exception ignore) {
                        }
                    }
                }
            }
        }).start();
        return true;
    }
}
