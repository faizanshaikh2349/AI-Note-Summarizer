package com.example.ainotessummarizer.Fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.ainotessummarizer.adapter.QuestionsAdapter;
import com.example.ainotessummarizer.databinding.FragmentQuestionsBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QuestionsFragment extends Fragment {

    private FragmentQuestionsBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentQuestionsBinding.inflate(inflater, container, false);

        String questionsText = "";

        Bundle args = getArguments();
        if (args != null) {
            questionsText = args.getString("questions", "");
            binding.tvQuestionSetTitle.setText(args.getString("title", "Questions"));
            binding.tvQuestionSetMeta.setText(
                    args.getString("questionType", "Question Set") + " - "
                            + args.getInt("questionCount", 0) + " "
                            + (args.getInt("questionCount", 0) == 1 ? "question" : "questions")
            );
            binding.tvQuestionSetTimestamp.setText(
                    formatTimestamp(args.getLong("timestamp", System.currentTimeMillis()))
            );
        } else {
            binding.tvQuestionSetTitle.setText("Questions");
            binding.tvQuestionSetMeta.setText("Question Set");
            binding.tvQuestionSetTimestamp.setText("");
        }

        List<String> questionsList = parseQuestions(questionsText);

        binding.btnBack.setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );

        binding.rvQuestions.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        binding.rvQuestions.setAdapter(
                new QuestionsAdapter(questionsList)
        );

        return binding.getRoot();
    }

    private String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                .format(timestamp);
    }

    private List<String> parseQuestions(String text) {

        List<String> questions = new ArrayList<>();

        if (TextUtils.isEmpty(text)) {
            return questions;
        }

        String[] lines = text.split("\n");

        for (String line : lines) {

            line = line.trim();

            if (!line.isEmpty()) {
                questions.add(line);
            }
        }

        return questions;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
