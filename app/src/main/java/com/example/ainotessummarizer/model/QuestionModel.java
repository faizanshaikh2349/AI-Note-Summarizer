package com.example.ainotessummarizer.model;

public class QuestionModel {

    private final String id;
    private final String title;
    private final long timestamp;
    private final int questionCount;
    private final String questionType;
    private final String questions;

    public QuestionModel(String id,
                         String title,
                         long timestamp,
                         int questionCount,
                         String questionType,
                         String questions) {
        this.id = id;
        this.title = title;
        this.timestamp = timestamp;
        this.questionCount = questionCount;
        this.questionType = questionType;
        this.questions = questions;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getTopic() {
        return title;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public String getQuestionType() {
        return questionType;
    }

    public String getQuestions() {
        return questions;
    }
}
