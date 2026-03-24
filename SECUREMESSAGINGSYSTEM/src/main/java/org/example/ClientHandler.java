package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

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
                try {
                    message = EncryptionUtil.decrypt(message);
                } catch (Exception ignored) {
                }

                System.out.println("Received: " + message);

                if (message.startsWith("LOGIN:")) {
                    handleLogin(message);
                } else if (message.startsWith("SIGNUP:")) {
                    handleSignup(message);
                } else if (message.equals("SIGNOUT")) {
                    handleSignout();
                    break;
                } else if (message.equals("ONLINE_USERS")) {
                    sendEncrypted(String.join(",", ChatServer.onlineUsers.keySet()));
                } else if (message.equals("HISTORY:PUBLIC")) {
                    sendPublicHistory();
                } else if (message.startsWith("HISTORY:PRIVATE:")) {
                    sendPrivateHistory(message);
                } else if (message.startsWith("HISTORY:GROUP:")) {
                    sendGroupHistory(message);
                } else if (message.startsWith("PUBLIC:")) {
                    handlePublicMessage(message);
                } else if (message.startsWith("PRIVATE:")) {
                    handlePrivateMessage(message);
                } else if (message.startsWith("CREATE_GROUP:")) {
                    handleCreateGroup(message);
                } else if (message.equals("LIST_GROUPS")) {
                    handleListGroups();
                } else if (message.startsWith("LEAVE_GROUP:")) {
                    handleLeaveGroup(message);
                } else if (message.startsWith("DELETE_GROUP:")) {
                    handleDeleteGroup(message);
                } else if (message.startsWith("GROUP:")) {
                    handleGroupMessage(message);
                } else {
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
                sendPendingPrivateMessages(user);
            } else {
                sendEncrypted("LOGIN_FAILED");
            }

            conn.close();

        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            sendEncrypted("LOGIN_ERROR");
        }
    }

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
            System.err.println("Server error: " + e.getMessage());
            sendEncrypted("SIGNUP_ERROR");
        }
    }

    private void handleSignout() {
        try {
            if (username != null) {
                ChatServer.onlineUsers.remove(username);
            }

            sendEncrypted("SIGNOUT_SUCCESS");
            socket.close();

        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private void handlePublicMessage(String message) {
        if (username == null) {
            sendEncrypted("LOGIN_REQUIRED");
            return;
        }

        String text = message.substring(7);

        saveMessage(username, "PUBLIC", text, true);

        for (ClientHandler client : ChatServer.onlineUsers.values()) {
            client.sendEncrypted("PUBLIC:" + username + ":" + text);
        }
    }

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
                saveMessage(username, targetUser, text, true);
                sendEncrypted("PRIVATE_SENT:" + targetUser + ":" + text);
            } else {
                saveMessage(username, targetUser, text, false);
                sendEncrypted("PRIVATE_QUEUED:" + targetUser + ":" + text);
            }

        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            sendEncrypted("PRIVATE_ERROR");
        }
    }

    private void handleCreateGroup(String message) {
        if (username == null) {
            sendEncrypted("LOGIN_REQUIRED");
            return;
        }

        try {
            String[] parts = message.split(":", 3);

            if (parts.length < 2) {
                sendEncrypted("GROUP_CREATE_INVALID");
                return;
            }

            String groupName = parts[1].trim();
            String memberList = parts.length >= 3 ? parts[2].trim() : "";

            if (groupName.isEmpty()) {
                sendEncrypted("GROUP_CREATE_INVALID");
                return;
            }

            Connection conn = DBConnection.getConnection();

            PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT * FROM chat_groups WHERE group_name = ?"
            );
            checkStmt.setString(1, groupName);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                sendEncrypted("GROUP_CREATE_FAILED_EXISTS");
                conn.close();
                return;
            }

            PreparedStatement insertGroup = conn.prepareStatement(
                    "INSERT INTO chat_groups (group_name, created_by) VALUES (?, ?)"
            );
            insertGroup.setString(1, groupName);
            insertGroup.setString(2, username);
            insertGroup.executeUpdate();

            List<String> members = new ArrayList<>();
            members.add(username);

            if (!memberList.isEmpty()) {
                String[] rawMembers = memberList.split(",");
                for (String m : rawMembers) {
                    String trimmed = m.trim();
                    if (!trimmed.isEmpty() && !members.contains(trimmed)) {
                        members.add(trimmed);
                    }
                }
            }

            PreparedStatement insertMember = conn.prepareStatement(
                    "INSERT INTO group_members (group_name, username) VALUES (?, ?)"
            );

            for (String member : members) {
                insertMember.setString(1, groupName);
                insertMember.setString(2, member);
                insertMember.executeUpdate();
            }

            conn.close();
            sendEncrypted("GROUP_CREATE_SUCCESS:" + groupName);

        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            sendEncrypted("GROUP_CREATE_ERROR");
        }
    }

    private void handleListGroups() {
        if (username == null) {
            sendEncrypted("LOGIN_REQUIRED");
            return;
        }

        try {
            Connection conn = DBConnection.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT group_name FROM group_members WHERE username = ? ORDER BY group_name"
            );
            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();
            List<String> groups = new ArrayList<>();

            while (rs.next()) {
                groups.add(rs.getString("group_name"));
            }

            conn.close();
            sendEncrypted("GROUP_LIST:" + String.join(",", groups));

        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            sendEncrypted("GROUP_LIST_ERROR");
        }
    }

    private void handleLeaveGroup(String message) {
        if (username == null) {
            sendEncrypted("LOGIN_REQUIRED");
            return;
        }

        try {
            String[] parts = message.split(":", 2);
            if (parts.length < 2) {
                sendEncrypted("GROUP_LEAVE_ERROR");
                return;
            }

            String groupName = parts[1].trim();
            Connection conn = DBConnection.getConnection();

            PreparedStatement ownerStmt = conn.prepareStatement(
                    "SELECT created_by FROM chat_groups WHERE group_name = ?"
            );
            ownerStmt.setString(1, groupName);
            ResultSet ownerRs = ownerStmt.executeQuery();

            if (!ownerRs.next()) {
                sendEncrypted("GROUP_LEAVE_ERROR");
                conn.close();
                return;
            }

            String createdBy = ownerRs.getString("created_by");
            if (username.equals(createdBy)) {
                sendEncrypted("GROUP_LEAVE_OWNER_BLOCKED");
                conn.close();
                return;
            }

            PreparedStatement leaveStmt = conn.prepareStatement(
                    "DELETE FROM group_members WHERE group_name = ? AND username = ?"
            );
            leaveStmt.setString(1, groupName);
            leaveStmt.setString(2, username);
            leaveStmt.executeUpdate();

            conn.close();
            sendEncrypted("GROUP_LEFT:" + groupName);

        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            sendEncrypted("GROUP_LEAVE_ERROR");
        }
    }

    private void handleDeleteGroup(String message) {
        if (username == null) {
            sendEncrypted("LOGIN_REQUIRED");
            return;
        }

        try {
            String[] parts = message.split(":", 2);
            if (parts.length < 2) {
                sendEncrypted("GROUP_DELETE_ERROR");
                return;
            }

            String groupName = parts[1].trim();

            Connection conn = DBConnection.getConnection();

            PreparedStatement ownerStmt = conn.prepareStatement(
                    "SELECT created_by FROM chat_groups WHERE group_name = ?"
            );
            ownerStmt.setString(1, groupName);
            ResultSet rs = ownerStmt.executeQuery();

            if (!rs.next()) {
                sendEncrypted("GROUP_DELETE_ERROR");
                conn.close();
                return;
            }

            String createdBy = rs.getString("created_by");
            if (!username.equals(createdBy)) {
                sendEncrypted("GROUP_DELETE_DENIED");
                conn.close();
                return;
            }

            PreparedStatement deleteMembers = conn.prepareStatement(
                    "DELETE FROM group_members WHERE group_name = ?"
            );
            deleteMembers.setString(1, groupName);
            deleteMembers.executeUpdate();

            PreparedStatement deleteMessages = conn.prepareStatement(
                    "DELETE FROM messages WHERE receiver = ?"
            );
            deleteMessages.setString(1, "GROUP:" + groupName);
            deleteMessages.executeUpdate();

            PreparedStatement deleteGroup = conn.prepareStatement(
                    "DELETE FROM chat_groups WHERE group_name = ?"
            );
            deleteGroup.setString(1, groupName);
            deleteGroup.executeUpdate();

            conn.close();
            sendEncrypted("GROUP_DELETED:" + groupName);

        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            sendEncrypted("GROUP_DELETE_ERROR");
        }
    }

    private void handleGroupMessage(String message) {
        if (username == null) {
            sendEncrypted("LOGIN_REQUIRED");
            return;
        }

        try {
            String[] parts = message.split(":", 3);

            if (parts.length < 3) {
                sendEncrypted("INVALID_GROUP_FORMAT");
                return;
            }

            String groupName = parts[1];
            String text = parts[2];

            Connection conn = DBConnection.getConnection();

            PreparedStatement membershipCheck = conn.prepareStatement(
                    "SELECT * FROM group_members WHERE group_name = ? AND username = ?"
            );
            membershipCheck.setString(1, groupName);
            membershipCheck.setString(2, username);

            ResultSet membershipRs = membershipCheck.executeQuery();
            if (!membershipRs.next()) {
                sendEncrypted("GROUP_ACCESS_DENIED");
                conn.close();
                return;
            }

            saveMessage(username, "GROUP:" + groupName, text, true);

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT username FROM group_members WHERE group_name = ?"
            );
            stmt.setString(1, groupName);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String member = rs.getString("username");
                ClientHandler target = ChatServer.onlineUsers.get(member);

                if (target != null) {
                    target.sendEncrypted("GROUP:" + groupName + ":" + username + ":" + text);
                }
            }

            conn.close();

        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            sendEncrypted("GROUP_ERROR");
        }
    }

    private void sendPublicHistory() {
        if (username == null) {
            sendEncrypted("LOGIN_REQUIRED");
            return;
        }

        try {
            Connection conn = DBConnection.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT sender, content FROM messages WHERE receiver = 'PUBLIC' ORDER BY id ASC"
            );

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String sender = rs.getString("sender");
                String content = rs.getString("content");
                sendEncrypted("PUBLIC:" + sender + ":" + content);
            }

            conn.close();

        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            sendEncrypted("HISTORY_ERROR");
        }
    }

    private void sendPrivateHistory(String message) {
        if (username == null) {
            sendEncrypted("LOGIN_REQUIRED");
            return;
        }

        try {
            String[] parts = message.split(":", 3);
            if (parts.length < 3) {
                sendEncrypted("HISTORY_ERROR");
                return;
            }

            String otherUser = parts[2];

            Connection conn = DBConnection.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT sender, receiver, content FROM messages " +
                            "WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?) " +
                            "ORDER BY id ASC"
            );

            stmt.setString(1, username);
            stmt.setString(2, otherUser);
            stmt.setString(3, otherUser);
            stmt.setString(4, username);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String sender = rs.getString("sender");
                String content = rs.getString("content");
                sendEncrypted("PRIVATE:" + sender + ":" + content);
            }

            conn.close();

        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            sendEncrypted("HISTORY_ERROR");
        }
    }

    private void sendGroupHistory(String message) {
        if (username == null) {
            sendEncrypted("LOGIN_REQUIRED");
            return;
        }

        try {
            String[] parts = message.split(":", 3);
            if (parts.length < 3) {
                sendEncrypted("HISTORY_ERROR");
                return;
            }

            String groupName = parts[2];

            Connection conn = DBConnection.getConnection();

            PreparedStatement membershipCheck = conn.prepareStatement(
                    "SELECT * FROM group_members WHERE group_name = ? AND username = ?"
            );
            membershipCheck.setString(1, groupName);
            membershipCheck.setString(2, username);

            ResultSet membershipRs = membershipCheck.executeQuery();
            if (!membershipRs.next()) {
                sendEncrypted("GROUP_ACCESS_DENIED");
                conn.close();
                return;
            }

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT sender, content FROM messages WHERE receiver = ? ORDER BY id ASC"
            );
            stmt.setString(1, "GROUP:" + groupName);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String sender = rs.getString("sender");
                String content = rs.getString("content");
                sendEncrypted("GROUP:" + groupName + ":" + sender + ":" + content);
            }

            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
            sendEncrypted("HISTORY_ERROR");
        }
    }

    private void saveMessage(String sender, String receiver, String content, boolean delivered) {
        try {
            Connection conn = DBConnection.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO messages (sender, receiver, content, delivered) VALUES (?, ?, ?, ?)"
            );
            stmt.setString(1, sender);
            stmt.setString(2, receiver);
            stmt.setString(3, content);
            stmt.setBoolean(4, delivered);

            stmt.executeUpdate();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendPendingPrivateMessages(String user) {
        try {
            Connection conn = DBConnection.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, sender, content FROM messages WHERE receiver = ? AND delivered = FALSE ORDER BY id ASC"
            );
            stmt.setString(1, user);

            ResultSet rs = stmt.executeQuery();
            List<Integer> deliveredIds = new ArrayList<>();

            while (rs.next()) {
                int id = rs.getInt("id");
                String sender = rs.getString("sender");
                String content = rs.getString("content");

                sendEncrypted("PRIVATE:" + sender + ":" + content);
                deliveredIds.add(id);
            }

            for (Integer id : deliveredIds) {
                PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE messages SET delivered = TRUE WHERE id = ?"
                );
                updateStmt.setInt(1, id);
                updateStmt.executeUpdate();
            }

            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendEncrypted(String message) {
        try {
            String encrypted = EncryptionUtil.encrypt(message);
            writer.println(encrypted);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}