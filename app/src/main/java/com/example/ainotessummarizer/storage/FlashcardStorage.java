package com.example.ainotessummarizer.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.ainotessummarizer.model.FlashcardSetModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class FlashcardStorage {

    private static final String PREF_NAME = "flashcard_prefs";
    private static final String KEY_FLASHCARD_SETS = "flashcard_sets";

    private FlashcardStorage() {
    }

    public static void saveFlashcardSets(Context context, List<FlashcardSetModel> sets) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = new Gson().toJson(sets);
        sharedPreferences.edit().putString(KEY_FLASHCARD_SETS, json).apply();
    }

    public static List<FlashcardSetModel> getFlashcardSets(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(KEY_FLASHCARD_SETS, null);

        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }

        Type type = new TypeToken<List<FlashcardSetModel>>() {}.getType();
        List<FlashcardSetModel> flashcardSets = new Gson().fromJson(json, type);
        return flashcardSets == null ? new ArrayList<>() : flashcardSets;
    }
}
