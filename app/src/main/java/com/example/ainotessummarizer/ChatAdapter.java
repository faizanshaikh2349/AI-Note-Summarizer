package com.example.ainotessummarizer;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

import io.noties.markwon.Markwon;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_AI = 1;
    private static final int VIEW_TYPE_SUGGESTION = 2;

    public interface ChatActionListener {
        void onSuggestionSelected(String suggestion);
        void onCopyMessage(String message);
        void onListenMessage(String message);
    }

    private final List<ChatMessage> messageList;
    private final Markwon markwon;
    private final Context context;
    private final ChatActionListener listener;

    public ChatAdapter(List<ChatMessage> messageList, Context context, ChatActionListener listener) {
        this.messageList = messageList;
        this.markwon = Markwon.create(context);
        this.context = context;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messageList.get(position);
        if (message.isSuggestionMessage()) {
            return VIEW_TYPE_SUGGESTION;
        }
        return message.isUser() ? VIEW_TYPE_USER : VIEW_TYPE_AI;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_USER) {
            return new UserViewHolder(inflater.inflate(R.layout.item_chat_user, parent, false));
        }

        if (viewType == VIEW_TYPE_SUGGESTION) {
            return new SuggestionViewHolder(inflater.inflate(R.layout.item_chat_suggestions, parent, false));
        }

        return new AiViewHolder(inflater.inflate(R.layout.item_chat_ai, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);

        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).textMessage.setText(message.getMessage());
            return;
        }

        if (holder instanceof SuggestionViewHolder) {
            ((SuggestionViewHolder) holder).bind(message);
            return;
        }

        ((AiViewHolder) holder).bind(message);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView textMessage;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.txtUserMessage);
        }
    }

    class AiViewHolder extends RecyclerView.ViewHolder {
        private final TextView textMessage;
        private final ImageView btnActions;

        AiViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.txtAiMessage);
            btnActions = itemView.findViewById(R.id.btnAiMessageActions);
        }

        void bind(ChatMessage message) {
            markwon.setMarkdown(textMessage, message.getMessage());

            if ("Typing...".equals(message.getMessage())) {
                btnActions.setVisibility(View.GONE);
                return;
            }

            btnActions.setVisibility(View.VISIBLE);
            btnActions.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(context, btnActions);
                popupMenu.getMenu().add(0, 1, 0, "Copy");
                popupMenu.getMenu().add(0, 2, 1, "Listen");
                popupMenu.setOnMenuItemClickListener(item -> {
                    if (listener == null) {
                        return false;
                    }

                    if (item.getItemId() == 1) {
                        listener.onCopyMessage(message.getMessage());
                        return true;
                    }

                    if (item.getItemId() == 2) {
                        listener.onListenMessage(message.getMessage());
                        return true;
                    }

                    return false;
                });
                popupMenu.show();
            });
        }
    }

    class SuggestionViewHolder extends RecyclerView.ViewHolder {
        private final TextView textMessage;
        private final LinearLayout suggestionContainer;

        SuggestionViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.txtSuggestionPrompt);
            suggestionContainer = itemView.findViewById(R.id.layoutSuggestionButtons);
        }

        void bind(ChatMessage message) {
            textMessage.setText(message.getMessage());
            suggestionContainer.removeAllViews();

            for (String suggestion : message.getSuggestionOptions()) {
                MaterialButton button = new MaterialButton(
                        context,
                        null,
                        com.google.android.material.R.attr.materialButtonOutlinedStyle);
                button.setText(suggestion);
                button.setAllCaps(false);
                button.setCornerRadius(999);
                button.setStrokeWidth((int) (1 * context.getResources().getDisplayMetrics().density));
                button.setStrokeColor(ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.primary)));
                button.setTextColor(ContextCompat.getColor(context, R.color.primary));
                button.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(context, android.R.color.white)));

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                params.topMargin = (int) (10 * context.getResources().getDisplayMetrics().density);
                button.setLayoutParams(params);
                button.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onSuggestionSelected(suggestion);
                    }
                });
                suggestionContainer.addView(button);
            }
        }
    }
}
