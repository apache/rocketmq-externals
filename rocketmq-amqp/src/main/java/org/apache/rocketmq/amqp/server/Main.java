package org.apache.rocketmq.amqp.server;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        AMQPServer amqpServer = new AMQPServer();
        amqpServer.execute();
    }
}
