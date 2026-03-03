package com.smsproject;

public class Message 
{
    private String sender;
    private String recipient;
    private String content;

    //message constructor
    public Message(String sender, String recipient, String content) {
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
    }

    //getters for sender, recipient, and content
    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getContent() {
        return content;
    }

    
}
