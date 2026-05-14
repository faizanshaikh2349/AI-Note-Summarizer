package com.example.ainotessummarizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatSessionDrawerAdapter extends RecyclerView.Adapter<ChatSessionDrawerAdapter.ChatSessionViewHolder> {

    public interface DrawerActionListener {
        void onChatSelected(ChatActivity.ChatSessionItem chatSessionItem);
        void onDeleteRequested(ChatActivity.ChatSessionItem chatSessionItem);
        void onSaveToggleRequested(ChatActivity.ChatSessionItem chatSessionItem);
    }

    private final List<ChatActivity.ChatSessionItem> items = new ArrayList<>();
    private final DrawerActionListener listener;

    public ChatSessionDrawerAdapter(DrawerActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ChatActivity.ChatSessionItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatSessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_drawer_chat, parent, false);
        return new ChatSessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatSessionViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ChatSessionViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final TextView subtitleView;
        private final ImageView bookmarkView;
        private final ImageView moreView;

        ChatSessionViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.txtDrawerChatTitle);
            subtitleView = itemView.findViewById(R.id.txtDrawerChatSubtitle);
            bookmarkView = itemView.findViewById(R.id.imgDrawerBookmark);
            moreView = itemView.findViewById(R.id.btnDrawerChatActions);
        }

        void bind(ChatActivity.ChatSessionItem item) {
            titleView.setText(item.title);
            subtitleView.setText(item.lastMessage);
            subtitleView.setVisibility(item.lastMessage == null || item.lastMessage.isEmpty()
                    ? View.GONE
                    : View.VISIBLE);
            bookmarkView.setVisibility(item.bookmarked ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> listener.onChatSelected(item));

            moreView.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(itemView.getContext(), moreView);

                if (!item.legacy) {
                    popupMenu.getMenu().add(0, 1, 0, item.bookmarked
                            ? "Remove from Saved Notes"
                            : "Save Notes");
                }
                popupMenu.getMenu().add(0, 2, 1, "Delete");

                popupMenu.setOnMenuItemClickListener(menuItem -> {
                    if (menuItem.getItemId() == 1) {
                        listener.onSaveToggleRequested(item);
                        return true;
                    }

                    if (menuItem.getItemId() == 2) {
                        listener.onDeleteRequested(item);
                        return true;
                    }

                    return false;
                });
                popupMenu.show();
            });
        }
    }
}
