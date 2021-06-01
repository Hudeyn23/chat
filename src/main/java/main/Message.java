package main;

public class Message {
    public enum MessageType {
        LOGIN, MESSAGE, INFO, REQUEST , LOGOUT;
    }

    MessageType type;
    String body;

    public Message(MessageType type, String body) {
        this.type = type;
        this.body = body;
    }

    public MessageType getType() {
        return type;
    }

    public String getBody() {
        return body;
    }
}
