package example.client;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

public class Publisher {

    private final static String QUEUE_NAME = "hello";

    public static void main(String[] argv)
        throws java.io.IOException, InterruptedException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("admin");
        factory.setPassword("admin");
        Connection connection = factory.newConnection();

        Channel channel = connection.createChannel();

       // channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        channel.exchangeDeclare("topic","topic");
        channel.queueBind(QUEUE_NAME,"topic","black");
        String message = "Hello world topic!";
        channel.basicPublish("amq.topic", "black", null, message.getBytes());
        System.out.println(" [x] Sent '" + message + "'");

        channel.close();
        connection.close();

    }
}
