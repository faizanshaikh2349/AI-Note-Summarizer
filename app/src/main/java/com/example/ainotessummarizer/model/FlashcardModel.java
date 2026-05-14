package com.example.ainotessummarizer.model;

import java.io.Serializable;

public class FlashcardModel implements Serializable {
    private String term;
    private String definition;

    public FlashcardModel(String term, String definition) {
        this.term = term;
        this.definition = definition;
    }

    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }
    public String getDefinition() { return definition; }
    public void setDefinition(String definition) { this.definition = definition; }
}