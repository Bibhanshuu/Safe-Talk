package com.example.data;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ChatRoom {
    private String roomId = "";
    private List<String> participants = new ArrayList<>();
    private Timestamp createdAt = null;
    private String lastMessage = "";
    private Timestamp lastMessageTime = null;

    public ChatRoom() {}

    public ChatRoom(String roomId, List<String> participants, Timestamp createdAt, String lastMessage, Timestamp lastMessageTime) {
        this.roomId = roomId;
        this.participants = participants;
        this.createdAt = createdAt;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) { this.participants = participants; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public Timestamp getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(Timestamp lastMessageTime) { this.lastMessageTime = lastMessageTime; }
}
