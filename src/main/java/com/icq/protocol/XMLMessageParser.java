package com.icq.protocol;

import com.icq.model.ChatMessage;
import com.icq.model.GroupChat;
import com.icq.model.User;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDateTime;

public class XMLMessageParser {
    private XMLMessageParser() {
    }

    public static Message parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            Element root = document.getDocumentElement();

            Message message = new Message();
            message.setType(text(root, "type"));
            message.setUser(text(root, "user"));
            message.setFrom(text(root, "from"));
            message.setTo(text(root, "to"));
            message.setText(text(root, "text"));
            message.setPassword(text(root, "password"));
            message.setAvatarPath(text(root, "avatarPath"));
            message.setEmail(text(root, "email"));
            message.setCode(text(root, "code"));
            message.setNewUsername(text(root, "newUsername"));
            message.setFilePath(text(root, "filePath"));
            message.setFileName(text(root, "fileName"));
            message.setFileData(text(root, "fileData"));
            message.setMessageType(text(root, "messageType"));
            message.setFileSize(parseLong(text(root, "fileSize")));
            message.setMessageId(parseInt(text(root, "messageId")));
            message.setGroupId(parseInt(text(root, "groupId")));
            message.setGroupName(text(root, "groupName"));
            message.setGroupAvatarPath(text(root, "groupAvatarPath"));
            String duration = text(root, "duration");
            if (duration != null && !duration.isBlank()) {
                message.setDuration(Integer.parseInt(duration));
            }
            String time = text(root, "time");
            if (time != null && !time.isBlank()) {
                message.setTime(LocalDateTime.parse(time));
            }

            NodeList profiles = root.getElementsByTagName("profiles");
            if (profiles.getLength() > 0) {
                NodeList items = ((Element) profiles.item(0)).getElementsByTagName("profile");
                for (int i = 0; i < items.getLength(); i++) {
                    Element item = (Element) items.item(i);
                    message.getProfiles().add(new User(
                            0,
                            text(item, "username"),
                            "",
                            text(item, "avatarPath"),
                            text(item, "email"),
                            null,
                            parseDate(text(item, "lastLogin"))
                    ));
                }
            }

            NodeList users = root.getElementsByTagName("users");
            if (users.getLength() > 0) {
                NodeList userNodes = ((Element) users.item(0)).getElementsByTagName("user");
                for (int i = 0; i < userNodes.getLength(); i++) {
                    message.getUsers().add(userNodes.item(i).getTextContent());
                }
            }

            NodeList members = root.getElementsByTagName("members");
            if (members.getLength() > 0) {
                NodeList memberNodes = ((Element) members.item(0)).getElementsByTagName("member");
                for (int i = 0; i < memberNodes.getLength(); i++) {
                    message.getMembers().add(memberNodes.item(i).getTextContent());
                }
            }

            NodeList groupContainers = root.getElementsByTagName("groups");
            if (groupContainers.getLength() > 0) {
                NodeList items = ((Element) groupContainers.item(0)).getElementsByTagName("group");
                for (int i = 0; i < items.getLength(); i++) {
                    Element item = (Element) items.item(i);
                    GroupChat group = new GroupChat(
                            parseInt(text(item, "id")),
                            text(item, "name"),
                            text(item, "avatarPath"),
                            text(item, "owner"),
                            null
                    );
                    NodeList memberNodes = item.getElementsByTagName("member");
                    for (int j = 0; j < memberNodes.getLength(); j++) {
                        group.getMembers().add(memberNodes.item(j).getTextContent());
                    }
                    message.getGroups().add(group);
                }
            }

            NodeList histories = root.getElementsByTagName("history");
            if (histories.getLength() > 0) {
                NodeList items = ((Element) histories.item(0)).getElementsByTagName("item");
                for (int i = 0; i < items.getLength(); i++) {
                    Element item = (Element) items.item(i);
                    message.getHistory().add(new ChatMessage(
                            parseInt(text(item, "id")),
                            text(item, "from"),
                            text(item, "to"),
                            text(item, "text"),
                            LocalDateTime.parse(text(item, "time")),
                            valueOrDefault(text(item, "messageType"), "text"),
                            text(item, "filePath"),
                            text(item, "fileName"),
                            parseLong(text(item, "fileSize")),
                            parseInt(text(item, "duration"))
                    ));
                }
            }

            return message;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid XML message: " + xml, e);
        }
    }

    private static LocalDateTime parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value);
    }

    private static int parseInt(String value) {
        return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
    }

    private static long parseLong(String value) {
        return value == null || value.isBlank() ? 0 : Long.parseLong(value);
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String text(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        Node node = nodes.item(0);
        return node == null ? null : node.getTextContent();
    }
}
