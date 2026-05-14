package com.example.ainotessummarizer.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.example.ainotessummarizer.databinding.ItemQuestionBinding;

import java.util.List;

public class QuestionsAdapter extends RecyclerView.Adapter<QuestionsAdapter.ViewHolder> {

    List<String> questions;

    public QuestionsAdapter(List<String> questions) {
        this.questions = questions;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        ItemQuestionBinding binding;

        ViewHolder(ItemQuestionBinding b) {
            super(b.getRoot());
            binding = b;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        ItemQuestionBinding binding =
                ItemQuestionBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        holder.binding.tvQuestion.setText(questions.get(position));
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }
}