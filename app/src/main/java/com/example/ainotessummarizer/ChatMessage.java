package com.example.ainotessummarizer;

import java.util.ArrayList;
import java.util.List;

public class ChatMessage {
    public static final String SENT_BY_USER = "user";
    public static final String SENT_BY_BOT = "bot";
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_SUGGESTION = "suggestion";

    private String message;
    private String sentBy;
    private String messageType;
    private ArrayList<String> suggestionOptions;

    public ChatMessage() {
    }

    public ChatMessage(String message, String sentBy) {
        this.message = message;
        this.sentBy = sentBy;
        this.messageType = TYPE_TEXT;
    }

    public ChatMessage(String message, String sentBy, String messageType, List<String> suggestionOptions) {
        this.message = message;
        this.sentBy = sentBy;
        this.messageType = messageType;
        this.suggestionOptions = suggestionOptions == null
                ? new ArrayList<>()
                : new ArrayList<>(suggestionOptions);
    }

    public static ChatMessage createSuggestionMessage(String message, List<String> suggestions) {
        return new ChatMessage(message, SENT_BY_BOT, TYPE_SUGGESTION, suggestions);
    }

    public String getMessage() {
        return message;
    }

    public String getSentBy() {
        return sentBy;
    }

    public String getMessageType() {
        return messageType;
    }

    public ArrayList<String> getSuggestionOptions() {
        return suggestionOptions == null ? new ArrayList<>() : suggestionOptions;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setSentBy(String sentBy) {
        this.sentBy = sentBy;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public void setSuggestionOptions(ArrayList<String> suggestionOptions) {
        this.suggestionOptions = suggestionOptions;
    }

    public boolean isUser() {
        return SENT_BY_USER.equals(sentBy);
    }

    public boolean isSuggestionMessage() {
        return TYPE_SUGGESTION.equals(messageType);
    }
}
