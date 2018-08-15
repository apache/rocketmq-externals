package org.apache.rocketmq.amqp.message;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;

public class AMQPReceiver {
    
    public void consumer(Map msg) throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(msg.get("consumerGroup").toString());
        consumer.setNamesrvAddr(msg.get("namesrv").toString());
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        consumer.subscribe(msg.get("topic").toString(), "*");
        consumer.registerMessageListener(   new MessageListenerConcurrently() {

            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                ConsumeConcurrentlyContext context) {
                System.out.printf("%s Receive New Messages: %s %n", Thread.currentThread().getName(), msgs);
                /*Iterator<MessageExt> a = msgs.iterator();
                for (MessageExt ext : msgs) {
                    String s = new String(ext.getBody());
                    System.out.println("ext.getBody() = " + s);
                }*/

                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        consumer.start();
    }
}
