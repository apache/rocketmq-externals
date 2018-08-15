package org.apache.rocketmq.amqp.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AMQPServer {

    private int amqpPort = 5678;

    public void execute() throws IOException {
        runServer(amqpPort);
    }

    public void runServer(int port) throws IOException {
        ServerSocket serverSocket = null;
        Socket socket = null;

        serverSocket = new ServerSocket(port);
        while (true)
        {
            socket = serverSocket.accept();
            //new thread for a client
            new ClientThread(socket).start();
        }
    }

}
