package com.example.ainotessummarizer.model;

public class ChatModel {

    private String chatId;
    private String title;
    private String lastMessage;
    private long timestamp;
    private boolean isBookmarked;
    private String summaryMode;

    public ChatModel() {
    }

    public ChatModel(String chatId, String title, String lastMessage,
                     long timestamp, boolean isBookmarked) {
        this(chatId, title, lastMessage, timestamp, isBookmarked, "");
    }

    public ChatModel(String chatId, String title, String lastMessage,
                     long timestamp, boolean isBookmarked, String summaryMode) {
        this.chatId = chatId;
        this.title = title;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.isBookmarked = isBookmarked;
        this.summaryMode = summaryMode;
    }

    public String getChatId() { return chatId; }
    public String getTitle() { return title; }
    public String getLastMessage() { return lastMessage; }
    public long getTimestamp() { return timestamp; }
    public boolean isBookmarked() { return isBookmarked; }
    public String getSummaryMode() { return summaryMode; }

    public void setChatId(String chatId) { this.chatId = chatId; }
    public void setTitle(String title) { this.title = title; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setBookmarked(boolean bookmarked) { isBookmarked = bookmarked; }
    public void setSummaryMode(String summaryMode) { this.summaryMode = summaryMode; }
}
