package org.apache.rocketmq.amqp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class TestClient {
    private static Socket socket;

    public static void main(String[] args) {
        Map<String, String> messageProperties = new HashMap<>();
        messageProperties.put("topic","TopicTest11");
        messageProperties.put("tags","TagC");
        messageProperties.put("msg","Hello RocketMQ AMQP a");
        messageProperties.put("producerGroup","Group1");
        messageProperties.put("namesrv","localhost:9876");
        messageProperties.put("ep","producer");

        try {
            socket = new Socket(InetAddress.getByName("localhost"),5672);
            //Send the message to the server

            OutputStream os = socket.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(os);
            oos.writeObject(messageProperties);
            oos.flush();
            System.out.println("Message sent to server");
            /*OutputStreamWriter osw = new OutputStreamWriter(os);
            BufferedWriter bw = new BufferedWriter(osw);
            bw.write(String.valueOf(messageProperties));*/

            // receive message from server
            InputStream is = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String msg = br.readLine();
            System.out.println("Message received from server = " + msg);

        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
