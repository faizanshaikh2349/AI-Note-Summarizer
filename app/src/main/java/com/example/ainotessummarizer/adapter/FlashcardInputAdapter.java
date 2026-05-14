package com.example.ainotessummarizer.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ainotessummarizer.R;
import com.example.ainotessummarizer.model.FlashcardModel;

import java.util.List;

public class FlashcardInputAdapter extends RecyclerView.Adapter<FlashcardInputAdapter.FlashcardInputViewHolder> {

    private List<FlashcardModel> flashcardList;

    public FlashcardInputAdapter(List<FlashcardModel> flashcardList) {
        this.flashcardList = flashcardList;
    }

    @NonNull
    @Override
    public FlashcardInputViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_flashcard_input, parent, false);
        return new FlashcardInputViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FlashcardInputViewHolder holder, int position) {
        FlashcardModel flashcard = flashcardList.get(position);

        if (holder.termTextWatcher != null) {
            holder.etTerm.removeTextChangedListener(holder.termTextWatcher);
        }
        if (holder.definitionTextWatcher != null) {
            holder.etDefinition.removeTextChangedListener(holder.definitionTextWatcher);
        }

        holder.etTerm.setText(flashcard.getTerm());
        holder.etDefinition.setText(flashcard.getDefinition());

        holder.termTextWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                flashcardList.get(holder.getAdapterPosition()).setTerm(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        holder.etTerm.addTextChangedListener(holder.termTextWatcher);

        holder.definitionTextWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                flashcardList.get(holder.getAdapterPosition()).setDefinition(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        holder.etDefinition.addTextChangedListener(holder.definitionTextWatcher);
    }

    @Override
    public int getItemCount() {
        return flashcardList.size();
    }

    public List<FlashcardModel> getFlashcardList() {
        return flashcardList;
    }

    public static class FlashcardInputViewHolder extends RecyclerView.ViewHolder {
        EditText etTerm, etDefinition;
        TextWatcher termTextWatcher, definitionTextWatcher;

        public FlashcardInputViewHolder(@NonNull View itemView) {
            super(itemView);
            etTerm = itemView.findViewById(R.id.etInputFCTerm);
            etDefinition = itemView.findViewById(R.id.etInputFCDefinition);
        }
    }
}