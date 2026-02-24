package org.example;

import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String username;

    public ClientHandler(Socket socket) throws Exception {
        this.socket = socket;
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {

        try {
            String message;

            while ((message = reader.readLine()) != null) {

                // Decrypt incoming message
                try {
                    message = EncryptionUtil.decrypt(message);
                } catch (Exception ignored) {
                    // LOGIN might not be encrypted initially
                }

                System.out.println("Received: " + message);

                if (message.startsWith("LOGIN:")) {
                    handleLogin(message);
                }

                else if (message.startsWith("SIGNUP:")) {
                    handleSignup(message);
                }

                else if (message.equals("SIGNOUT")) {
                    handleSignout();
                    break;
                }

                else if (message.startsWith("PUBLIC:")) {
                    handlePublicMessage(message);
                }

                else if (message.startsWith("PRIVATE:")) {
                    handlePrivateMessage(message);
                }

                else if (message.startsWith("GROUP:")) {
                    handleGroupMessage(message);
                }

                else if (message.equals("ONLINE_USERS")) {
                    sendEncrypted(String.join(",", ChatServer.onlineUsers.keySet()));
                }

                else {
                    sendEncrypted("UNKNOWN_COMMAND");
                }
            }

        } catch (Exception e) {
            System.out.println("Client disconnected.");
        } finally {
            if (username != null) {
                ChatServer.onlineUsers.remove(username);
                System.out.println(username + " went offline.");
            }
        }
    }

    // ---------------- LOGIN ----------------

    private void handleLogin(String message) {

        try {
            String[] parts = message.split(":");

            if (parts.length < 3) {
                sendEncrypted("INVALID_LOGIN_FORMAT");
                return;
            }

            String user = parts[1];
            String pass = parts[2];

            Connection conn = DBConnection.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM users WHERE username = ? AND password = ?"
            );

            stmt.setString(1, user);
            stmt.setString(2, pass);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                this.username = user;
                ChatServer.onlineUsers.put(user, this);
                sendEncrypted("LOGIN_SUCCESS");
                System.out.println(user + " logged in.");
            } else {
                sendEncrypted("LOGIN_FAILED");
            }

            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
            sendEncrypted("LOGIN_ERROR");
        }
    }
    //---------------- SIGN UP ------------------
    private void handleSignup(String message) {

        try {
            String[] parts = message.split(":");

            if (parts.length < 3) {
                sendEncrypted("INVALID_SIGNUP_FORMAT");
                return;
            }

            String user = parts[1];
            String pass = parts[2];

            Connection conn = DBConnection.getConnection();

            // Check if user already exists
            PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT * FROM users WHERE username = ?"
            );
            checkStmt.setString(1, user);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                sendEncrypted("SIGNUP_FAILED_USERNAME_EXISTS");
            } else {

                PreparedStatement insertStmt = conn.prepareStatement(
                        "INSERT INTO users (username, password) VALUES (?, ?)"
                );

                insertStmt.setString(1, user);
                insertStmt.setString(2, pass);
                insertStmt.executeUpdate();

                sendEncrypted("SIGNUP_SUCCESS");
            }

            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
            sendEncrypted("SIGNUP_ERROR");
        }
    }


    // ---------------- SIGN OUT ---------------


    private void handleSignout() {

        try {
            if (username != null) {
                ChatServer.onlineUsers.remove(username);
                System.out.println(username + " signed out.");
            }

            sendEncrypted("SIGNOUT_SUCCESS");

            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------- PUBLIC MESSAGE ----------------

    private void handlePublicMessage(String message) {

        if (username == null) {
            sendEncrypted("LOGIN_REQUIRED");
            return;
        }

        String text = message.substring(7);

        saveMessage(username, "PUBLIC", text);

        for (ClientHandler client : ChatServer.onlineUsers.values()) {
            client.sendEncrypted("PUBLIC:" + username + ":" + text);
        }
    }

    // ---------------- PRIVATE MESSAGE ----------------

    private void handlePrivateMessage(String message) {

        if (username == null) {
            sendEncrypted("LOGIN_REQUIRED");
            return;
        }

        try {
            String[] parts = message.split(":", 3);

            if (parts.length < 3) {
                sendEncrypted("INVALID_PRIVATE_FORMAT");
                return;
            }

            String targetUser = parts[1];
            String text = parts[2];

            ClientHandler target = ChatServer.onlineUsers.get(targetUser);

            if (target != null) {


                target.sendEncrypted("PRIVATE:" + username + ":" + text);

                saveMessage(username, targetUser, text);

            } else {
                sendEncrypted("USER_NOT_ONLINE");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------- GROUP MESSAGE ----------------

    private void handleGroupMessage(String message) {

        if (username == null) {
            sendEncrypted("LOGIN_REQUIRED");
            return;
        }

        try {
            String[] parts = message.split(":", 3);
            String groupName = parts[1];
            String text = parts[2];

            Connection conn = DBConnection.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT username FROM group_members WHERE group_name = ?"
            );

            stmt.setString(1, groupName);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String member = rs.getString("username");
                ClientHandler target = ChatServer.onlineUsers.get(member);

                if (target != null) {
                    target.sendEncrypted(
                            "GROUP:" + groupName + ":" + username + ":" + text
                    );
                }
            }

            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------- SAVE MESSAGE ----------------

    private void saveMessage(String sender, String receiver, String content) {

        try {
            Connection conn = DBConnection.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO messages (sender, receiver, content) VALUES (?, ?, ?)"
            );

            stmt.setString(1, sender);
            stmt.setString(2, receiver);
            stmt.setString(3, content);

            stmt.executeUpdate();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------- ENCRYPTED SEND ----------------

    private void sendEncrypted(String message) {
        try {
            String encrypted = EncryptionUtil.encrypt(message);
            writer.println(encrypted);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}