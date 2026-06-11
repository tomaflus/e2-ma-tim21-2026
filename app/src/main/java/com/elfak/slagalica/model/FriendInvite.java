package com.elfak.slagalica.model;

public class FriendInvite {
    private String id;
    private String senderId;
    private String senderName;
    private String receiverId;
    private String status; 
    private String gameId;
    private long timestamp;

    public FriendInvite() {}

    public FriendInvite(String senderId, String senderName, String receiverId) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.status = "PENDING";
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getReceiverId() { return receiverId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    public long getTimestamp() { return timestamp; }
}