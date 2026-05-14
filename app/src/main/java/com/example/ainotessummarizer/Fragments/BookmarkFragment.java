package com.example.ainotessummarizer.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.ainotessummarizer.adapter.BookmarkAdapter;
import com.example.ainotessummarizer.databinding.FragmentBookmarkBinding;

/**
 * Bookmark tab — displays saved/bookmarked AI chat sessions.
 *
 * UI-only structure:
 * • RecyclerView with BookmarkAdapter (empty by default)
 * • Empty state shown when adapter has no items
 *
 * MVVM-ready: call {@code adapter.submitList(list)} from a ViewModel observer
 * to populate the list without changing any UI code.
 */
public class BookmarkFragment extends Fragment {

    private FragmentBookmarkBinding binding;
    private BookmarkAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentBookmarkBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        updateEmptyState();
    }

    private void setupRecyclerView() {
        adapter = new BookmarkAdapter();
        binding.rvBookmarks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvBookmarks.setAdapter(adapter);

        // Future: observe ViewModel LiveData and call adapter.submitList(list)
        // Then call updateEmptyState() inside the observer.
    }

    /** Shows / hides the empty state based on adapter item count. */
    private void updateEmptyState() {
        boolean isEmpty = adapter.getItemCount() == 0;
        binding.layoutEmptyBookmark.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.rvBookmarks.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;  // Avoid memory leaks
    }
}