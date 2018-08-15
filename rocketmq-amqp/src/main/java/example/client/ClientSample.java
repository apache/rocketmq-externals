package example.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class ClientSample {
    private Socket socket;
    private  Scanner scanner;
    private ClientSample(InetAddress serverAddress, int serverPort)
    {
        try {
            this.socket = new Socket(serverAddress,serverPort);
            this.scanner = new Scanner(System.in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void start()
    {
        String input;
        while (true)
        {

            input = scanner.nextLine();
            PrintWriter out = null;
            try {
                out = new PrintWriter(this.socket.getOutputStream(), true);
                out.println(input);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
    public static void main(String[] args) {

        ClientSample client = null;
            try {
                client = new ClientSample(InetAddress.getByName("localhost"), 5672);
                System.out.println("client.socket.getPort() = " + client.socket.getPort());
               client.start();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
    }
}
