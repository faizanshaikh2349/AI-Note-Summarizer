package com.example.ainotessummarizer.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ainotessummarizer.databinding.ItemBookmarkBinding;
import com.example.ainotessummarizer.model.ChatModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * Adapter for displaying bookmarked chat sessions.
 * Uses ListAdapter + DiffUtil for efficient, animation-friendly updates.
 * No dummy data — list starts empty and is populated via {@link #submitList}.
 */
public class BookmarkAdapter extends ListAdapter<ChatModel, BookmarkAdapter.BookmarkViewHolder> {

    /** Callback for item interactions — wire up in the future. */
    public interface OnBookmarkClickListener {
        void onItemClick(ChatModel chat);
        void onBookmarkToggle(ChatModel chat, int position);
    }

    private OnBookmarkClickListener listener;

    private static final DiffUtil.ItemCallback<ChatModel> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull ChatModel oldItem,
                                               @NonNull ChatModel newItem) {
                    return Objects.equals(oldItem.getChatId(), newItem.getChatId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull ChatModel oldItem,
                                                  @NonNull ChatModel newItem) {
                    return Objects.equals(oldItem.getTitle(), newItem.getTitle())
                            && Objects.equals(oldItem.getLastMessage(), newItem.getLastMessage())
                            && oldItem.getTimestamp() == newItem.getTimestamp()
                            && oldItem.isBookmarked() == newItem.isBookmarked();
                }
            };

    public BookmarkAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnBookmarkClickListener(OnBookmarkClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookmarkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBookmarkBinding binding = ItemBookmarkBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new BookmarkViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BookmarkViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class BookmarkViewHolder extends RecyclerView.ViewHolder {

        private final ItemBookmarkBinding binding;

        BookmarkViewHolder(@NonNull ItemBookmarkBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onItemClick(getItem(getAdapterPosition()));
                }
            });

            binding.ivBookmarkToggle.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onBookmarkToggle(getItem(getAdapterPosition()),
                            getAdapterPosition());
                }
            });
        }

        void bind(ChatModel chat) {
            binding.tvChatTitle.setText(chat.getTitle());
            binding.tvLastMessage.setText(chat.getLastMessage());

            // Format timestamp
            if (chat.getTimestamp() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
                binding.tvTimestamp.setText(sdf.format(new Date(chat.getTimestamp())));
            } else {
                binding.tvTimestamp.setText("");
            }
        }
    }
}
