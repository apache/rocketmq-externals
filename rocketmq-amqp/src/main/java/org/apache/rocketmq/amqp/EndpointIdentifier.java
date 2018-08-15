package org.apache.rocketmq.amqp;

import java.util.Map;
import org.apache.rocketmq.amqp.message.AMQPReceiver;
import org.apache.rocketmq.amqp.message.AMQPSender;

public class EndpointIdentifier {
    public String findEndpoint()
    {
        String endpoint ="producer";


        return endpoint;
    }
    public void execute(Map messageProperties) throws Exception {
        String endpoint = messageProperties.get("ep").toString();
        switch (endpoint) {
            case "producer" :
              AMQPSender sender = new AMQPSender();
              sender.producer(messageProperties);
                break;

            case "consumer" :
                AMQPReceiver receiver = new AMQPReceiver();
                receiver.consumer(messageProperties);
                break;

            case "broker" :
                break;
            default:
                endpointError();
        }

    }

    public String endpointError()
    {
        return "error in finding end point action";
    }
}
