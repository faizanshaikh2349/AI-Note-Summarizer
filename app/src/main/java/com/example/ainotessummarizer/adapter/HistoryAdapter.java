package com.example.ainotessummarizer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ainotessummarizer.databinding.ItemHistoryBinding;
import com.example.ainotessummarizer.model.QuestionModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class HistoryAdapter extends ListAdapter<QuestionModel, HistoryAdapter.HistoryViewHolder> {

    public interface OnHistoryClickListener {
        void onHistoryClick(QuestionModel question);
    }

    private OnHistoryClickListener listener;

    public void setOnHistoryClickListener(OnHistoryClickListener listener) {
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<QuestionModel> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<QuestionModel>() {

                @Override
                public boolean areItemsTheSame(@NonNull QuestionModel oldItem,
                                               @NonNull QuestionModel newItem) {
                    return Objects.equals(oldItem.getId(), newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull QuestionModel oldItem,
                                                  @NonNull QuestionModel newItem) {
                    return oldItem.getTitle().equals(newItem.getTitle())
                            && oldItem.getTimestamp() == newItem.getTimestamp()
                            && oldItem.getQuestionCount() == newItem.getQuestionCount()
                            && oldItem.getQuestionType().equals(newItem.getQuestionType())
                            && oldItem.getQuestions().equals(newItem.getQuestions());
                }
            };

    public HistoryAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        ItemHistoryBinding binding = ItemHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );

        return new HistoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder {

        private final ItemHistoryBinding binding;

        HistoryViewHolder(ItemHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(QuestionModel question) {

            binding.tvChatTitle.setText(question.getTitle());
            binding.tvLastMessage.setText(
                    question.getQuestionType() + " - "
                            + question.getQuestionCount() + " "
                            + (question.getQuestionCount() == 1 ? "question" : "questions")
            );

            SimpleDateFormat sdf =
                    new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());

            binding.tvTimestamp.setText(
                    sdf.format(new Date(question.getTimestamp()))
            );

            binding.ivBookmarkIcon.setVisibility(View.VISIBLE);
            binding.ivBookmarkIcon.setImageResource(com.example.ainotessummarizer.R.drawable.ic_chevron_right);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onHistoryClick(question);
                }
            });
        }
    }
}
