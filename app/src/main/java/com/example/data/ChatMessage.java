package com.example.data;

import com.google.firebase.Timestamp;

public class ChatMessage {
    private String id = "";
    private String senderTempId = "";
    private String text = "";
    private Timestamp timestamp = null;
    private String formattedTime = "";
    private boolean isSystemMessage = false;
    private String imageUrl = null;
    private String reaction = null;

    public ChatMessage() {
        // Required empty constructor for Firestore
    }

    public ChatMessage(String id, String senderTempId, String text, Timestamp timestamp,
                       String formattedTime, boolean isSystemMessage, String imageUrl, String reaction) {
        this.id = id;
        this.senderTempId = senderTempId;
        this.text = text;
        this.timestamp = timestamp;
        this.formattedTime = formattedTime;
        this.isSystemMessage = isSystemMessage;
        this.imageUrl = imageUrl;
        this.reaction = reaction;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderTempId() { return senderTempId; }
    public void setSenderTempId(String senderTempId) { this.senderTempId = senderTempId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public String getFormattedTime() { return formattedTime; }
    public void setFormattedTime(String formattedTime) { this.formattedTime = formattedTime; }

    public boolean isSystemMessage() { return isSystemMessage; }
    public void setSystemMessage(boolean systemMessage) { isSystemMessage = systemMessage; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getReaction() { return reaction; }
    public void setReaction(String reaction) { this.reaction = reaction; }
}
