package com.example.ainotessummarizer.model;

import java.util.List;

public class QuestionResponseModel {

    public List<Choice> choices;

    public static class Choice {
        public Message message;
    }

    public static class Message {
        public String content;
    }
}