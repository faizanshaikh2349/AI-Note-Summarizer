package com.example.ainotessummarizer.model;

import java.io.Serializable;
import java.util.List;

public class FlashcardSetModel implements Serializable {
    private String setTitle;
    private String setDescription;
    private List<FlashcardModel> flashcards;

    public FlashcardSetModel(String setTitle, String setDescription, List<FlashcardModel> flashcards) {
        this.setTitle = setTitle;
        this.setDescription = setDescription;
        this.flashcards = flashcards;
    }

    public String getSetTitle() { return setTitle; }
    public void setSetTitle(String setTitle) { this.setTitle = setTitle; }
    public String getSetDescription() { return setDescription; }
    public void setDescription(String setDescription) { this.setDescription = setDescription; }
    public List<FlashcardModel> getFlashcards() { return flashcards; }
    public void setFlashcards(List<FlashcardModel> flashcards) { this.flashcards = flashcards; }
}