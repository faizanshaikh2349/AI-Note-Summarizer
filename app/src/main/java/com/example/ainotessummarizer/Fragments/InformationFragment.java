package com.example.ainotessummarizer.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ainotessummarizer.databinding.FragmentInformationBinding;

/**
 * Information tab — app info, legal links, and credits.
 *
 * UI-only structure:
 * • App logo, name, version
 * • About section
 * • Privacy Policy & Terms rows (clickable, UI only)
 * • Contact section
 * • Developer credit
 */
public class InformationFragment extends Fragment {

    private FragmentInformationBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentInformationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupClickListeners();
    }

    private void setupClickListeners() {
        // Privacy Policy — UI only; open a WebView / browser in the future
        binding.rowPrivacyPolicy.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Privacy Policy — coming soon",
                        Toast.LENGTH_SHORT).show());

        // Terms & Conditions — UI only; open a WebView / browser in the future
        binding.rowTerms.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Terms & Conditions — coming soon",
                        Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;  // Avoid memory leaks
    }
}