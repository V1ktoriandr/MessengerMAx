package com.icq.gui.client;

import com.icq.client.ClientConnection;
import com.icq.client.VoiceRecorder;
import com.icq.model.ChatMessage;
import com.icq.model.DialogItem;
import com.icq.model.GroupChat;
import com.icq.model.User;
import com.icq.protocol.Message;
import com.icq.util.AvatarUtil;
import com.icq.util.IconUtil;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class ChatController {
    @FXML private ImageView ownAvatarView;
    @FXML private Label ownNameLabel;
    @FXML private Label ownStatusLabel;
    @FXML private TextField searchField;
    @FXML private ListView<DialogItem> dialogsList;
    @FXML private ListView<GroupChat> groupsList;
    @FXML private ImageView chatAvatarView;
    @FXML private Label titleLabel;
    @FXML private Label chatStatusLabel;
    @FXML private Label typingLabel;
    @FXML private ListView<ChatMessage> messagesList;
    @FXML private TextField messageField;
    @FXML private Button attachButton;
    @FXML private Button emojiButton;
    @FXML private Button voiceButton;
    @FXML private Button sendButton;
    @FXML private Button profileEditButton;
    @FXML private Button newGroupButton;
    @FXML private Label recordingTimerLabel;
    @FXML private HBox recordingWavesBox;

    private final ObservableList<DialogItem> dialogs = FXCollections.observableArrayList();
    private final ObservableList<DialogItem> filteredDialogs = FXCollections.observableArrayList();
    private final ObservableList<GroupChat> groups = FXCollections.observableArrayList();
    private final ObservableList<GroupChat> filteredGroups = FXCollections.observableArrayList();
    private final ObservableList<ChatMessage> messages = FXCollections.observableArrayList();
    private final Map<String, DialogItem> dialogByUser = new HashMap<>();
    private final Map<String, String> avatars = new HashMap<>();
    private final Map<String, String> emails = new HashMap<>();
    private final PauseTransition typingHideDelay = new PauseTransition(Duration.seconds(1.5));
    private final VoiceRecorder voiceRecorder = new VoiceRecorder();
    private final ImageViewer imageViewer = new ImageViewer();
    private ClientConnection connection;
    private String selectedUser;
    private GroupChat selectedGroup;
    private Timeline recordingTimer;
    private int recordingSeconds;

    @FXML
    private void initialize() {
        dialogsList.setItems(filteredDialogs);
        dialogsList.setCellFactory(list -> new DialogCell());
        groupsList.setItems(filteredGroups);
        groupsList.setCellFactory(list -> new GroupCell());
        groupsList.setManaged(false);
        messagesList.setItems(messages);
        messagesList.setCellFactory(list -> new MessageCell());
        VBox.setVgrow(messagesList, Priority.ALWAYS);
        typingLabel.setVisible(false);
        typingHideDelay.setOnFinished(event -> typingLabel.setVisible(false));

        dialogsList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, item) -> selectDialog(item));
        groupsList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, item) -> selectGroup(item));
        searchField.textProperty().addListener((observable, oldValue, newValue) -> filterLists());
        installIcons();

        messageField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                sendMessage();
            } else if (event.isControlDown() && event.getCode() == KeyCode.V) {
                pasteImageFromClipboard();
            } else if (selectedUser != null) {
                connection.sendTyping(selectedUser);
            }
        });
    }

    public void setConnection(ClientConnection connection) {
        this.connection = connection;
        ownNameLabel.setText(connection.getUsername());
        ownStatusLabel.setText("Online");
        connection.setMessageConsumer(message -> Platform.runLater(() -> handleMessage(message)));
        connection.setErrorConsumer(error -> Platform.runLater(() -> chatStatusLabel.setText("Connection error: " + error)));
    }

    @FXML
    private void sendMessage() {
        String text = messageField.getText().trim();
        if ((selectedUser == null && selectedGroup == null) || text.isBlank()) {
            return;
        }
        if (selectedGroup != null) {
            connection.sendGroupChat(selectedGroup.getId(), text);
        } else {
            connection.sendChat(selectedUser, text);
        }
        messageField.clear();
    }

    @FXML
    private void attachFile() {
        if (selectedUser == null && selectedGroup == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Attach file");
        File file = chooser.showOpenDialog(messageField.getScene().getWindow());
        if (file != null) {
            sendFile(file.toPath());
        }
    }

    @FXML
    private void toggleVoiceRecording() {
        if (selectedUser == null) {
            return;
        }
        try {
            if (!voiceRecorder.isRecording()) {
                voiceRecorder.start();
                startRecordingUi();
                voiceButton.getStyleClass().add("recording");
            } else {
                VoiceRecorder.RecordedVoice voice = voiceRecorder.stop();
                stopRecordingUi();
                voiceButton.getStyleClass().remove("recording");
                connection.sendVoice(selectedUser, voice.file(), voice.duration());
            }
        } catch (Exception e) {
            notify("Voice recording error: " + e.getMessage());
        }
    }

    @FXML
    private void insertEmoji() {
        new EmojiPicker(emoji -> {
            messageField.appendText(emoji);
            messageField.requestFocus();
        }).show();
    }

    @FXML
    private void showChats() {
        dialogsList.setVisible(true);
        dialogsList.setManaged(true);
        groupsList.setVisible(false);
        groupsList.setManaged(false);
    }

    @FXML
    private void showGroups() {
        dialogsList.setVisible(false);
        dialogsList.setManaged(false);
        groupsList.setVisible(true);
        groupsList.setManaged(true);
    }

    @FXML
    private void openCreateGroup() {
        Stage stage = new Stage();
        TextField name = new TextField();
        name.setPromptText("Group name");
        TextField members = new TextField();
        members.setPromptText("Members separated by commas: Alex,Vika");
        Button create = new Button("Create group");
        create.setMaxWidth(Double.MAX_VALUE);
        create.setOnAction(event -> {
            if (!name.getText().trim().isBlank()) {
                connection.createGroup(name.getText().trim(), null, Arrays.stream(members.getText().split(","))
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .toList());
                stage.close();
            }
        });
        VBox root = new VBox(14, new Label("New group"), name, members, create);
        root.setPadding(new Insets(22));
        root.getStyleClass().add("auth-card");
        Scene scene = new Scene(root, 420, 260);
        scene.getStylesheets().add(ChatController.class.getResource("/css/style.css").toExternalForm());
        stage.setTitle("Create group");
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void openProfileSettings() {
        new ProfileSettingsWindow().show(connection, emails.get(connection.getUsername()), avatars.get(connection.getUsername()));
    }

    private void handleMessage(Message message) {
        switch (message.getType()) {
            case "users" -> updateOnlineUsers(message);
            case "profiles" -> updateProfiles(message);
            case "groups" -> updateGroups(message);
            case "group_invite" -> notify("You were invited to group: " + message.getGroupName());
            case "group_chat" -> addGroupMessage(message);
            case "chat" -> addChatMessage(new ChatMessage(message.getMessageId(), message.getFrom(), message.getTo(), message.getText(), message.getTime()));
            case "voice" -> addChatMessage(new ChatMessage(message.getMessageId(), message.getFrom(), message.getTo(), "Voice message",
                    message.getTime(), "voice", saveIncomingBinary(message, "received_voice"), "voice.wav", 0, message.getDuration()));
            case "file" -> addChatMessage(new ChatMessage(message.getMessageId(), message.getFrom(), message.getTo(), message.getFileName(),
                    message.getTime(), message.getMessageType(), saveIncomingBinary(message, "received_files"), message.getFileName(), message.getFileSize(), 0));
            case "history" -> {
                messages.setAll(message.getHistory());
                updateLastMessagesFromHistory();
                scrollDown();
            }
            case "typing" -> showTyping(message.getFrom());
            case "dialog_deleted" -> removeDialog(message);
            case "delete_message_all" -> removeMessageById(message.getMessageId());
            case "profile_updated" -> {
                ownNameLabel.setText(message.getUser());
                chatStatusLabel.setText("Profile updated");
            }
            case "profile_error", "error", "auth_error" -> notify(message.getText());
            default -> {
            }
        }
    }

    private void selectDialog(DialogItem item) {
        if (item == null) {
            return;
        }
        selectedUser = item.getUsername();
        selectedGroup = null;
        item.setUnreadCount(0);
        dialogsList.refresh();
        titleLabel.setText(selectedUser);
        chatStatusLabel.setText(item.isOnline() ? "Online" : "Offline");
        setRoundedImage(chatAvatarView, avatars.get(selectedUser), 42);
        messages.clear();
        connection.requestHistory(selectedUser);
    }

    private void selectGroup(GroupChat group) {
        if (group == null) {
            return;
        }
        selectedGroup = group;
        selectedUser = null;
        titleLabel.setText(group.getName());
        chatStatusLabel.setText(group.getMembers().size() + " members");
        setRoundedImage(chatAvatarView, group.getAvatarPath(), 46);
        messages.clear();
    }

    private void updateOnlineUsers(Message message) {
        for (DialogItem item : dialogs) {
            item.setOnline(message.getUsers().contains(item.getUsername()));
        }
        for (String username : message.getUsers()) {
            if (!username.equals(connection.getUsername())) {
                ensureDialog(username).setOnline(true);
            }
        }
        filterLists();
    }

    private void updateProfiles(Message message) {
        for (User user : message.getProfiles()) {
            avatars.put(user.getUsername(), user.getAvatarPath());
            emails.put(user.getUsername(), user.getEmail());
            if (user.getUsername().equals(connection.getUsername())) {
                setRoundedImage(ownAvatarView, user.getAvatarPath(), 54);
            } else {
                ensureDialog(user.getUsername());
            }
        }
        filterLists();
    }

    private void updateGroups(Message message) {
        groups.setAll(message.getGroups());
        filterLists();
    }

    private DialogItem ensureDialog(String username) {
        return dialogByUser.computeIfAbsent(username, key -> {
            DialogItem item = new DialogItem(key, avatars.get(key), false);
            dialogs.add(item);
            return item;
        });
    }

    private void addChatMessage(ChatMessage chatMessage) {
        String companion = chatMessage.getSender().equals(connection.getUsername()) ? chatMessage.getReceiver() : chatMessage.getSender();
        DialogItem dialog = ensureDialog(companion);
        dialog.setLastMessage(lastMessageText(chatMessage));
        dialog.setTime(chatMessage.formattedTime());
        if (!companion.equals(selectedUser)) {
            dialog.setUnreadCount(dialog.getUnreadCount() + 1);
            notify("New message from " + companion);
        }
        filterLists();
        if (belongsToCurrentDialog(chatMessage)) {
            messages.add(chatMessage);
            scrollDown();
        }
    }

    private String lastMessageText(ChatMessage message) {
        if (message.isVoice()) {
            return "Voice message";
        }
        if (message.isImage()) {
            return "Image: " + message.getFileName();
        }
        if (message.isFile()) {
            return "File: " + message.getFileName();
        }
        return message.getMessage();
    }

    private void updateLastMessagesFromHistory() {
        if (selectedUser == null || messages.isEmpty()) {
            return;
        }
        ChatMessage last = messages.get(messages.size() - 1);
        DialogItem dialog = ensureDialog(selectedUser);
        dialog.setLastMessage(lastMessageText(last));
        dialog.setTime(last.formattedTime());
        dialogsList.refresh();
    }

    private void showTyping(String from) {
        if (from != null && from.equals(selectedUser)) {
            typingLabel.setText(from + " is typing...");
            typingLabel.setVisible(true);
            typingHideDelay.playFromStart();
        }
    }

    private void filterLists() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        filteredDialogs.setAll(dialogs.stream()
                .filter(item -> item.getUsername().toLowerCase().contains(query)
                        || item.getLastMessage().toLowerCase().contains(query))
                .toList());
        filteredGroups.setAll(groups.stream()
                .filter(group -> group.getName().toLowerCase().contains(query)
                        || group.getMembers().stream().anyMatch(member -> member.toLowerCase().contains(query)))
                .toList());
        dialogsList.refresh();
        groupsList.refresh();
    }

    private void addGroupMessage(Message message) {
        if (selectedGroup != null && selectedGroup.getId() == message.getGroupId()) {
            messages.add(new ChatMessage(message.getMessageId(), message.getFrom(), "group:" + message.getGroupId(), message.getText(), message.getTime()));
            scrollDown();
        } else {
            notify("New group message");
        }
    }

    private void removeDialog(Message message) {
        String companion = message.getFrom() != null && message.getFrom().equals(connection.getUsername()) ? message.getTo() : message.getFrom();
        if (companion == null || companion.isBlank()) {
            companion = message.getUser();
        }
        DialogItem item = dialogByUser.remove(companion);
        if (item != null) {
            dialogs.remove(item);
        }
        if (companion != null && companion.equals(selectedUser)) {
            selectedUser = null;
            messages.clear();
            titleLabel.setText("Select a chat");
        }
        filterLists();
    }

    private void removeMessageById(int messageId) {
        messages.removeIf(message -> message.getId() == messageId);
    }

    private boolean belongsToCurrentDialog(ChatMessage message) {
        return selectedUser != null
                && ((message.getSender().equals(connection.getUsername()) && message.getReceiver().equals(selectedUser))
                || (message.getSender().equals(selectedUser) && message.getReceiver().equals(connection.getUsername())));
    }

    private void scrollDown() {
        if (!messages.isEmpty()) {
            messagesList.scrollTo(messages.size() - 1);
        }
    }

    private void sendFile(Path file) {
        try {
            if (selectedUser != null) {
                connection.sendFile(selectedUser, file);
            } else {
                notify("Group file sending is not available yet");
            }
        } catch (Exception e) {
            notify("Cannot send file: " + e.getMessage());
        }
    }

    private void pasteImageFromClipboard() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (!clipboard.hasImage() || selectedUser == null) {
            return;
        }
        Image image = clipboard.getImage();
        ImageView preview = new ImageView(image);
        preview.setPreserveRatio(true);
        preview.setFitWidth(360);
        preview.setFitHeight(260);
        Button send = new Button("Send");
        Button cancel = new Button("Cancel");
        Stage stage = new Stage();
        send.setOnAction(event -> {
            try {
                Path dir = Path.of("clipboard_images");
                Files.createDirectories(dir);
                Path file = dir.resolve("pasted_" + System.currentTimeMillis() + ".png").toAbsolutePath();
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file.toFile());
                sendFile(file);
                stage.close();
            } catch (Exception e) {
                notify("Cannot send pasted image: " + e.getMessage());
            }
        });
        cancel.setOnAction(event -> stage.close());
        VBox root = new VBox(14, new Label("Image preview"), preview, new HBox(10, send, cancel));
        root.setPadding(new Insets(18));
        root.getStyleClass().add("auth-card");
        Scene scene = new Scene(root, 430, 380);
        scene.getStylesheets().add(ChatController.class.getResource("/css/style.css").toExternalForm());
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(scene);
        stage.setTitle("Paste image");
        stage.show();
    }

    private String saveIncomingBinary(Message message, String folder) {
        if (message.getFileData() == null || message.getFileData().isBlank()) {
            return message.getFilePath();
        }
        try {
            Path directory = Path.of(folder);
            Files.createDirectories(directory);
            String name = message.getFileName() == null ? "file.bin" : message.getFileName();
            Path file = directory.resolve(System.currentTimeMillis() + "_" + name).toAbsolutePath();
            Files.write(file, Base64.getDecoder().decode(message.getFileData()));
            return file.toString();
        } catch (Exception e) {
            notify("Cannot save file: " + e.getMessage());
            return message.getFilePath();
        }
    }

    private void openFile(ChatMessage message) {
        File file = message.getFilePath() == null ? null : new File(message.getFilePath());
        if (file == null || !file.exists()) {
            notify("File is missing: " + message.getFileName());
            return;
        }
        try {
            if (message.isImage()) {
                imageViewer.show(file);
            } else if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (Exception e) {
            notify("Cannot open file: " + e.getMessage());
        }
    }

    private void setRoundedImage(ImageView imageView, String path, double size) {
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        imageView.setImage(AvatarUtil.loadAvatar(path, size));
        imageView.setClip(new Circle(size / 2, size / 2, size / 2));
    }

    private void installIcons() {
        attachButton.setGraphic(IconUtil.icon("attachment.png", "+", 18));
        emojiButton.setGraphic(IconUtil.icon("emoji.png", ":)", 20));
        voiceButton.setGraphic(IconUtil.icon("microphone.png", "Mic", 20));
        sendButton.setGraphic(IconUtil.icon("send.png", ">", 18));
        profileEditButton.setGraphic(IconUtil.icon("profile.png", "Edit", 12));
        newGroupButton.setGraphic(IconUtil.icon("group.png", "+", 16));
    }

    private void startRecordingUi() {
        recordingSeconds = 0;
        recordingWavesBox.setVisible(true);
        recordingWavesBox.setManaged(true);
        recordingTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            recordingSeconds++;
            recordingTimerLabel.setText("REC " + recordingSeconds + "s");
        }));
        recordingTimer.setCycleCount(Timeline.INDEFINITE);
        recordingTimer.play();
    }

    private void stopRecordingUi() {
        recordingTimerLabel.setText("");
        recordingWavesBox.setVisible(false);
        recordingWavesBox.setManaged(false);
        if (recordingTimer != null) {
            recordingTimer.stop();
        }
    }

    private void notify(String text) {
        chatStatusLabel.setText(text);
    }

    private class DialogCell extends ListCell<DialogItem> {
        @Override
        protected void updateItem(DialogItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setContextMenu(null);
                return;
            }
            ImageView avatar = new ImageView();
            setRoundedImage(avatar, avatars.get(item.getUsername()), 44);
            Label name = new Label(item.getUsername());
            name.getStyleClass().add("dialog-name");
            Label last = new Label(item.getLastMessage());
            last.getStyleClass().add("dialog-last");
            Label time = new Label(item.getTime());
            time.getStyleClass().add("dialog-time");
            Label unread = new Label(item.getUnreadCount() > 0 ? String.valueOf(item.getUnreadCount()) : "");
            unread.getStyleClass().add("unread-badge");
            unread.setVisible(item.getUnreadCount() > 0);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox top = new HBox(8, name, spacer, time);
            HBox bottom = new HBox(8, last, unread);
            VBox text = new VBox(4, top, bottom);
            HBox row = new HBox(12, avatar, text);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add(item.isOnline() ? "dialog-row-online" : "dialog-row");
            setGraphic(row);

            MenuItem pin = new MenuItem("Pin chat");
            pin.setOnAction(event -> {
                dialogs.remove(item);
                dialogs.add(0, item);
                filterLists();
            });
            MenuItem clear = new MenuItem("Clear history");
            clear.setOnAction(event -> {
                if (item.getUsername().equals(selectedUser)) {
                    messages.clear();
                }
            });
            MenuItem local = new MenuItem("Delete only for me");
            local.setOnAction(event -> confirm("Hide dialog?", () -> {
                connection.deleteDialogLocal(item.getUsername());
                dialogs.remove(item);
                dialogByUser.remove(item.getUsername());
                filterLists();
            }));
            MenuItem all = new MenuItem("Delete for everyone");
            all.setOnAction(event -> confirm("Delete dialog for everyone?", () -> connection.deleteDialogForAll(item.getUsername())));
            setContextMenu(new ContextMenu(pin, clear, local, all));
        }
    }

    private class GroupCell extends ListCell<GroupChat> {
        @Override
        protected void updateItem(GroupChat item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setContextMenu(null);
                return;
            }
            ImageView avatar = new ImageView();
            setRoundedImage(avatar, item.getAvatarPath(), 44);
            Label name = new Label(item.getName());
            name.getStyleClass().add("dialog-name");
            Label last = new Label(item.getMembers().size() + " members");
            last.getStyleClass().add("dialog-last");
            VBox text = new VBox(4, name, last);
            HBox row = new HBox(12, avatar, text);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("dialog-row-online");
            setGraphic(row);

            MenuItem remove = new MenuItem("Remove member");
            remove.setOnAction(event -> {
                TextField member = new TextField();
                member.setPromptText("Member name");
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setHeaderText("Remove member from group");
                alert.getDialogPane().setContent(member);
                alert.showAndWait().filter(ButtonType.OK::equals)
                        .ifPresent(button -> connection.removeGroupMember(item.getId(), member.getText().trim()));
            });
            MenuItem leave = new MenuItem("Leave group");
            leave.setOnAction(event -> confirm("Leave group?", () -> connection.leaveGroup(item.getId())));
            setContextMenu(new ContextMenu(remove, leave));
        }
    }

    private class MessageCell extends ListCell<ChatMessage> {
        @Override
        protected void updateItem(ChatMessage item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setContextMenu(null);
                return;
            }
            boolean own = item.getSender().equals(connection.getUsername());
            ImageView avatar = new ImageView();
            setRoundedImage(avatar, avatars.get(item.getSender()), 34);
            Label author = new Label((own ? "You" : item.getSender()) + " - " + item.formattedTime());
            author.getStyleClass().add("message-author");
            VBox bubble = new VBox(6, author, contentFor(item));
            bubble.getStyleClass().add(own ? "message-own" : "message-other");
            bubble.setMaxWidth(560);
            bubble.setPadding(new Insets(10, 14, 10, 14));
            HBox wrapper = own ? new HBox(bubble, avatar) : new HBox(avatar, bubble);
            wrapper.setSpacing(10);
            wrapper.setAlignment(own ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            wrapper.setPadding(new Insets(5, 16, 5, 16));
            FadeTransition transition = new FadeTransition(Duration.millis(160), wrapper);
            transition.setFromValue(0.55);
            transition.setToValue(1);
            transition.play();
            setGraphic(wrapper);

            MenuItem copy = new MenuItem("Copy text");
            copy.setOnAction(event -> {
                ClipboardContent content = new ClipboardContent();
                content.putString(item.getMessage());
                Clipboard.getSystemClipboard().setContent(content);
            });
            MenuItem removeLocal = new MenuItem("Delete message");
            removeLocal.setOnAction(event -> messages.remove(item));
            MenuItem removeAll = new MenuItem("Delete for everyone");
            removeAll.setOnAction(event -> connection.deleteMessageForAll(selectedUser, item.getId()));
            setContextMenu(new ContextMenu(copy, removeLocal, removeAll));
        }

        private javafx.scene.Node contentFor(ChatMessage item) {
            if (item.isVoice()) {
                return new VoiceMessageView(item.getFilePath(), item.getDuration());
            }
            if (item.isFile()) {
                return fileCard(item);
            }
            Label text = new Label(item.getMessage());
            text.setWrapText(true);
            return text;
        }

        private javafx.scene.Node fileCard(ChatMessage item) {
            VBox card = new VBox(8);
            card.getStyleClass().add("file-card");
            if (item.isImage()) {
                File file = new File(item.getFilePath());
                if (file.exists()) {
                    ImageView thumbnail = new ImageView(new Image(file.toURI().toString(), 220, 150, true, true));
                    thumbnail.setPreserveRatio(true);
                    card.getChildren().add(thumbnail);
                }
            }
            Label name = new Label(item.getFileName() == null ? item.getMessage() : item.getFileName());
            name.getStyleClass().add("file-name");
            Label meta = new Label(formatBytes(item.getFileSize()) + " - " + item.formattedTime());
            meta.getStyleClass().add("message-time");
            card.getChildren().addAll(name, meta);
            card.setOnMouseClicked(event -> openFile(item));
            return card;
        }

        private String formatBytes(long bytes) {
            if (bytes < 1024) {
                return bytes + " B";
            }
            if (bytes < 1024 * 1024) {
                return (bytes / 1024) + " KB";
            }
            return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
        }
    }

    private void confirm(String text, Runnable action) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText(null);
        alert.showAndWait().filter(ButtonType.OK::equals).ifPresent(button -> action.run());
    }
}
