package org.apache.rocketmq.samples.springboot.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.starter.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.starter.core.RocketMQListener;
import org.springframework.stereotype.Service;

/**
 * StringConsumer Created by aqlu on 2017/11/16.
 */
@Slf4j
@Service
@RocketMQMessageListener(topic = "${spring.rocketmq.topic}", consumerGroup = "string_consumer")
public class StringConsumer implements RocketMQListener<String> {
    @Override
    public void onMessage(String message) {
        log.info("------- StringConsumer received: {}", message);
    }
}
