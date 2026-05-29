package com.icq.protocol;

import com.icq.model.ChatMessage;
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

            if (!message.getHistory().isEmpty() || "history".equals(message.getType())) {
                Element history = document.createElement("history");
                root.appendChild(history);
                for (ChatMessage chatMessage : message.getHistory()) {
                    Element item = document.createElement("item");
                    history.appendChild(item);
                    append(document, item, "from", chatMessage.getSender());
                    append(document, item, "to", chatMessage.getReceiver());
                    append(document, item, "text", chatMessage.getMessage());
                    append(document, item, "time", chatMessage.getSendTime().format(FORMATTER));
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
