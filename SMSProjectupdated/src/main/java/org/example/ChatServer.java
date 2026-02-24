package org.example;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {

    public static ConcurrentHashMap<String, ClientHandler> onlineUsers =
            new ConcurrentHashMap<>();

    public static void main(String[] args) {

        int port = 5000;

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("=================================");
            System.out.println(" Secure Messaging Server Started ");
            System.out.println(" Listening on port " + port);
            System.out.println("=================================");

            while (true) {

                Socket socket = serverSocket.accept();
                System.out.println("New client connected from: "
                        + socket.getInetAddress());

                ClientHandler handler = new ClientHandler(socket);
                Thread thread = new Thread(handler);
                thread.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}