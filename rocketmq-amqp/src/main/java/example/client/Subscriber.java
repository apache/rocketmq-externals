package example.client;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

public class Subscriber {

    private final static String QUEUE_NAME = "hello";

    public static void main(String[] argv)
            throws java.io.IOException,
            InterruptedException {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("admin");
        factory.setPassword("admin");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
       // channel.queueBind(QUEUE_NAME, "amq.topic", QUEUE_NAME);
        System.out.println(" [*] Waiting for messages.");
        channel.exchangeDeclare("amq.direct","direct");
        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(QUEUE_NAME, false,"amq.direct", consumer);

        while (true) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            System.out.println(delivery.getProperties().getHeaders());
            String message = new String(delivery.getBody());
            System.out.println(" [x] Received '" + message + "'");

            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }
    }
}