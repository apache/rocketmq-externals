package org.apache.rocketmq.amqp.message;

import java.util.Map;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;

public class AMQPSender {



    public void producer(Map msgProp) throws Exception {

        DefaultMQProducer producer = new DefaultMQProducer(msgProp.get("producerGroup").toString());
        producer.setNamesrvAddr(msgProp.get("namesrv").toString());
        producer.start();
        Message msg = new Message(msgProp.get("topic").toString()/* Topic */,
            msgProp.get("tags").toString() /* Tag */,
            msgProp.get("msg").toString().getBytes(RemotingHelper.DEFAULT_CHARSET) /* Message body */
        );

        SendResult sendResult = producer.send(msg);
        System.out.printf("%s%n", sendResult);
        producer.shutdown();

    }
}
