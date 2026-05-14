package com.example.ainotessummarizer.Fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.example.ainotessummarizer.CreateFlashcardActivity;
import com.example.ainotessummarizer.FlashcardDetailActivity;
import com.example.ainotessummarizer.R;
import com.example.ainotessummarizer.adapter.FlashcardSetAdapter;
import com.example.ainotessummarizer.model.FlashcardSetModel;
import com.example.ainotessummarizer.storage.FlashcardStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FlashcardFragment extends Fragment {

    private RecyclerView recyclerView;
    private FlashcardSetAdapter adapter;
    private List<FlashcardSetModel> flashcardSets;
    private List<FlashcardSetModel> allFlashcardSets;
    private FloatingActionButton fabAddFlashcard;
    private EditText etFlashcardSearch;
    private TextView tvEmptyState;

    // CreateFlashcardActivity se naya set wapas receive karne ke liye
    private final ActivityResultLauncher<Intent> createFlashcardLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    FlashcardSetModel newSet = (FlashcardSetModel) result.getData().getSerializableExtra("newFlashcardSet");
                    if (newSet != null) {
                        allFlashcardSets.add(0, newSet);
                        FlashcardStorage.saveFlashcardSets(requireContext(), allFlashcardSets);
                        filterFlashcardSets(etFlashcardSearch.getText().toString().trim());
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Aapke fragment ki XML file ka naam (e.g., fragment_flashcard.xml)
        View view = inflater.inflate(R.layout.fragment_flashcard, container, false);

        recyclerView = view.findViewById(R.id.rvFlashcardSets);
        fabAddFlashcard = view.findViewById(R.id.fabAddFlashcard);
        etFlashcardSearch = view.findViewById(R.id.etFlashcardSearch);
        tvEmptyState = view.findViewById(R.id.tvFlashcardEmptyState);

        allFlashcardSets = FlashcardStorage.getFlashcardSets(requireContext());
        flashcardSets = new ArrayList<>(allFlashcardSets);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FlashcardSetAdapter(flashcardSets, position -> {
            FlashcardSetModel clickedSet = flashcardSets.get(position);
            Intent intent = new Intent(getContext(), FlashcardDetailActivity.class);
            intent.putExtra("selectedFlashcardSet", clickedSet);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        fabAddFlashcard.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), CreateFlashcardActivity.class);
            createFlashcardLauncher.launch(intent);
        });

        etFlashcardSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFlashcardSets(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        toggleEmptyState();

        return view;
    }

    private void filterFlashcardSets(String query) {
        List<FlashcardSetModel> filteredList = new ArrayList<>();
        if (query.isEmpty()) {
            filteredList.addAll(allFlashcardSets);
        } else {
            String normalizedQuery = query.toLowerCase(Locale.getDefault());
            for (FlashcardSetModel set : allFlashcardSets) {
                String title = set.getSetTitle() == null ? "" : set.getSetTitle();
                String description = set.getSetDescription() == null ? "" : set.getSetDescription();
                if (title.toLowerCase(Locale.getDefault()).contains(normalizedQuery)
                        || description.toLowerCase(Locale.getDefault()).contains(normalizedQuery)) {
                    filteredList.add(set);
                }
            }
        }

        flashcardSets.clear();
        flashcardSets.addAll(filteredList);
        adapter.notifyDataSetChanged();
        toggleEmptyState();
    }

    private void toggleEmptyState() {
        boolean isEmpty = flashcardSets.isEmpty();
        tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}
