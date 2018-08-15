package org.apache.rocketmq.amqp.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Map;
import org.apache.rocketmq.amqp.EndpointIdentifier;

public class ClientThread extends Thread {
    protected Socket socket;

    public ClientThread (Socket clientSocket)
    {
        this.socket = clientSocket;
    }
    public  void  run()
    {
        try {
            //read message from client
            InputStream is = socket.getInputStream();
            ObjectInputStream ois = new ObjectInputStream(is);
            Map msg = (Map)ois.readObject();
          /*  InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);*/
           // String msg = br.readLine();
            System.out.println("msg from client= " + msg);
            //communicate with broker

            EndpointIdentifier endpointIdentifier = new EndpointIdentifier();
            endpointIdentifier.execute(msg);

            //Send back the response to client
            OutputStream os = socket.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os);
            BufferedWriter bw = new BufferedWriter(osw);
            bw.write("Message sent to broker");
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        /*
        String line;
        InputStream inputStream = null;
        BufferedReader bufferedReaderInput = null;
        DataOutputStream dataOutputStream = null;

        try {
            inputStream = socket.getInputStream();
            bufferedReaderInput = new BufferedReader(new InputStreamReader(inputStream));
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true)
        {
            System.out.println("socketinit = " + socket.getPort());
            try {
                line = bufferedReaderInput.readLine();
                if (line==null||line.equalsIgnoreCase("QUIT"))
                {
                    socket.close();
                    return;
                }
                else
                {
                    System.out.println("socket = " + socket.getPort());
                    //dataOutputStream.writeBytes(line+"\n\r");
                    System.out.println(line);
                    dataOutputStream.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();


            }
        }*/
    }
}
