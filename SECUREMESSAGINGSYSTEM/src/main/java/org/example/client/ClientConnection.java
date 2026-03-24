package org.example.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.example.EncryptionUtil;

import javafx.application.Platform;

public class ClientConnection {

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private FXClient ui;

    public ClientConnection(FXClient ui) {
        this.ui = ui;

        try {
            socket = new Socket("localhost", 5000);

            reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            writer = new PrintWriter(
                    socket.getOutputStream(), true
            );

            listen();

        } catch (Exception e) {
            Platform.runLater(() ->
                    ui.handleServerMessage("Could not connect to server. Start ChatServer first."));
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public void send(String message) {
        try {
            String encrypted = EncryptionUtil.encrypt(message);
            writer.println(encrypted);
        } catch (Exception e) {
            Platform.runLater(() ->
                    ui.handleServerMessage("Failed to send message."));
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private void listen() {
        new Thread(() -> {
            try {
                String response;

                while ((response = reader.readLine()) != null) {
                    try {
                        response = EncryptionUtil.decrypt(response);
                    } catch (Exception ignored) {
                    }

                    String finalResponse = response;
                    Platform.runLater(() -> ui.handleServerMessage(finalResponse));
                }

            } catch (Exception e) {
                Platform.runLater(() ->
                        ui.handleServerMessage("Disconnected from server."));
            }
        }).start();
    }
}