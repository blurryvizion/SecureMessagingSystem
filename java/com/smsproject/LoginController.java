package com.smsproject;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    // Hardcoded users — swap with a DB/service layer later
    private static final Map<String, String> USERS = new HashMap<>() {{
        put("alice", "password123");
        put("bob",   "qwerty");
    }};

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (USERS.containsKey(username) && USERS.get(username).equals(password)) {
            loadSmsView(username);
        } else {
            errorLabel.setText("Invalid username or password.");
            errorLabel.setVisible(true);
            passwordField.clear();
        }
    }

    private void loadSmsView(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/smsproject/primary.fxml"));
            Parent root = loader.load();

            // Optional: pass username to the SMS controller
            PrimaryController controller = loader.getController();
            controller.setUsername(username);

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root, 400, 600));
            stage.setTitle("SMS - " + username);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}