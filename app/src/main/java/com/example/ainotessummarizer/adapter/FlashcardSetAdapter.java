package com.example.ainotessummarizer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ainotessummarizer.R;
import com.example.ainotessummarizer.model.FlashcardSetModel;

import java.util.List;

public class FlashcardSetAdapter extends RecyclerView.Adapter<FlashcardSetAdapter.SetViewHolder> {

    private List<FlashcardSetModel> setList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public FlashcardSetAdapter(List<FlashcardSetModel> setList, OnItemClickListener listener) {
        this.setList = setList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_flashcard_set, parent, false);
        return new SetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SetViewHolder holder, int position) {
        FlashcardSetModel model = setList.get(position);
        holder.tvTitle.setText(model.getSetTitle());
        holder.tvDescription.setText(model.getSetDescription());

        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                listener.onItemClick(adapterPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return setList.size();
    }

    public static class SetViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription;

        public SetViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvFCSetTitle);
            tvDescription = itemView.findViewById(R.id.tvFCSetDetails);
        }
    }
}