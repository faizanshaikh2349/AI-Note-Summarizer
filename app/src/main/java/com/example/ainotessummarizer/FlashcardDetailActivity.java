package com.example.ainotessummarizer;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.example.ainotessummarizer.adapter.FlashcardPagerAdapter;
import com.example.ainotessummarizer.model.FlashcardModel;
import com.example.ainotessummarizer.model.FlashcardSetModel;

import java.util.ArrayList;
import java.util.List;

public class FlashcardDetailActivity extends AppCompatActivity {

    private TextView tvDetailSetTitle, tvDetailSetDescription;
    private ViewPager2 viewPagerFlashcards;
    private FlashcardPagerAdapter pagerAdapter;
    private List<FlashcardModel> individualFlashcards;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Naya layout set kiya
        setContentView(R.layout.activity_flashcard_detail);

        // UI components ko find kiya
        Toolbar toolbar = findViewById(R.id.toolbarDetail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        tvDetailSetTitle = findViewById(R.id.tvDetailSetTitle);
        tvDetailSetDescription = findViewById(R.id.tvDetailSetDescription);
        viewPagerFlashcards = findViewById(R.id.rvIndividualFlashcards);

        // Click kiye gaye set ka data receive kiya
        FlashcardSetModel selectedSet = (FlashcardSetModel) getIntent().getSerializableExtra("selectedFlashcardSet");

        if (selectedSet != null) {
            tvDetailSetTitle.setText(selectedSet.getSetTitle());
            tvDetailSetDescription.setText(selectedSet.getSetDescription());

            individualFlashcards = selectedSet.getFlashcards();
            if (individualFlashcards == null) {
                individualFlashcards = new ArrayList<>();
            }

            // Adapter aur 3D Page Transformer set kiya
            pagerAdapter = new FlashcardPagerAdapter(this, individualFlashcards);
            viewPagerFlashcards.setAdapter(pagerAdapter);

            // Ye line card swipe karne par badhiya animation degi
            viewPagerFlashcards.setPageTransformer(new DepthPageTransformer());

        } else {
            Toast.makeText(this, "Data not found.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}