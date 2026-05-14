package com.example.ainotessummarizer.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.ainotessummarizer.Fragments.FlipCardFragment;
import com.example.ainotessummarizer.model.FlashcardModel;

import java.util.List;

public class FlashcardPagerAdapter extends FragmentStateAdapter {
    private List<FlashcardModel> flashcards;

    public FlashcardPagerAdapter(@NonNull FragmentActivity fragmentActivity, List<FlashcardModel> flashcards) {
        super(fragmentActivity);
        this.flashcards = flashcards;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return FlipCardFragment.newInstance(flashcards.get(position));
    }

    @Override
    public int getItemCount() {
        return flashcards.size();
    }
}