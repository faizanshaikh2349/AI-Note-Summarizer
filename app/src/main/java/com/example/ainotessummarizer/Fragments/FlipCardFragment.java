package com.example.ainotessummarizer.Fragments;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.ainotessummarizer.R;
import com.example.ainotessummarizer.model.FlashcardModel;

public class FlipCardFragment extends Fragment {
    private static final String ARG_FLASHCARD = "flashcard";
    private FlashcardModel flashcard;
    private View cardFront, cardBack;
    private TextView textFront, textBack;
    private boolean isFrontVisible = true;

    public static FlipCardFragment newInstance(FlashcardModel flashcard) {
        FlipCardFragment fragment = new FlipCardFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_FLASHCARD, flashcard);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) flashcard = (FlashcardModel) getArguments().getSerializable(ARG_FLASHCARD);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.custom_flashcard_display, container, false);
        cardFront = view.findViewById(R.id.card_front);
        cardBack = view.findViewById(R.id.card_back);
        textFront = view.findViewById(R.id.text_front);
        textBack = view.findViewById(R.id.text_back);

        if (flashcard != null) {
            textFront.setText(flashcard.getTerm());
            textBack.setText(flashcard.getDefinition());
        }

        float scale = getResources().getDisplayMetrics().density;
        cardFront.setCameraDistance(8000 * scale);
        cardBack.setCameraDistance(8000 * scale);

        cardFront.setVisibility(View.VISIBLE);
        cardBack.setVisibility(View.GONE);
        view.findViewById(R.id.flashcard_card).setOnClickListener(v -> flipCard());

        return view;
    }

    private void flipCard() {
        AnimatorSet frontFlipOut = (AnimatorSet) AnimatorInflater.loadAnimator(requireContext(), R.anim.card_flip_right_out);
        AnimatorSet backFlipIn = (AnimatorSet) AnimatorInflater.loadAnimator(requireContext(), R.anim.card_flip_right_in);

        if (isFrontVisible) {
            frontFlipOut.setTarget(cardFront); backFlipIn.setTarget(cardBack);
            frontFlipOut.start(); backFlipIn.start();
            frontFlipOut.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    cardFront.setVisibility(View.GONE); cardBack.setVisibility(View.VISIBLE);
                    isFrontVisible = false;
                }
            });
        } else {
            frontFlipOut.setTarget(cardBack); backFlipIn.setTarget(cardFront);
            frontFlipOut.start(); backFlipIn.start();
            frontFlipOut.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    cardFront.setVisibility(View.VISIBLE); cardBack.setVisibility(View.GONE);
                    isFrontVisible = true;
                }
            });
        }
    }
}