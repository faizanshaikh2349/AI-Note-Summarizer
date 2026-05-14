package com.example.ainotessummarizer.Fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.ainotessummarizer.BuildConfig;
import com.example.ainotessummarizer.R;
import com.example.ainotessummarizer.adapter.HistoryAdapter;
import com.example.ainotessummarizer.databinding.FragmentHistoryBinding;
import com.example.ainotessummarizer.model.QuestionModel;
import com.example.ainotessummarizer.model.QuestionResponseModel;
import com.example.ainotessummarizer.network.OpenAIService;
import com.example.ainotessummarizer.network.RetrofitClient;
import com.example.ainotessummarizer.repository.QuestionHistoryRepository;
import com.example.ainotessummarizer.session.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryFragment extends Fragment {

    private static final int MAX_QUESTIONS = 20;

    private FragmentHistoryBinding binding;
    private HistoryAdapter adapter;
    private SessionManager sessionManager;
    private QuestionHistoryRepository questionHistoryRepository;

    private final List<QuestionModel> questionList = new ArrayList<>();

    private String pendingTopic;
    private int pendingNumber;
    private String pendingType;
    private boolean isGenerating;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        sessionManager = new SessionManager(requireContext());
        questionHistoryRepository = new QuestionHistoryRepository(requireContext());

        setupRecyclerView();
        setupSearch();
        setupGenerateButton();
        setupClearHistory();
        loadHistoryFromDatabase();
    }

    private void setupRecyclerView() {

        adapter = new HistoryAdapter();

        binding.rvHistory.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        binding.rvHistory.setAdapter(adapter);

        adapter.setOnHistoryClickListener(question ->
                openQuestionsFragment(question)
        );
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterHistory(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupGenerateButton() {
        binding.btnGenerateQuestions.setOnClickListener(v -> {
            if (!hasValidUserSession()) {
                showToast("Please log in again to generate questions");
                return;
            }

            showGenerateDialog();
        });
    }

    private void showGenerateDialog() {

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_generate_questions, null);

        EditText editTopic = dialogView.findViewById(R.id.editTopic);
        EditText editNumber = dialogView.findViewById(R.id.editNumber);
        RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroupType);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btnGenerate).setOnClickListener(v -> {
            if (isGenerating) {
                showToast("Question generation is already in progress");
                return;
            }

            String topic = editTopic.getText().toString().trim();
            String numberStr = editNumber.getText().toString().trim();

            if (topic.isEmpty()) {
                editTopic.setError("Enter topic");
                return;
            }

            if (numberStr.isEmpty()) {
                editNumber.setError("Enter number");
                return;
            }

            int number;
            try {
                number = Integer.parseInt(numberStr);
            } catch (NumberFormatException e) {
                editNumber.setError("Enter a valid number");
                return;
            }

            if (number <= 0) {
                editNumber.setError("Number must be at least 1");
                return;
            }

            if (number > MAX_QUESTIONS) {
                editNumber.setError("Maximum " + MAX_QUESTIONS + " questions");
                return;
            }

            int selectedId = radioGroup.getCheckedRadioButtonId();

            if (selectedId == -1) {
                showToast("Select question type");
                return;
            }

            RadioButton selectedRadio = dialogView.findViewById(selectedId);
            String type = selectedRadio.getText().toString();

            pendingTopic = topic;
            pendingNumber = number;
            pendingType = type;

            if (BuildConfig.GROQ_API_KEY == null || BuildConfig.GROQ_API_KEY.trim().isEmpty()) {
                showToast("Missing GROQ API key. Add GROQ_API_KEY to local.properties");
                return;
            }

            setGeneratingState(true);
            generateAIQuestions(topic, number, type);

            dialog.dismiss();
        });

        dialog.show();
    }

    private void generateAIQuestions(String topic, int number, String type) {

        OpenAIService service =
                RetrofitClient.getClient().create(OpenAIService.class);

        RequestBody body;
        try {
            JSONObject payload = new JSONObject();
            payload.put("model", "llama-3.3-70b-versatile");

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", buildPrompt(topic, number, type)));

            payload.put("messages", messages);

            body = RequestBody.create(
                    payload.toString(),
                    MediaType.parse("application/json")
            );
        } catch (JSONException e) {
            setGeneratingState(false);
            showToast("Could not prepare question request");
            return;
        }

        Call<QuestionResponseModel> call = service.generateQuestions(body);

        call.enqueue(new Callback<QuestionResponseModel>() {

            @Override
            public void onResponse(Call<QuestionResponseModel> call,
                                   Response<QuestionResponseModel> response) {
                setGeneratingState(false);

                if (!response.isSuccessful()) {

                    if (response.code() == 401) {
                        showToast("Invalid API key");
                    } else if (response.code() == 429) {
                        showToast("Rate limit exceeded");
                    } else if (response.code() >= 500) {
                        showToast("Server error. Try again later");
                    } else {
                        showToast("Request failed: " + response.code());
                    }

                    return;
                }

                if (response.body() == null ||
                        response.body().choices == null ||
                        response.body().choices.isEmpty()) {

                    showToast("AI returned empty response");

                    return;
                }

                String questions =
                        response.body()
                                .choices
                                .get(0)
                                .message
                                .content;

                if (questions == null || questions.trim().isEmpty()) {

                    showToast("No questions generated");

                    return;
                }

                int questionCount = getQuestionCount(questions);

                QuestionModel question = new QuestionModel(
                        UUID.randomUUID().toString(),
                        pendingTopic,
                        System.currentTimeMillis(),
                        questionCount == 0 ? pendingNumber : questionCount,
                        pendingType,
                        questions
                );

                boolean saved = questionHistoryRepository.saveQuestionSet(
                        getCurrentUserEmail(),
                        question
                );

                if (saved) {
                    loadHistoryFromDatabase();
                } else {
                    questionList.add(0, question);
                    filterHistory(binding.etSearch.getText() == null
                            ? ""
                            : binding.etSearch.getText().toString());
                    showToast("Saved locally, but history could not be linked to this user");
                }

                openQuestionsFragment(question);
            }

            @Override
            public void onFailure(Call<QuestionResponseModel> call, Throwable t) {
                setGeneratingState(false);
                showToast("Network error: " + t.getMessage());
            }
        });
    }

    private String buildPrompt(String topic, int number, String type) {
        if ("MCQ".equalsIgnoreCase(type)) {
            return "Generate " + number + " multiple-choice questions about " + topic + ". "
                    + "Return only a numbered list. Keep each question on a single line in the format: "
                    + "Question | A) option | B) option | C) option | D) option. Do not include answers.";
        }

        return "Generate " + number + " " + type + " questions about " + topic + ". "
                + "Return only a numbered list with one complete question per line.";
    }

    private int getQuestionCount(String questionsText) {
        if (questionsText == null || questionsText.trim().isEmpty()) {
            return 0;
        }

        int count = 0;
        String[] lines = questionsText.split("\n");

        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                count++;
            }
        }

        return count;
    }

    private void filterHistory(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        List<QuestionModel> filteredList = new ArrayList<>();

        for (QuestionModel question : questionList) {
            String searchableText = (
                    question.getTitle() + " "
                            + question.getQuestionType() + " "
                            + question.getQuestionCount()
            ).toLowerCase();

            if (normalizedQuery.isEmpty() || searchableText.contains(normalizedQuery)) {
                filteredList.add(question);
            }
        }

        adapter.submitList(filteredList, this::updateEmptyState);
    }

    private void openQuestionsFragment(QuestionModel questionModel) {
        if (!isAdded()) {
            return;
        }

        QuestionsFragment fragment = new QuestionsFragment();

        Bundle bundle = new Bundle();
        bundle.putString("questions", questionModel.getQuestions());
        bundle.putString("title", questionModel.getTitle());
        bundle.putLong("timestamp", questionModel.getTimestamp());
        bundle.putInt("questionCount", questionModel.getQuestionCount());
        bundle.putString("questionType", questionModel.getQuestionType());
        fragment.setArguments(bundle);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void setupClearHistory() {

        binding.btnClearHistory.setOnClickListener(v -> {
            String userEmail = getCurrentUserEmail();
            if (userEmail == null) {
                questionList.clear();
                filterHistory("");
                return;
            }

            questionHistoryRepository.clearQuestionHistory(userEmail);
            loadHistoryFromDatabase();
        });
    }

    private void loadHistoryFromDatabase() {
        questionList.clear();

        String userEmail = getCurrentUserEmail();
        if (userEmail != null) {
            questionList.addAll(questionHistoryRepository.getQuestionHistory(userEmail));
        }

        filterHistory(binding.etSearch.getText() == null
                ? ""
                : binding.etSearch.getText().toString());
    }

    private void updateEmptyState() {

        boolean isEmpty = adapter.getItemCount() == 0;

        binding.layoutEmptyHistory.setVisibility(
                isEmpty ? View.VISIBLE : View.GONE
        );

        binding.rvHistory.setVisibility(
                isEmpty ? View.GONE : View.VISIBLE
        );
    }

    private void setGeneratingState(boolean generating) {
        isGenerating = generating;

        if (binding == null) {
            return;
        }

        binding.btnGenerateQuestions.setEnabled(!generating);
        binding.btnGenerateQuestions.setText(
                generating ? "Generating..." : "Generate Questions"
        );
    }

    private void showToast(String message) {
        if (!isAdded()) {
            return;
        }

        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    private boolean hasValidUserSession() {
        return sessionManager != null
                && sessionManager.isSessionValid()
                && getCurrentUserEmail() != null;
    }

    private String getCurrentUserEmail() {
        return sessionManager == null ? null : sessionManager.getUserEmail();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (questionHistoryRepository != null) {
            questionHistoryRepository.close();
        }
    }
}
