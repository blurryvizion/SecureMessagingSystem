package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TestClient {

    public static void main(String[] args) {

        try {
            Socket socket = new Socket("localhost", 5000);

            BufferedReader console =
                    new BufferedReader(new InputStreamReader(System.in));

            PrintWriter writer =
                    new PrintWriter(socket.getOutputStream(), true);

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Connected to server. Type messages:");

            new Thread(() -> {
                try {
                    String response;

                    while ((response = reader.readLine()) != null) {

                        try {
                            response = EncryptionUtil.decrypt(response);
                        } catch (Exception ignored) {}

                        if (response.startsWith("PRIVATE:")) {

                            String[] parts = response.split(":", 3);
                            String sender = parts[1];
                            String text = parts[2];

                            System.out.println("PRIVATE from " + sender + ": " + text);
                        }

                        else if (response.startsWith("GROUP:")) {

                            String[] parts = response.split(":", 4);
                            String group = parts[1];
                            String sender = parts[2];
                            String text = parts[3];

                            System.out.println("GROUP [" + group + "] " + sender + ": " + text);
                        }

                        else if (response.startsWith("PUBLIC:")) {

                            String[] parts = response.split(":", 3);
                            String sender = parts[1];
                            String text = parts[2];

                            System.out.println("PUBLIC " + sender + ": " + text);
                        }

                        else {
                            System.out.println("Server says: " + response);
                        }
                    }

                } catch (Exception e) {
                    System.out.println("Disconnected from server.");
                }
            }).start();

            String message;
            while ((message = console.readLine()) != null) {
                writer.println(message);
            }

        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}