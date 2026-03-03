package com.smsproject;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class PrimaryController {

    @FXML private VBox messageBox;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField inputField;

    private String currentUser;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");

    @FXML
    public void initialize() {
        messageBox.heightProperty().addListener((obs, oldVal, newVal) ->
            scrollPane.setVvalue(1.0)
        );
    }

    public void setUsername(String username) {
        this.currentUser = username;
    }

    @FXML
    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (!msg.isEmpty()) {
            addMessage(msg, true);
            inputField.clear();
            simulateReply("Got it: \"" + msg + "\"");
        }
    }

    public void addMessage(String message, boolean isSent) {
        Label bubble = new Label(message);
        bubble.setWrapText(true);
        bubble.setMaxWidth(280);
        bubble.setPadding(new Insets(8, 12, 8, 12));

        if (isSent) {
            bubble.setStyle(
                "-fx-background-color: #0b93f6;" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 18 18 4 18;" +
                "-fx-font-size: 13px;"
            );
        } else {
            bubble.setStyle(
                "-fx-background-color: #e5e5ea;" +
                "-fx-text-fill: black;" +
                "-fx-background-radius: 18 18 18 4;" +
                "-fx-font-size: 13px;"
            );
        }

        Label timestamp = new Label(LocalTime.now().format(TIME_FORMAT));
        timestamp.setStyle("-fx-font-size: 10px; -fx-text-fill: #999999;");

        VBox bubbleWithTime = new VBox(2, bubble, timestamp);
        HBox row = new HBox(bubbleWithTime);
        row.setPadding(new Insets(4, 10, 4, 10));

        if (isSent) {
            row.setAlignment(Pos.CENTER_RIGHT);
            bubbleWithTime.setAlignment(Pos.CENTER_RIGHT);
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            bubbleWithTime.setAlignment(Pos.CENTER_LEFT);
        }

        messageBox.getChildren().add(row);
    }

    private void simulateReply(String replyText) {
        new Thread(() -> {
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            javafx.application.Platform.runLater(() -> addMessage(replyText, false));
        }).start();
    }
}