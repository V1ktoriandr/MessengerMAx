package com.icq.protocol;

import com.icq.model.ChatMessage;
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
            String time = text(root, "time");
            if (time != null && !time.isBlank()) {
                message.setTime(LocalDateTime.parse(time));
            }

            NodeList users = root.getElementsByTagName("users");
            if (users.getLength() > 0) {
                NodeList userNodes = ((Element) users.item(0)).getElementsByTagName("user");
                for (int i = 0; i < userNodes.getLength(); i++) {
                    message.getUsers().add(userNodes.item(i).getTextContent());
                }
            }

            NodeList histories = root.getElementsByTagName("history");
            if (histories.getLength() > 0) {
                NodeList items = ((Element) histories.item(0)).getElementsByTagName("item");
                for (int i = 0; i < items.getLength(); i++) {
                    Element item = (Element) items.item(i);
                    message.getHistory().add(new ChatMessage(
                            text(item, "from"),
                            text(item, "to"),
                            text(item, "text"),
                            LocalDateTime.parse(text(item, "time"))
                    ));
                }
            }

            return message;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid XML message: " + xml, e);
        }
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
