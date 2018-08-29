package org.apache.rocketmq.samples.springboot;

import org.apache.rocketmq.samples.springboot.domain.OrderPaidEvent;

import java.lang.System;
import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Resource;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.spring.starter.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.starter.core.RocketMQTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.messaging.support.MessageBuilder;

/**
 * ProducerApplication Created by aqlu on 2017/11/16.
 *
 */
@SpringBootApplication
public class ProducerApplication implements CommandLineRunner {
    private static final String TRANS_NAME = "myTxProducerGroup";
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    public static void main(String[] args) {
        SpringApplication.run(ProducerApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // send string
        SendResult sendResult = rocketMQTemplate.syncSend("string-topic", "Hello, World!");
        System.out.println("string-topic 1" + "  sendResult=" + sendResult);

        // send string with spring Message
        sendResult = rocketMQTemplate.syncSend("string-topic", MessageBuilder.withPayload("Hello, World! I'm from spring message").build());
        System.out.println("string-topic 2"+"  sendResult="+sendResult);

        // send user-defined object
        rocketMQTemplate.asyncSend("order-paid-topic", new OrderPaidEvent("T_001", new BigDecimal("88.00")), new SendCallback() {
            public void onSuccess(SendResult var1) {
                System.out.println("async onSucess SendResult=" + var1);
            }

            public void onException(Throwable var1) {
                System.out.println("async onException Throwable=" + var1);
            }

        });

        // send message with special tag
        rocketMQTemplate.convertAndSend("message-ext-topic:tag0", "I'm from tag0"); //not be consume
        rocketMQTemplate.convertAndSend("message-ext-topic:tag1", "I'm from tag1");

        // send transactional messages
        testTransaction();
    }


    private void testTransaction() throws MQClientException {
        String producerName = "txTest";

        String[] tags = new String[] {"TagA", "TagB", "TagC", "TagD", "TagE"};
        for (int i = 0; i < 10; i++) {
            try {

                org.apache.rocketmq.common.message.Message msg =
                    new org.apache.rocketmq.common.message.Message("string-topic", tags[i % tags.length], "KEY" + i,
                        ("Hello RocketMQ " + i).getBytes(RemotingHelper.DEFAULT_CHARSET));
                System.out.printf("send msg body = %s%n",new String(msg.getBody()));
                SendResult sendResult = rocketMQTemplate.sendMessageInTransaction(TRANS_NAME, msg, null);
                System.out.printf("XXXXX:   %s%n", sendResult);

                Thread.sleep(10);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @RocketMQTransactionListener(transName = TRANS_NAME)
    class TransactionListenerImpl implements TransactionListener {
        private AtomicInteger transactionIndex = new AtomicInteger(0);

        private ConcurrentHashMap<String, Integer> localTrans = new ConcurrentHashMap<String, Integer>();

        @Override
        public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
            System.out.println("executeLocalTransaction is executed !!!");
            int value = transactionIndex.getAndIncrement();
            int status = value % 3;
            localTrans.put(msg.getTransactionId(), status);
            return LocalTransactionState.UNKNOW;
        }

        @Override
        public LocalTransactionState checkLocalTransaction(MessageExt msg) {
            System.out.println("checkLocalTransaction is executed !!!");
            //(new RuntimeException()).printStackTrace();
            Integer status = localTrans.get(msg.getTransactionId());
            if (null != status) {
                switch (status) {
                    case 0:
                        return LocalTransactionState.UNKNOW;
                    case 1:
                        return LocalTransactionState.COMMIT_MESSAGE;
                    case 2:
                        return LocalTransactionState.ROLLBACK_MESSAGE;
                }
            }
            return LocalTransactionState.COMMIT_MESSAGE;
        }
    }

}
