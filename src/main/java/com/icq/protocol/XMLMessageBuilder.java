package com.icq.protocol;

import com.icq.model.ChatMessage;
import com.icq.model.GroupChat;
import com.icq.model.User;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class XMLMessageBuilder {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private XMLMessageBuilder() {
    }

    public static String build(Message message) {
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element root = document.createElement("message");
            document.appendChild(root);

            append(document, root, "type", message.getType());
            append(document, root, "user", message.getUser());
            append(document, root, "from", message.getFrom());
            append(document, root, "to", message.getTo());
            append(document, root, "text", message.getText());
            append(document, root, "password", message.getPassword());
            append(document, root, "avatarPath", message.getAvatarPath());
            append(document, root, "email", message.getEmail());
            append(document, root, "code", message.getCode());
            append(document, root, "newUsername", message.getNewUsername());
            append(document, root, "filePath", message.getFilePath());
            append(document, root, "fileName", message.getFileName());
            append(document, root, "fileData", message.getFileData());
            append(document, root, "messageType", message.getMessageType());
            if (message.getFileSize() > 0) {
                append(document, root, "fileSize", String.valueOf(message.getFileSize()));
            }
            if (message.getMessageId() > 0) {
                append(document, root, "messageId", String.valueOf(message.getMessageId()));
            }
            if (message.getGroupId() > 0) {
                append(document, root, "groupId", String.valueOf(message.getGroupId()));
            }
            append(document, root, "groupName", message.getGroupName());
            append(document, root, "groupAvatarPath", message.getGroupAvatarPath());
            if (message.getDuration() > 0) {
                append(document, root, "duration", String.valueOf(message.getDuration()));
            }
            if (message.getTime() != null) {
                append(document, root, "time", message.getTime().format(FORMATTER));
            }

            if (!message.getUsers().isEmpty()) {
                Element users = document.createElement("users");
                root.appendChild(users);
                for (String user : message.getUsers()) {
                    append(document, users, "user", user);
                }
            }

            if (!message.getMembers().isEmpty()) {
                Element members = document.createElement("members");
                root.appendChild(members);
                for (String member : message.getMembers()) {
                    append(document, members, "member", member);
                }
            }

            if (!message.getProfiles().isEmpty()) {
                Element profiles = document.createElement("profiles");
                root.appendChild(profiles);
                for (User user : message.getProfiles()) {
                    Element item = document.createElement("profile");
                    profiles.appendChild(item);
                    append(document, item, "username", user.getUsername());
                    append(document, item, "avatarPath", user.getAvatarPath());
                    append(document, item, "email", user.getEmail());
                    append(document, item, "lastLogin", user.getLastLogin() == null ? "" : user.getLastLogin().format(FORMATTER));
                }
            }

            if (!message.getGroups().isEmpty() || "groups".equals(message.getType())) {
                Element groups = document.createElement("groups");
                root.appendChild(groups);
                for (GroupChat group : message.getGroups()) {
                    Element item = document.createElement("group");
                    groups.appendChild(item);
                    append(document, item, "id", String.valueOf(group.getId()));
                    append(document, item, "name", group.getName());
                    append(document, item, "avatarPath", group.getAvatarPath());
                    append(document, item, "owner", group.getOwner());
                    for (String member : group.getMembers()) {
                        append(document, item, "member", member);
                    }
                }
            }

            if (!message.getHistory().isEmpty() || "history".equals(message.getType())) {
                Element history = document.createElement("history");
                root.appendChild(history);
                for (ChatMessage chatMessage : message.getHistory()) {
                    Element item = document.createElement("item");
                    history.appendChild(item);
                    append(document, item, "id", String.valueOf(chatMessage.getId()));
                    append(document, item, "from", chatMessage.getSender());
                    append(document, item, "to", chatMessage.getReceiver());
                    append(document, item, "text", chatMessage.getMessage());
                    append(document, item, "time", chatMessage.getSendTime().format(FORMATTER));
                    append(document, item, "messageType", chatMessage.getMessageType());
                    append(document, item, "filePath", chatMessage.getFilePath());
                    append(document, item, "fileName", chatMessage.getFileName());
                    append(document, item, "fileSize", String.valueOf(chatMessage.getFileSize()));
                    append(document, item, "duration", String.valueOf(chatMessage.getDuration()));
                }
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString().replace("\r", "").replace("\n", "");
        } catch (Exception e) {
            throw new IllegalStateException("Cannot build XML message", e);
        }
    }

    public static String users(List<String> users) {
        Message message = new Message();
        message.setType("users");
        message.getUsers().addAll(users);
        return build(message);
    }

    public static String profiles(List<User> users) {
        Message message = new Message();
        message.setType("profiles");
        message.getProfiles().addAll(users);
        return build(message);
    }

    public static String groups(List<GroupChat> groups) {
        Message message = new Message();
        message.setType("groups");
        message.getGroups().addAll(groups);
        return build(message);
    }

    public static String history(List<ChatMessage> messages) {
        Message message = new Message();
        message.setType("history");
        message.getHistory().addAll(messages);
        return build(message);
    }

    private static void append(Document document, Element parent, String name, String value) {
        if (value == null) {
            return;
        }
        Element element = document.createElement(name);
        element.setTextContent(value);
        parent.appendChild(element);
    }
}
