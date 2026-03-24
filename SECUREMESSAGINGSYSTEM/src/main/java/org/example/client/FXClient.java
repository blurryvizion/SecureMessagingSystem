package org.example.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.util.*;
import java.util.stream.Collectors;

public class FXClient extends Application {

    private Stage primaryStage;
    private ClientConnection connection;

    private VBox messagesBox;
    private VBox onlineUsersBox;

    private String currentUser = "";
    private Label authStatusLabel;

    private final Set<String> knownUsers = new HashSet<>();
    private final Set<String> onlineUsers = new HashSet<>();
    private final Set<String> knownGroups = new HashSet<>();

    private String currentChannel = "general";
    private String selectedPrivateUser = null;
    private String selectedGroup = null;

    private Label channelTitleLabel;
    private Label channelSubtitleLabel;
    private TextField messageInput;

    private final List<MessageItem> generalMessages = new ArrayList<>();
    private final Map<String, List<MessageItem>> privateMessages = new HashMap<>();
    private final Map<String, List<MessageItem>> groupMessages = new HashMap<>();

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Secure Chat");

        connection = new ClientConnection(this);
        showAuthView();

        stage.show();
    }

    private void showAuthView() {
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: #313338;");

        Label title = new Label("Secure Chat");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");

        Label subtitle = new Label("Login or create an account to continue");
        subtitle.setStyle("-fx-text-fill: #b5bac1; -fx-font-size: 14px;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(300);
        styleInput(usernameField);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(300);
        styleInput(passwordField);

        authStatusLabel = new Label("");
        authStatusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 13px;");

        Button loginBtn = new Button("Login");
        loginBtn.setPrefWidth(300);
        loginBtn.setStyle("-fx-background-color: #5865f2; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10;");

        Button signupBtn = new Button("Sign Up");
        signupBtn.setPrefWidth(300);
        signupBtn.setStyle("-fx-background-color: #3ba55d; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10;");

        loginBtn.setOnAction(e -> {
            String user = usernameField.getText().trim();
            String pass = passwordField.getText().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                authStatusLabel.setText("Enter username and password.");
                return;
            }

            currentUser = user;
            knownUsers.add(user);
            connection.send("LOGIN:" + user + ":" + pass);
        });

        signupBtn.setOnAction(e -> {
            String user = usernameField.getText().trim();
            String pass = passwordField.getText().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                authStatusLabel.setText("Enter username and password.");
                return;
            }

            currentUser = user;
            knownUsers.add(user);
            connection.send("SIGNUP:" + user + ":" + pass);
        });

        VBox card = new VBox(12, title, subtitle, usernameField, passwordField, loginBtn, signupBtn, authStatusLabel);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(30));
        card.setMaxWidth(380);
        card.setStyle("-fx-background-color: #2b2d31; -fx-background-radius: 16;");

        root.getChildren().add(card);

        Scene scene = new Scene(root, 600, 500);
        primaryStage.setScene(scene);
    }

    public void showChatView() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #313338;");

        VBox leftSidebar = createLeftSidebar();
        VBox centerArea = createCenterArea();

        root.setLeft(leftSidebar);
        root.setCenter(centerArea);

        Scene scene = new Scene(root, 1200, 740);
        primaryStage.setScene(scene);

        renderOnlineUsers();
        switchToGeneralChannel();
    }

    private VBox createLeftSidebar() {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(280);
        sidebar.setStyle("-fx-background-color: #2b2d31;");
        sidebar.setPadding(new Insets(15));
        sidebar.setSpacing(15);

        Label serverTitle = new Label("Secure Chat");
        serverTitle.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        Label channelsTitle = new Label("TEXT CHANNELS");
        channelsTitle.setStyle("-fx-text-fill: #949ba4; -fx-font-size: 11px; -fx-font-weight: bold;");

        Button generalBtn = createSidebarButton("# general");
        Button groupsBtn = createSidebarButton("# groups");

        generalBtn.setOnAction(e -> switchToGeneralChannel());
        groupsBtn.setOnAction(e -> switchToGroupsRoot());

        Label accountTitle = new Label("LOGGED IN AS");
        accountTitle.setStyle("-fx-text-fill: #949ba4; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label userLabel = new Label(currentUser);
        userLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        Button refreshBtn = new Button("Refresh Online Users");
        refreshBtn.setMaxWidth(Double.MAX_VALUE);
        refreshBtn.setStyle("-fx-background-color: #5865f2; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10;");
        refreshBtn.setOnAction(e -> connection.send("ONLINE_USERS"));

        Button createGroupBtn = new Button("Create Group");
        createGroupBtn.setMaxWidth(Double.MAX_VALUE);
        createGroupBtn.setStyle("-fx-background-color: #3ba55d; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10;");
        createGroupBtn.setOnAction(e -> openCreateGroupDialog());

        Button refreshGroupsBtn = new Button("Refresh Groups");
        refreshGroupsBtn.setMaxWidth(Double.MAX_VALUE);
        refreshGroupsBtn.setStyle("-fx-background-color: #4f545c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10;");
        refreshGroupsBtn.setOnAction(e -> connection.send("LIST_GROUPS"));

        Button signOutBtn = new Button("Sign Out");
        signOutBtn.setMaxWidth(Double.MAX_VALUE);
        signOutBtn.setStyle("-fx-background-color: #ed4245; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8;");
        signOutBtn.setOnAction(e -> connection.send("SIGNOUT"));

        Label usersTitle = new Label("USERS");
        usersTitle.setStyle("-fx-text-fill: #949ba4; -fx-font-size: 11px; -fx-font-weight: bold;");

        onlineUsersBox = new VBox(6);
        onlineUsersBox.setPadding(new Insets(4, 0, 0, 0));

        ScrollPane usersScroll = new ScrollPane(onlineUsersBox);
        usersScroll.setFitToWidth(true);
        usersScroll.setPrefHeight(280);
        usersScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        sidebar.getChildren().addAll(
                serverTitle,
                channelsTitle,
                generalBtn,
                groupsBtn,
                new Separator(),
                accountTitle,
                userLabel,
                refreshBtn,
                createGroupBtn,
                refreshGroupsBtn,
                signOutBtn,
                usersTitle,
                usersScroll
        );

        return sidebar;
    }

    private VBox createCenterArea() {
        VBox center = new VBox();
        center.setStyle("-fx-background-color: #313338;");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15));
        header.setSpacing(10);
        header.setStyle("-fx-background-color: #313338; -fx-border-color: transparent transparent #1e1f22 transparent;");

        channelTitleLabel = new Label("# general");
        channelTitleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        channelSubtitleLabel = new Label("Secure Messaging System");
        channelSubtitleLabel.setStyle("-fx-text-fill: #949ba4; -fx-font-size: 12px;");

        VBox headerText = new VBox(channelTitleLabel, channelSubtitleLabel);
        headerText.setSpacing(2);
        header.getChildren().add(headerText);

        messagesBox = new VBox();
        messagesBox.setSpacing(10);
        messagesBox.setPadding(new Insets(15));
        messagesBox.setStyle("-fx-background-color: #313338;");

        ScrollPane scrollPane = new ScrollPane(messagesBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #313338; -fx-background-color: #313338;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        HBox composer = new HBox();
        composer.setPadding(new Insets(12));
        composer.setSpacing(10);
        composer.setStyle("-fx-background-color: #313338;");

        messageInput = new TextField();
        messageInput.setPromptText("Message #general");
        messageInput.setStyle("-fx-background-color: #383a40; -fx-text-fill: white; -fx-prompt-text-fill: #949ba4; -fx-background-radius: 10; -fx-padding: 12;");
        HBox.setHgrow(messageInput, Priority.ALWAYS);

        Button sendButton = new Button("Send");
        sendButton.setStyle("-fx-background-color: #5865f2; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 16 10 16;");

        sendButton.setOnAction(e -> sendCurrentMessage());
        messageInput.setOnAction(e -> sendCurrentMessage());

        composer.getChildren().addAll(messageInput, sendButton);
        center.getChildren().addAll(header, scrollPane, composer);

        return center;
    }

    private void sendCurrentMessage() {
        if (messageInput == null) return;

        String text = messageInput.getText().trim();
        if (text.isEmpty()) return;

        if ("general".equals(currentChannel)) {
            connection.send("PUBLIC:" + text);
        } else if ("private".equals(currentChannel)) {
            if (selectedPrivateUser == null || selectedPrivateUser.isEmpty()) {
                addSystemMessageToCurrentView("Select a user first for private chat.");
                return;
            }
            if (selectedPrivateUser.equals(currentUser)) {
                addSystemMessageToCurrentView("You cannot private message yourself.");
                return;
            }
            connection.send("PRIVATE:" + selectedPrivateUser + ":" + text);
        } else if ("groups".equals(currentChannel)) {
            if (selectedGroup == null || selectedGroup.isEmpty()) {
                addSystemMessageToCurrentView("Select a group first.");
                return;
            }
            connection.send("GROUP:" + selectedGroup + ":" + text);
        }

        messageInput.clear();
    }

    private void switchToGeneralChannel() {
        currentChannel = "general";
        selectedPrivateUser = null;
        selectedGroup = null;
        updateChannelHeader("# general", "Secure Messaging System");
        messageInput.setPromptText("Message #general");
        renderOnlineUsers();

        generalMessages.clear();
        renderCurrentConversation();
        connection.send("HISTORY:PUBLIC");
    }

    private void switchToPrivateConversation(String user) {
        currentChannel = "private";
        selectedPrivateUser = user;
        selectedGroup = null;
        updateChannelHeader("@ " + user, "Direct message conversation");
        messageInput.setPromptText("Message @" + user);
        renderOnlineUsers();

        privateMessages.put(user, new ArrayList<>());
        renderCurrentConversation();
        connection.send("HISTORY:PRIVATE:" + user);
    }

    private void switchToGroupsRoot() {
        currentChannel = "groups";
        selectedPrivateUser = null;
        selectedGroup = null;
        updateChannelHeader("# groups", "Select a group to open its chat.");
        messageInput.setPromptText("Select a group first");
        renderOnlineUsers();
        renderGroupSelectionView();
    }

    private void switchToGroupConversation(String groupName) {
        currentChannel = "groups";
        selectedPrivateUser = null;
        selectedGroup = groupName;
        updateChannelHeader("# " + groupName, "Group conversation");
        messageInput.setPromptText("Message #" + groupName);
        renderOnlineUsers();

        groupMessages.put(groupName, new ArrayList<>());
        renderCurrentConversation();
        connection.send("HISTORY:GROUP:" + groupName);
    }

    private void openCreateGroupDialog() {
        TextInputDialog groupDialog = new TextInputDialog();
        groupDialog.setTitle("Create Group");
        groupDialog.setHeaderText("Create a new group");
        groupDialog.setContentText("Group name:");
        Optional<String> groupResult = groupDialog.showAndWait();

        if (!groupResult.isPresent()) return;

        String groupName = groupResult.get().trim();
        if (groupName.isEmpty()) return;

        TextInputDialog membersDialog = new TextInputDialog();
        membersDialog.setTitle("Add Members");
        membersDialog.setHeaderText("Enter usernames separated by commas");
        membersDialog.setContentText("Members:");
        Optional<String> membersResult = membersDialog.showAndWait();

        String members = membersResult.orElse("").trim();
        connection.send("CREATE_GROUP:" + groupName + ":" + members);
    }

    private void openGroupOptionsDialog() {
        if (selectedGroup == null || selectedGroup.isEmpty()) {
            addSystemMessageToCurrentView("Select a group first.");
            return;
        }

        List<String> choices = Arrays.asList("Leave Group", "Delete Group");
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Leave Group", choices);
        dialog.setTitle("Group Options");
        dialog.setHeaderText("Options for #" + selectedGroup);
        dialog.setContentText("Choose:");

        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent()) return;

        if ("Leave Group".equals(result.get())) {
            connection.send("LEAVE_GROUP:" + selectedGroup);
        } else if ("Delete Group".equals(result.get())) {
            connection.send("DELETE_GROUP:" + selectedGroup);
        }
    }

    private void updateChannelHeader(String title, String subtitle) {
        channelTitleLabel.setText(title);
        channelSubtitleLabel.setText(subtitle);
    }

    private Button createSidebarButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setStyle("-fx-background-color: transparent; -fx-text-fill: #dbdee1; -fx-font-size: 14px; -fx-background-radius: 8; -fx-padding: 8 10 8 10;");

        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #35373c; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-padding: 8 10 8 10;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: transparent; -fx-text-fill: #dbdee1; -fx-font-size: 14px; -fx-background-radius: 8; -fx-padding: 8 10 8 10;"));

        return button;
    }

    private void styleInput(TextField field) {
        field.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: white; -fx-prompt-text-fill: #949ba4; -fx-background-radius: 8; -fx-padding: 10;");
    }

    public void handleServerMessage(String message) {
        Platform.runLater(() -> {
            if (message == null) return;

            switch (message) {
                case "LOGIN_SUCCESS":
                    if (authStatusLabel != null) authStatusLabel.setText("");
                    showChatView();
                    connection.send("ONLINE_USERS");
                    connection.send("LIST_GROUPS");
                    return;

                case "LOGIN_FAILED":
                    if (authStatusLabel != null) {
                        authStatusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 13px;");
                        authStatusLabel.setText("Login failed. Check username/password.");
                    }
                    return;

                case "LOGIN_ERROR":
                    if (authStatusLabel != null) {
                        authStatusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 13px;");
                        authStatusLabel.setText("Login error.");
                    }
                    return;

                case "SIGNUP_SUCCESS":
                    if (authStatusLabel != null) {
                        authStatusLabel.setStyle("-fx-text-fill: #3ba55d; -fx-font-size: 13px;");
                        authStatusLabel.setText("Signup successful. Now log in.");
                    }
                    return;

                case "SIGNUP_FAILED_USERNAME_EXISTS":
                    if (authStatusLabel != null) {
                        authStatusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 13px;");
                        authStatusLabel.setText("Username already exists.");
                    }
                    return;

                case "SIGNUP_ERROR":
                    if (authStatusLabel != null) {
                        authStatusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 13px;");
                        authStatusLabel.setText("Signup error.");
                    }
                    return;

                case "GROUP_CREATE_FAILED_EXISTS":
                    addSystemMessageToCurrentView("Group name already exists.");
                    return;

                case "GROUP_CREATE_INVALID":
                    addSystemMessageToCurrentView("Invalid group creation request.");
                    return;

                case "GROUP_CREATE_ERROR":
                    addSystemMessageToCurrentView("Group creation error.");
                    return;

                case "GROUP_LIST_ERROR":
                    addSystemMessageToCurrentView("Could not load groups.");
                    return;

                case "GROUP_ACCESS_DENIED":
                    addSystemMessageToCurrentView("You are not a member of that group.");
                    return;

                case "GROUP_ERROR":
                    addSystemMessageToCurrentView("Group message error.");
                    return;

                case "GROUP_LEAVE_OWNER_BLOCKED":
                    addSystemMessageToCurrentView("Group creator cannot leave. Delete the group instead.");
                    return;

                case "GROUP_DELETE_DENIED":
                    addSystemMessageToCurrentView("Only the group creator can delete this group.");
                    return;

                case "GROUP_LEAVE_ERROR":
                    addSystemMessageToCurrentView("Could not leave group.");
                    return;

                case "GROUP_DELETE_ERROR":
                    addSystemMessageToCurrentView("Could not delete group.");
                    return;

                case "HISTORY_ERROR":
                    addSystemMessageToCurrentView("Could not load message history.");
                    return;

                case "SIGNOUT_SUCCESS":
                    currentChannel = "general";
                    selectedPrivateUser = null;
                    selectedGroup = null;

                    onlineUsers.clear();
                    knownUsers.clear();
                    knownGroups.clear();

                    generalMessages.clear();
                    privateMessages.clear();
                    groupMessages.clear();

                    connection = new ClientConnection(this);
                    showAuthView();
                    return;
            }

            if (message.startsWith("GROUP_CREATE_SUCCESS:")) {
                String groupName = message.substring("GROUP_CREATE_SUCCESS:".length()).trim();
                if (!groupName.isEmpty()) {
                    knownGroups.add(groupName);
                    switchToGroupConversation(groupName);
                    connection.send("LIST_GROUPS");
                }
                return;
            }

            if (message.startsWith("GROUP_LIST:")) {
                updateGroups(message.substring("GROUP_LIST:".length()));
                return;
            }

            if (message.startsWith("GROUP_LEFT:")) {
                String groupName = message.substring("GROUP_LEFT:".length()).trim();
                knownGroups.remove(groupName);
                groupMessages.remove(groupName);

                if (groupName.equals(selectedGroup)) {
                    selectedGroup = null;
                    switchToGroupsRoot();
                }

                connection.send("LIST_GROUPS");
                return;
            }

            if (message.startsWith("GROUP_DELETED:")) {
                String groupName = message.substring("GROUP_DELETED:".length()).trim();
                knownGroups.remove(groupName);
                groupMessages.remove(groupName);

                if (groupName.equals(selectedGroup)) {
                    selectedGroup = null;
                    switchToGroupsRoot();
                }

                connection.send("LIST_GROUPS");
                return;
            }

            if (message.startsWith("PRIVATE_SENT:")) {
                String[] parts = message.split(":", 3);
                if (parts.length == 3) {
                    String target = parts[1];
                    String content = parts[2];
                    privateMessages.computeIfAbsent(target, k -> new ArrayList<>())
                            .add(MessageItem.privateOutgoing(target, content));
                    knownUsers.add(target);

                    if ("private".equals(currentChannel) && target.equals(selectedPrivateUser)) {
                        renderCurrentConversation();
                    }
                }
                return;
            }

            if (message.startsWith("PRIVATE_QUEUED:")) {
                String[] parts = message.split(":", 3);
                if (parts.length == 3) {
                    String target = parts[1];
                    String content = parts[2];
                    privateMessages.computeIfAbsent(target, k -> new ArrayList<>())
                            .add(MessageItem.privateQueued(target, content));
                    knownUsers.add(target);

                    if ("private".equals(currentChannel) && target.equals(selectedPrivateUser)) {
                        renderCurrentConversation();
                    }
                }
                return;
            }

            if ("PRIVATE_ERROR".equals(message)) {
                addSystemMessageToCurrentView("Private message error.");
                return;
            }

            if (isOnlineUsersResponse(message)) {
                updateOnlineUsers(message);
                return;
            }

            routeIncomingMessage(message);
        });
    }

    private void routeIncomingMessage(String text) {
        if (text.startsWith("PUBLIC:")) {
            String[] parts = text.split(":", 3);
            if (parts.length == 3) {
                String sender = parts[1];
                String content = parts[2];
                knownUsers.add(sender);
                generalMessages.add(MessageItem.publicMessage(sender, content));

                if ("general".equals(currentChannel)) {
                    renderCurrentConversation();
                }
            }
            return;
        }

        if (text.startsWith("PRIVATE:")) {
            String[] parts = text.split(":", 3);
            if (parts.length == 3) {
                String sender = parts[1];
                String content = parts[2];
                knownUsers.add(sender);
                privateMessages.computeIfAbsent(sender, k -> new ArrayList<>())
                        .add(MessageItem.privateIncoming(sender, content));

                if ("private".equals(currentChannel) && sender.equals(selectedPrivateUser)) {
                    renderCurrentConversation();
                }
            }
            return;
        }

        if (text.startsWith("GROUP:")) {
            String[] parts = text.split(":", 4);
            if (parts.length == 4) {
                String groupName = parts[1];
                String sender = parts[2];
                String content = parts[3];

                knownGroups.add(groupName);
                groupMessages.computeIfAbsent(groupName, k -> new ArrayList<>())
                        .add(MessageItem.groupMessage(groupName, sender, content));

                if ("groups".equals(currentChannel) && groupName.equals(selectedGroup)) {
                    renderCurrentConversation();
                }
            }
            return;
        }

        addSystemMessageToCurrentView(text);
    }

    private boolean isOnlineUsersResponse(String message) {
        if (message == null || message.trim().isEmpty()) return false;

        if (message.contains("PUBLIC:") || message.contains("PRIVATE:") || message.contains("GROUP:")
                || message.contains("LOGIN") || message.contains("SIGNUP") || message.contains("SIGNOUT")
                || message.startsWith("GROUP_LIST:") || message.startsWith("GROUP_CREATE_")
                || message.startsWith("GROUP_LEFT:") || message.startsWith("GROUP_DELETED:")) {
            return false;
        }

        String[] parts = message.split(",");
        for (String part : parts) {
            String user = part.trim();
            if (user.isEmpty()) return false;
            if (!user.matches("[a-zA-Z0-9_]+")) return false;
        }
        return true;
    }

    private void updateOnlineUsers(String message) {
        onlineUsers.clear();

        if (message != null && !message.trim().isEmpty()) {
            String[] users = message.split(",");
            for (String user : users) {
                String trimmed = user.trim();
                if (!trimmed.isEmpty()) {
                    onlineUsers.add(trimmed);
                    knownUsers.add(trimmed);
                }
            }
        }

        if (!currentUser.isEmpty()) {
            knownUsers.add(currentUser);
        }

        renderOnlineUsers();
    }

    private void updateGroups(String message) {
        knownGroups.clear();

        if (message != null && !message.trim().isEmpty()) {
            String[] groups = message.split(",");
            for (String group : groups) {
                String trimmed = group.trim();
                if (!trimmed.isEmpty()) {
                    knownGroups.add(trimmed);
                }
            }
        }

        if ("groups".equals(currentChannel) && selectedGroup == null) {
            renderGroupSelectionView();
        }
    }

    private void renderOnlineUsers() {
        if (onlineUsersBox == null) return;

        onlineUsersBox.getChildren().clear();

        List<String> sortedUsers = knownUsers.stream().sorted().collect(Collectors.toList());

        for (String user : sortedUsers) {
            boolean isOnline = onlineUsers.contains(user);

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 8, 6, 8));
            row.setStyle(
                    "-fx-background-color: " +
                            ((selectedPrivateUser != null && selectedPrivateUser.equals(user) && "private".equals(currentChannel))
                                    ? "#35373c" : "transparent") +
                            ";" +
                            "-fx-background-radius: 8;"
            );

            Circle statusDot = new Circle(5);
            statusDot.setFill(isOnline ? Color.LIMEGREEN : Color.web("#5c5f66"));

            Label nameLabel = new Label(user);
            nameLabel.setStyle(
                    "-fx-text-fill: " + (isOnline ? "white" : "#9aa0a6") + ";" +
                            "-fx-font-size: 13px;" +
                            (user.equals(currentUser) ? "-fx-font-weight: bold;" : "")
            );

            row.getChildren().addAll(statusDot, nameLabel);

            row.setOnMouseClicked(e -> {
                if (!user.equals(currentUser)) {
                    switchToPrivateConversation(user);
                }
            });

            onlineUsersBox.getChildren().add(row);
        }
    }

    private void renderGroupSelectionView() {
        clearMessageView();

        VBox container = new VBox(12);
        container.setPadding(new Insets(10));

        Label title = new Label("Your Groups");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label subtitle = new Label("Select a group to open its chat.");
        subtitle.setStyle("-fx-text-fill: #b5bac1; -fx-font-size: 13px;");

        Button optionsBtn = new Button("Group Options");
        optionsBtn.setStyle("-fx-background-color: #4f545c; -fx-text-fill: white; -fx-background-radius: 8;");
        optionsBtn.setOnAction(e -> openGroupOptionsDialog());

        container.getChildren().addAll(title, subtitle, optionsBtn);

        List<String> sortedGroups = knownGroups.stream().sorted().collect(Collectors.toList());

        if (sortedGroups.isEmpty()) {
            Label empty = new Label("You are not in any groups yet.");
            empty.setStyle("-fx-text-fill: #9aa0a6; -fx-font-size: 14px;");
            container.getChildren().add(empty);
        } else {
            for (String group : sortedGroups) {
                Button groupButton = new Button("# " + group);
                groupButton.setMaxWidth(Double.MAX_VALUE);
                groupButton.setAlignment(Pos.CENTER_LEFT);
                groupButton.setStyle(
                        "-fx-background-color: #383a40;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 14px;" +
                                "-fx-background-radius: 10;" +
                                "-fx-padding: 12;"
                );
                groupButton.setOnAction(e -> switchToGroupConversation(group));
                container.getChildren().add(groupButton);
            }
        }

        messagesBox.getChildren().add(container);
    }

    private void renderCurrentConversation() {
        clearMessageView();

        if ("general".equals(currentChannel)) {
            for (MessageItem item : generalMessages) {
                renderMessageItem(item);
            }
            return;
        }

        if ("private".equals(currentChannel)) {
            if (selectedPrivateUser == null) return;

            List<MessageItem> conversation = privateMessages.getOrDefault(selectedPrivateUser, new ArrayList<>());
            for (MessageItem item : conversation) {
                renderMessageItem(item);
            }
            return;
        }

        if ("groups".equals(currentChannel)) {
            if (selectedGroup == null) {
                renderGroupSelectionView();
                return;
            }

            VBox headerButtons = new VBox(10);
            headerButtons.setPadding(new Insets(0, 0, 10, 0));

            Button optionsBtn = new Button("Leave / Delete Group");
            optionsBtn.setStyle("-fx-background-color: #4f545c; -fx-text-fill: white; -fx-background-radius: 8;");
            optionsBtn.setOnAction(e -> openGroupOptionsDialog());

            headerButtons.getChildren().add(optionsBtn);
            messagesBox.getChildren().add(headerButtons);

            List<MessageItem> conversation = groupMessages.getOrDefault(selectedGroup, new ArrayList<>());
            for (MessageItem item : conversation) {
                renderMessageItem(item);
            }
        }
    }

    private void clearMessageView() {
        if (messagesBox != null) {
            messagesBox.getChildren().clear();
        }
    }

    private void addSystemMessageToCurrentView(String text) {
        MessageItem item = MessageItem.system(text);

        if ("general".equals(currentChannel)) {
            generalMessages.add(item);
        } else if ("private".equals(currentChannel) && selectedPrivateUser != null) {
            privateMessages.computeIfAbsent(selectedPrivateUser, k -> new ArrayList<>()).add(item);
        } else if ("groups".equals(currentChannel) && selectedGroup != null) {
            groupMessages.computeIfAbsent(selectedGroup, k -> new ArrayList<>()).add(item);
        }

        renderCurrentConversation();
    }

    private void renderMessageItem(MessageItem item) {
        HBox wrapper = new HBox();
        wrapper.setPadding(new Insets(2, 0, 2, 0));

        VBox bubble = new VBox(4);
        bubble.setMaxWidth(700);
        bubble.setStyle("-fx-background-color: #383a40; -fx-background-radius: 12; -fx-padding: 10 14 10 14;");

        if ("public".equals(item.type)) {
            Label userLabel = new Label(item.title);
            userLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 14px;");

            Label msgLabel = new Label(item.content);
            msgLabel.setWrapText(true);
            msgLabel.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 14px;");

            bubble.getChildren().addAll(userLabel, msgLabel);
        } else if ("privateIncoming".equals(item.type)) {
            Label title = new Label("Private from " + item.title);
            title.setStyle("-fx-text-fill: #ffcc66; -fx-font-weight: bold; -fx-font-size: 14px;");

            Label msgLabel = new Label(item.content);
            msgLabel.setWrapText(true);
            msgLabel.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 14px;");

            bubble.getChildren().addAll(title, msgLabel);
        } else if ("privateOutgoing".equals(item.type)) {
            Label title = new Label("Sent privately to " + item.title);
            title.setStyle("-fx-text-fill: #57f287; -fx-font-weight: bold; -fx-font-size: 14px;");

            Label msgLabel = new Label(item.content);
            msgLabel.setWrapText(true);
            msgLabel.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 14px;");

            bubble.getChildren().addAll(title, msgLabel);
        } else if ("privateQueued".equals(item.type)) {
            Label title = new Label("Queued for " + item.title + " (offline)");
            title.setStyle("-fx-text-fill: #faa61a; -fx-font-weight: bold; -fx-font-size: 14px;");

            Label msgLabel = new Label(item.content);
            msgLabel.setWrapText(true);
            msgLabel.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 14px;");

            bubble.getChildren().addAll(title, msgLabel);
        } else if ("group".equals(item.type)) {
            Label title = new Label(item.title);
            title.setStyle("-fx-text-fill: #66d9ef; -fx-font-weight: bold; -fx-font-size: 14px;");

            Label msgLabel = new Label(item.content);
            msgLabel.setWrapText(true);
            msgLabel.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 14px;");

            bubble.getChildren().addAll(title, msgLabel);
        } else {
            Label label = new Label(item.content);
            label.setWrapText(true);
            label.setStyle("-fx-text-fill: #b5bac1; -fx-font-size: 14px;");
            bubble.getChildren().add(label);
        }

        wrapper.getChildren().add(bubble);
        messagesBox.getChildren().add(wrapper);
    }

    public static void main(String[] args) {
        launch();
    }

    private static class MessageItem {
        String type;
        String title;
        String content;

        MessageItem(String type, String title, String content) {
            this.type = type;
            this.title = title;
            this.content = content;
        }

        static MessageItem publicMessage(String sender, String content) {
            return new MessageItem("public", sender, content);
        }

        static MessageItem privateIncoming(String sender, String content) {
            return new MessageItem("privateIncoming", sender, content);
        }

        static MessageItem privateOutgoing(String target, String content) {
            return new MessageItem("privateOutgoing", target, content);
        }

        static MessageItem privateQueued(String target, String content) {
            return new MessageItem("privateQueued", target, content);
        }

        static MessageItem groupMessage(String groupName, String sender, String content) {
            return new MessageItem("group", sender + " in #" + groupName, content);
        }

        static MessageItem system(String content) {
            return new MessageItem("system", "", content);
        }
    }
}