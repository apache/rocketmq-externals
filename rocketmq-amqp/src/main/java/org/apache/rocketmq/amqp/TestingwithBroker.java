package org.apache.rocketmq.amqp;

import java.util.HashMap;
import java.util.Map;

public class TestingwithBroker {

    public static void main(String[] args) {
        EndpointIdentifier endpointIdentifier = new EndpointIdentifier();
        Map<String, String> messageProperties = new HashMap<>();
        messageProperties.put("topic","TopicTest11");
        messageProperties.put("tags","TagB");
        messageProperties.put("msg","Hello RocketMQ");
        messageProperties.put("producerGroup","Group1");
        messageProperties.put("namesrv","localhost:9876");
        messageProperties.put("ep","producer");

        try {
            System.out.println("messageProperties = " + messageProperties);
            endpointIdentifier.execute(messageProperties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
