package com.example.ainotessummarizer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ainotessummarizer.adapter.FlashcardInputAdapter;
import com.example.ainotessummarizer.model.FlashcardModel;
import com.example.ainotessummarizer.model.FlashcardSetModel;

import java.util.ArrayList;
import java.util.List;

public class CreateFlashcardActivity extends AppCompatActivity {

    private EditText etCreateFCSetName;
    private ImageView btnCreateFcAddFlashcard;
    private RecyclerView recyclerFlashcardInputs;
    private Button btnCreateFCSet;

    private List<FlashcardModel> flashcards;
    private FlashcardInputAdapter flashcardInputAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_flashcard);

        etCreateFCSetName = findViewById(R.id.etCreateFCSetName);
        btnCreateFcAddFlashcard = findViewById(R.id.btnCreateFcAddFlashcard);
        recyclerFlashcardInputs = findViewById(R.id.recyclerFlashcardInputs);
        btnCreateFCSet = findViewById(R.id.btnCreateFCSet);

        flashcards = new ArrayList<>();
        flashcards.add(new FlashcardModel("", ""));

        recyclerFlashcardInputs.setLayoutManager(new LinearLayoutManager(this));
        flashcardInputAdapter = new FlashcardInputAdapter(flashcards);
        recyclerFlashcardInputs.setAdapter(flashcardInputAdapter);

        btnCreateFcAddFlashcard.setOnClickListener(v -> {
            flashcards.add(new FlashcardModel("", ""));
            flashcardInputAdapter.notifyItemInserted(flashcards.size() - 1);
            recyclerFlashcardInputs.scrollToPosition(flashcards.size() - 1);
        });

        btnCreateFCSet.setOnClickListener(v -> {
            String setTitle = etCreateFCSetName.getText().toString().trim();

            if (setTitle.isEmpty()) {
                etCreateFCSetName.setError("Set title cannot be empty");
                return;
            }

            List<FlashcardModel> currentFlashcards = flashcardInputAdapter.getFlashcardList();
            List<FlashcardModel> validFlashcards = new ArrayList<>();
            for (FlashcardModel fc : currentFlashcards) {
                if (!fc.getTerm().trim().isEmpty() || !fc.getDefinition().trim().isEmpty()) {
                    validFlashcards.add(fc);
                }
            }

            if (validFlashcards.isEmpty()) {
                Toast.makeText(this, "Please add at least one term and definition.", Toast.LENGTH_SHORT).show();
                return;
            }

            String setDescription = validFlashcards.size() + " terms • AI Generated";
            FlashcardSetModel newSet = new FlashcardSetModel(setTitle, setDescription, validFlashcards);

            Intent resultIntent = new Intent();
            resultIntent.putExtra("newFlashcardSet", newSet);

            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        });
    }
}