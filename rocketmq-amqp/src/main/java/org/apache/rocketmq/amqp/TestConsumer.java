package org.apache.rocketmq.amqp;

import java.util.HashMap;
import java.util.Map;

public class TestConsumer {
    public static void main(String[] args) {
        EndpointIdentifier endpointIdentifier = new EndpointIdentifier();
        Map<String, String> messageProperties = new HashMap<>();
        messageProperties.put("topic","TopicTest11");
        messageProperties.put("consumerGroup","Group1");
        messageProperties.put("namesrv","localhost:9876");
        messageProperties.put("ep","consumer");

        try {
            endpointIdentifier.execute(messageProperties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
