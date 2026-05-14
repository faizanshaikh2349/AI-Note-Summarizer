package com.example.ainotessummarizer;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ainotessummarizer.model.ChatModel;
import com.example.ainotessummarizer.session.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity implements
        ChatAdapter.ChatActionListener,
        ChatSessionDrawerAdapter.DrawerActionListener,
        TextToSpeech.OnInitListener {

    public static final String EXTRA_CHAT_ID = "extra_chat_id";

    private static final String USER_CHATS_NODE = "UserChats";
    private static final String SESSIONS_NODE = "sessions";
    private static final String MESSAGES_NODE = "messages";
    private static final String USER_ROLE = ChatMessage.SENT_BY_USER;
    private static final String BOT_ROLE = ChatMessage.SENT_BY_BOT;
    private static final String DEFAULT_CHAT_TITLE = "New Chat";
    private static final String MODE_SHORT = "Short Summary";
    private static final String MODE_BRIEF = "Brief Summary";
    private static final String MODE_LONG = "Long Summary";
    private static final String MODE_BULLET = "Bullet Point Summary";
    private static final List<String> SUMMARY_OPTIONS = Arrays.asList(
            MODE_SHORT, MODE_BRIEF, MODE_LONG, MODE_BULLET);
    private static final String WELCOME_PROMPT = "Choose how you want your summary:";
    private static final int SPEECH_REQUEST_CODE = 100;
    private static final int MAX_TITLE_LENGTH = 28;
    private static final int MAX_CONTENT_LENGTH = 12000;
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_API_KEY = BuildConfig.GROQ_API_KEY;

    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private RecyclerView recentChatsRecycler;
    private RecyclerView savedChatsRecycler;
    private TextView recentEmptyText;
    private TextView savedEmptyText;
    private TextView toolbarTitle;
    private EditText etMessage;
    private ImageView btnSend;
    private ImageView btnMenu;
    private ImageView btnOptionsPlus;
    private ImageView btnMic;
    private ImageView btnNewChat;

    private ChatAdapter chatAdapter;
    private ChatSessionDrawerAdapter recentAdapter;
    private ChatSessionDrawerAdapter savedAdapter;
    private final List<ChatMessage> chatList = new ArrayList<>();
    private final Map<String, ChatSessionItem> chatSessionMap = new HashMap<>();

    private DatabaseReference dbRef;
    private ValueEventListener chatHistoryListener;
    private final OkHttpClient client = new OkHttpClient();
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    private String currentChatId;
    private boolean currentChatIsLegacy;
    private String currentChatTitle = DEFAULT_CHAT_TITLE;
    private String currentSummaryMode = "";
    private String requestedChatId;
    private Uri pendingCameraUri;

    private TextToSpeech textToSpeech;
    private boolean isTextToSpeechReady;

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && pendingCameraUri != null) {
                    processAttachment(pendingCameraUri, "Camera image");
                }
            });

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    processAttachment(uri, "Selected image");
                }
            });

    private final ActivityResultLauncher<String[]> openDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    processAttachment(uri, "Document");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        SessionManager sessionManager = new SessionManager(this);
        String userEmail = sessionManager.getUserEmail();

        if (userEmail == null || !sessionManager.isSessionValid()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }

        requestedChatId = getIntent().getStringExtra(EXTRA_CHAT_ID);

        String safeEmail = userEmail.replace(".", "_").replace("@", "_at_");
        dbRef = FirebaseDatabase.getInstance()
                .getReference(USER_CHATS_NODE)
                .child(safeEmail);

        initViews();
        setupRecyclerViews();
        setupClickListeners();

        textToSpeech = new TextToSpeech(this, this);
        observeChatHistory();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        recyclerView = findViewById(R.id.chatRecyclerView);
        recentChatsRecycler = findViewById(R.id.recyclerRecentChats);
        savedChatsRecycler = findViewById(R.id.recyclerSavedChats);
        recentEmptyText = findViewById(R.id.txtRecentEmpty);
        savedEmptyText = findViewById(R.id.txtSavedEmpty);
        toolbarTitle = findViewById(R.id.tvToolbarTitle);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnMenu = findViewById(R.id.btnMenu);
        btnOptionsPlus = findViewById(R.id.btnOptionsPlus);
        btnMic = findViewById(R.id.btnMic);
        btnNewChat = findViewById(R.id.btnNewChat);
    }

    private void setupRecyclerViews() {
        chatAdapter = new ChatAdapter(chatList, this, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        recentAdapter = new ChatSessionDrawerAdapter(this);
        savedAdapter = new ChatSessionDrawerAdapter(this);
        recentChatsRecycler.setLayoutManager(new LinearLayoutManager(this));
        savedChatsRecycler.setLayoutManager(new LinearLayoutManager(this));
        recentChatsRecycler.setAdapter(recentAdapter);
        savedChatsRecycler.setAdapter(savedAdapter);
        recentChatsRecycler.setNestedScrollingEnabled(false);
        savedChatsRecycler.setNestedScrollingEnabled(false);
    }

    private void setupClickListeners() {
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        btnNewChat.setOnClickListener(v -> startNewChat());
        btnOptionsPlus.setOnClickListener(v -> showBottomSheet());
        btnMic.setOnClickListener(v -> startVoiceInput());
        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText() != null
                    ? etMessage.getText().toString().trim()
                    : "";

            if (!text.isEmpty()) {
                sendTypedContent(text);
                etMessage.setText("");
            }
        });
    }

    private void startNewChat() {
        currentChatId = dbRef.child(SESSIONS_NODE).push().getKey();
        currentChatIsLegacy = false;
        currentChatTitle = DEFAULT_CHAT_TITLE;
        currentSummaryMode = "";

        if (currentChatId == null) {
            Toast.makeText(this, "Unable to create a new chat right now.", Toast.LENGTH_SHORT).show();
            return;
        }

        chatList.clear();
        chatAdapter.notifyDataSetChanged();
        toolbarTitle.setText(DEFAULT_CHAT_TITLE);

        long timestamp = System.currentTimeMillis();
        saveSessionMetadata(currentChatId, DEFAULT_CHAT_TITLE, "", timestamp, false, "");

        ChatMessage welcomeMessage = ChatMessage.createSuggestionMessage(WELCOME_PROMPT, SUMMARY_OPTIONS);
        addChatMessage(welcomeMessage);
        saveMessageToFirebase(welcomeMessage, false);
    }

    private void showBottomSheet() {
        View view = getLayoutInflater().inflate(R.layout.layout_bottom_sheet, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);

        LinearLayout cameraButton = view.findViewById(R.id.sheetBtnCamera);
        LinearLayout galleryButton = view.findViewById(R.id.sheetBtnGallery);
        LinearLayout fileButton = view.findViewById(R.id.sheetBtnFile);

        cameraButton.setOnClickListener(v -> {
            dialog.dismiss();
            launchCameraCapture();
        });

        galleryButton.setOnClickListener(v -> {
            dialog.dismiss();
            pickImageLauncher.launch("image/*");
        });

        fileButton.setOnClickListener(v -> {
            dialog.dismiss();
            openDocumentLauncher.launch(new String[] {
                    "application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            });
        });

        dialog.show();
    }

    private void launchCameraCapture() {
        try {
            File cameraDir = new File(getCacheDir(), "camera");
            if (!cameraDir.exists() && !cameraDir.mkdirs()) {
                Toast.makeText(this, "Unable to open camera right now.", Toast.LENGTH_SHORT).show();
                return;
            }

            File imageFile = File.createTempFile("captured_", ".jpg", cameraDir);
            pendingCameraUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    imageFile);
            takePictureLauncher.launch(pendingCameraUri);
        } catch (IOException exception) {
            Toast.makeText(this, "Unable to open camera right now.", Toast.LENGTH_SHORT).show();
        }
    }

    private void processAttachment(Uri uri, String fallbackLabel) {
        String displayName = resolveDisplayName(uri);
        if (TextUtils.isEmpty(displayName)) {
            displayName = fallbackLabel;
        }

        String finalDisplayName = displayName;
        String userVisibleText = fallbackLabel + ": " + finalDisplayName;
        boolean shouldRefreshTitle = DEFAULT_CHAT_TITLE.equals(currentChatTitle);

        addChatMessage(new ChatMessage(userVisibleText, USER_ROLE));
        saveMessageToFirebase(new ChatMessage(userVisibleText, USER_ROLE), shouldRefreshTitle);
        addChatMessage(new ChatMessage("Typing...", BOT_ROLE));

        backgroundExecutor.execute(() -> {
            try {
                String extractedText = extractTextFromAttachment(uri);
                if (TextUtils.isEmpty(extractedText)) {
                    runOnUiThread(() -> {
                        removeTypingIndicator();
                        addAndPersistBotMessage("I could not extract readable text from that file. Please try another image or document.");
                    });
                    return;
                }

                requestSummaryFromGroq(extractedText, finalDisplayName);
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    removeTypingIndicator();
                    addAndPersistBotMessage("I could not process that attachment. Please use an image, PDF, or DOCX file.");
                });
            }
        });
    }

    private String extractTextFromAttachment(Uri uri) throws Exception {
        String mimeType = getContentResolver().getType(uri);
        String displayName = resolveDisplayName(uri);
        String lowerName = displayName == null ? "" : displayName.toLowerCase(Locale.ROOT);

        if ((mimeType != null && mimeType.startsWith("image/"))
                || lowerName.endsWith(".png")
                || lowerName.endsWith(".jpg")
                || lowerName.endsWith(".jpeg")
                || lowerName.endsWith(".webp")) {
            return AttachmentTextExtractor.extractImageText(this, uri);
        }

        if ("application/pdf".equals(mimeType) || lowerName.endsWith(".pdf")) {
            return AttachmentTextExtractor.extractPdfText(this, uri);
        }

        if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(mimeType)
                || lowerName.endsWith(".docx")) {
            return AttachmentTextExtractor.extractDocxText(this, uri);
        }

        throw new IllegalArgumentException("Unsupported file type");
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your note...");

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception exception) {
            Toast.makeText(this, "Voice not supported", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                etMessage.setText(result.get(0));
            }
        }
    }

    private void sendTypedContent(String text) {
        boolean shouldRefreshTitle = DEFAULT_CHAT_TITLE.equals(currentChatTitle);
        addChatMessage(new ChatMessage(text, USER_ROLE));
        saveMessageToFirebase(new ChatMessage(text, USER_ROLE), shouldRefreshTitle);
        addChatMessage(new ChatMessage("Typing...", BOT_ROLE));
        requestSummaryFromGroq(text, "text");
    }

    private void requestSummaryFromGroq(String rawContent, String sourceLabel) {
        JSONObject jsonBody = new JSONObject();

        try {
            jsonBody.put("model", "llama-3.3-70b-versatile");

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", buildSystemPrompt()));
            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", "Source: " + sourceLabel + "\n\nContent:\n" + trimContent(rawContent)));

            jsonBody.put("messages", messages);
        } catch (Exception exception) {
            runOnUiThread(() -> {
                removeTypingIndicator();
                addAndPersistBotMessage("I could not prepare the summary request.");
            });
            return;
        }

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(GROQ_URL)
                .header("Authorization", "Bearer " + GROQ_API_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    removeTypingIndicator();
                    addAndPersistBotMessage("Network error. Please try again.");
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        removeTypingIndicator();
                        addAndPersistBotMessage("Server error. Try again later.");
                    });
                    return;
                }

                try {
                    String data = response.body() != null ? response.body().string() : "";
                    String aiResponse = new JSONObject(data)
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    runOnUiThread(() -> {
                        removeTypingIndicator();
                        addAndPersistBotMessage(aiResponse);
                    });
                } catch (Exception exception) {
                    runOnUiThread(() -> {
                        removeTypingIndicator();
                        addAndPersistBotMessage("I could not read the AI response. Please try again.");
                    });
                }
            }
        });
    }

    private String buildSystemPrompt() {
        String styleInstruction;

        switch (TextUtils.isEmpty(currentSummaryMode) ? MODE_BRIEF : currentSummaryMode) {
            case MODE_SHORT:
                styleInstruction = "Create a very short summary in 2 to 3 crisp sentences.";
                break;
            case MODE_LONG:
                styleInstruction = "Create a detailed summary with clear paragraphs, preserving important context and details.";
                break;
            case MODE_BULLET:
                styleInstruction = "Create a bullet point summary with concise, meaningful points.";
                break;
            case MODE_BRIEF:
            default:
                styleInstruction = "Create a brief summary in one compact paragraph with the core meaning.";
                break;
        }

        return "You are an AI notes summarizer. " + styleInstruction
                + " Keep the answer accurate, readable, and directly based on the provided content."
                + " If the content looks incomplete, summarize only what is present.";
    }

    private String trimContent(String content) {
        String cleanContent = content == null ? "" : content.trim();
        if (cleanContent.length() <= MAX_CONTENT_LENGTH) {
            return cleanContent;
        }
        return cleanContent.substring(0, MAX_CONTENT_LENGTH) + "\n\n[Content truncated for summary]";
    }

    private void observeChatHistory() {
        chatHistoryListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<ChatSessionItem> sessions = buildChatHistory(snapshot);
                refreshDrawerLists(sessions);

                if (sessions.isEmpty()) {
                    if (TextUtils.isEmpty(currentChatId)) {
                        startNewChat();
                    }
                    return;
                }

                if (!TextUtils.isEmpty(requestedChatId)) {
                    ChatSessionItem target = chatSessionMap.get(requestedChatId);
                    requestedChatId = null;
                    if (target != null) {
                        loadChatSession(target);
                        return;
                    }
                }

                if (!TextUtils.isEmpty(currentChatId)) {
                    ChatSessionItem currentItem = chatSessionMap.get(currentChatId);
                    if (currentItem != null) {
                        currentChatTitle = currentItem.title;
                        currentSummaryMode = currentItem.summaryMode;
                        toolbarTitle.setText(currentItem.title);
                        return;
                    }
                }

                loadChatSession(sessions.get(0));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Unable to load chat history.", Toast.LENGTH_SHORT).show();
            }
        };

        dbRef.addValueEventListener(chatHistoryListener);
    }

    private List<ChatSessionItem> buildChatHistory(DataSnapshot snapshot) {
        List<ChatSessionItem> chatHistory = new ArrayList<>();
        chatSessionMap.clear();

        DataSnapshot sessionsSnapshot = snapshot.child(SESSIONS_NODE);
        for (DataSnapshot sessionSnapshot : sessionsSnapshot.getChildren()) {
            ChatSessionItem item = buildSessionEntry(sessionSnapshot, false);
            if (item != null) {
                chatHistory.add(item);
                chatSessionMap.put(item.chatId, item);
            }
        }

        for (DataSnapshot child : snapshot.getChildren()) {
            if (SESSIONS_NODE.equals(child.getKey())) {
                continue;
            }

            ChatSessionItem item = buildSessionEntry(child, true);
            if (item != null) {
                chatHistory.add(item);
                chatSessionMap.put(item.chatId, item);
            }
        }

        Collections.sort(chatHistory, (first, second) -> Long.compare(second.timestamp, first.timestamp));
        return chatHistory;
    }

    private ChatSessionItem buildSessionEntry(DataSnapshot snapshot, boolean legacy) {
        if (snapshot == null || snapshot.getKey() == null || !snapshot.hasChildren()) {
            return null;
        }

        String title;
        String lastMessage;
        long timestamp;
        boolean bookmarked = false;
        String summaryMode = "";

        if (legacy) {
            title = extractTitleFromMessages(snapshot);
            lastMessage = extractLastMessage(snapshot);
            timestamp = 0L;
        } else {
            ChatModel chatModel = snapshot.getValue(ChatModel.class);
            title = chatModel != null ? chatModel.getTitle() : null;
            lastMessage = chatModel != null ? chatModel.getLastMessage() : "";
            timestamp = chatModel != null ? chatModel.getTimestamp() : 0L;
            bookmarked = chatModel != null && chatModel.isBookmarked();
            summaryMode = chatModel != null ? chatModel.getSummaryMode() : "";

            if (TextUtils.isEmpty(title)) {
                title = extractTitleFromMessages(snapshot.child(MESSAGES_NODE));
            }
            if (TextUtils.isEmpty(lastMessage)) {
                lastMessage = extractLastMessage(snapshot.child(MESSAGES_NODE));
            }
        }

        if (TextUtils.isEmpty(title)) {
            title = DEFAULT_CHAT_TITLE;
        }

        return new ChatSessionItem(snapshot.getKey(), title, lastMessage, timestamp, bookmarked, legacy, summaryMode);
    }

    private void refreshDrawerLists(List<ChatSessionItem> sessions) {
        recentAdapter.submitList(sessions);
        recentEmptyText.setVisibility(sessions.isEmpty() ? View.VISIBLE : View.GONE);

        List<ChatSessionItem> savedNotes = new ArrayList<>();
        for (ChatSessionItem item : sessions) {
            if (item.bookmarked) {
                savedNotes.add(item);
            }
        }
        savedAdapter.submitList(savedNotes);
        savedEmptyText.setVisibility(savedNotes.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void loadChatSession(ChatSessionItem item) {
        if (item == null) {
            return;
        }

        currentChatId = item.chatId;
        currentChatIsLegacy = item.legacy;
        currentChatTitle = item.title;
        currentSummaryMode = item.summaryMode == null ? "" : item.summaryMode;
        toolbarTitle.setText(item.title);

        getMessagesReference(item.chatId, item.legacy)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        chatList.clear();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            ChatMessage chatMessage = child.getValue(ChatMessage.class);
                            if (chatMessage != null && !TextUtils.isEmpty(chatMessage.getMessage())) {
                                chatList.add(chatMessage);
                            }
                        }

                        if (chatList.isEmpty()) {
                            chatList.add(ChatMessage.createSuggestionMessage(WELCOME_PROMPT, SUMMARY_OPTIONS));
                        }

                        chatAdapter.notifyDataSetChanged();
                        recyclerView.scrollToPosition(Math.max(chatList.size() - 1, 0));
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(ChatActivity.this, "Unable to open this chat.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private DatabaseReference getMessagesReference(String chatId, boolean legacy) {
        if (legacy) {
            return dbRef.child(chatId);
        }

        return dbRef.child(SESSIONS_NODE)
                .child(chatId)
                .child(MESSAGES_NODE);
    }

    private void saveSessionMetadata(String chatId, String title, String lastMessage,
                                     long timestamp, boolean bookmarked, String summaryMode) {
        ChatModel chatModel = new ChatModel(chatId, title, lastMessage, timestamp, bookmarked, summaryMode);
        dbRef.child(SESSIONS_NODE)
                .child(chatId)
                .setValue(chatModel);
    }

    private void saveMessageToFirebase(ChatMessage message, boolean updateTitleFromMessage) {
        if (currentChatId == null || dbRef == null) {
            return;
        }

        getMessagesReference(currentChatId, currentChatIsLegacy)
                .push()
                .setValue(message);

        if (!currentChatIsLegacy) {
            long timestamp = System.currentTimeMillis();
            String titleToSave = updateTitleFromMessage ? buildChatTitle(message.getMessage()) : currentChatTitle;

            if (updateTitleFromMessage) {
                currentChatTitle = titleToSave;
                toolbarTitle.setText(titleToSave);
            }

            updateSessionMetadata(currentChatId, titleToSave, message.getMessage(), timestamp,
                    getCurrentBookmarkState(), currentSummaryMode);
        }
    }

    private void updateSessionMetadata(String chatId, String title, String lastMessage,
                                       long timestamp, boolean bookmarked, String summaryMode) {
        saveSessionMetadata(chatId, title, lastMessage, timestamp, bookmarked, summaryMode);
    }

    private boolean getCurrentBookmarkState() {
        ChatSessionItem item = chatSessionMap.get(currentChatId);
        return item != null && item.bookmarked;
    }

    private void addChatMessage(ChatMessage message) {
        runOnUiThread(() -> {
            chatList.add(message);
            chatAdapter.notifyItemInserted(chatList.size() - 1);
            recyclerView.smoothScrollToPosition(chatList.size() - 1);
        });
    }

    private void addAndPersistBotMessage(String message) {
        ChatMessage chatMessage = new ChatMessage(message, BOT_ROLE);
        addChatMessage(chatMessage);
        saveMessageToFirebase(chatMessage, false);
    }

    private void removeTypingIndicator() {
        if (!chatList.isEmpty()) {
            int lastIndex = chatList.size() - 1;
            if ("Typing...".equals(chatList.get(lastIndex).getMessage())) {
                chatList.remove(lastIndex);
                chatAdapter.notifyItemRemoved(lastIndex);
            }
        }
    }

    private String buildChatTitle(String message) {
        if (TextUtils.isEmpty(message)) {
            return DEFAULT_CHAT_TITLE;
        }

        String cleanMessage = message.trim().replaceAll("\\s+", " ");
        if (cleanMessage.length() <= MAX_TITLE_LENGTH) {
            return cleanMessage;
        }

        return cleanMessage.substring(0, MAX_TITLE_LENGTH).trim() + "...";
    }

    private String extractTitleFromMessages(DataSnapshot messagesSnapshot) {
        for (DataSnapshot child : messagesSnapshot.getChildren()) {
            ChatMessage chatMessage = child.getValue(ChatMessage.class);
            if (chatMessage != null
                    && chatMessage.isUser()
                    && !TextUtils.isEmpty(chatMessage.getMessage())
                    && !SUMMARY_OPTIONS.contains(chatMessage.getMessage())) {
                return buildChatTitle(chatMessage.getMessage());
            }
        }

        for (DataSnapshot child : messagesSnapshot.getChildren()) {
            ChatMessage chatMessage = child.getValue(ChatMessage.class);
            if (chatMessage != null && !TextUtils.isEmpty(chatMessage.getMessage())) {
                return buildChatTitle(chatMessage.getMessage());
            }
        }

        return DEFAULT_CHAT_TITLE;
    }

    private String extractLastMessage(DataSnapshot messagesSnapshot) {
        String lastMessage = "";
        for (DataSnapshot child : messagesSnapshot.getChildren()) {
            ChatMessage chatMessage = child.getValue(ChatMessage.class);
            if (chatMessage != null && !TextUtils.isEmpty(chatMessage.getMessage())) {
                lastMessage = chatMessage.getMessage();
            }
        }
        return lastMessage;
    }

    private String resolveDisplayName(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            return null;
        }

        try {
            int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (index >= 0 && cursor.moveToFirst()) {
                return cursor.getString(index);
            }
        } finally {
            cursor.close();
        }

        return null;
    }

    @Override
    public void onSuggestionSelected(String suggestion) {
        currentSummaryMode = suggestion;
        updateSessionMetadata(currentChatId, currentChatTitle, getCurrentLastMessage(),
                System.currentTimeMillis(), getCurrentBookmarkState(), currentSummaryMode);

        ChatMessage userMessage = new ChatMessage(suggestion, USER_ROLE);
        addChatMessage(userMessage);
        saveMessageToFirebase(userMessage, false);

        addAndPersistBotMessage("Perfect. Send me your text, image, or document and I will create a "
                + suggestion.toLowerCase(Locale.ROOT) + " for you.");
    }

    private String getCurrentLastMessage() {
        if (chatList.isEmpty()) {
            return "";
        }
        return chatList.get(chatList.size() - 1).getMessage();
    }

    @Override
    public void onCopyMessage(String message) {
        ClipboardManager clipboardManager =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(ClipData.newPlainText("AI Summary", message));
            Toast.makeText(this, "Summary copied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onListenMessage(String message) {
        if (!isTextToSpeechReady) {
            Toast.makeText(this, "Voice is not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }

        textToSpeech.stop();
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "summary_voice");
    }

    @Override
    public void onChatSelected(ChatSessionItem chatSessionItem) {
        drawerLayout.closeDrawer(GravityCompat.START);
        loadChatSession(chatSessionItem);
    }

    @Override
    public void onDeleteRequested(ChatSessionItem chatSessionItem) {
        drawerLayout.closeDrawer(GravityCompat.START);
        new AlertDialog.Builder(this)
                .setTitle("Delete Chat")
                .setMessage("Do you really want to delete '" + chatSessionItem.title + "'?")
                .setPositiveButton("Yes", (dialog, which) -> deleteChat(chatSessionItem))
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onSaveToggleRequested(ChatSessionItem chatSessionItem) {
        boolean newBookmarkState = !chatSessionItem.bookmarked;
        updateSessionMetadata(chatSessionItem.chatId, chatSessionItem.title, chatSessionItem.lastMessage,
                chatSessionItem.timestamp == 0L ? System.currentTimeMillis() : chatSessionItem.timestamp,
                newBookmarkState, chatSessionItem.summaryMode);
        Toast.makeText(this, newBookmarkState ? "Saved to notes" : "Removed from saved notes",
                Toast.LENGTH_SHORT).show();
    }

    private void deleteChat(ChatSessionItem item) {
        DatabaseReference targetReference = item.legacy
                ? dbRef.child(item.chatId)
                : dbRef.child(SESSIONS_NODE).child(item.chatId);

        targetReference.removeValue((error, ref) -> {
            if (error != null) {
                Toast.makeText(this, "Unable to delete chat right now.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (item.chatId.equals(currentChatId)) {
                currentChatId = null;
                currentChatTitle = DEFAULT_CHAT_TITLE;
                currentSummaryMode = "";
            }
            Toast.makeText(this, "Chat deleted", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.getDefault());
            isTextToSpeechReady = result != TextToSpeech.LANG_MISSING_DATA
                    && result != TextToSpeech.LANG_NOT_SUPPORTED;
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (dbRef != null && chatHistoryListener != null) {
            dbRef.removeEventListener(chatHistoryListener);
        }

        backgroundExecutor.shutdownNow();

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    public static class ChatSessionItem {
        public final String chatId;
        public final String title;
        public final String lastMessage;
        public final long timestamp;
        public final boolean bookmarked;
        public final boolean legacy;
        public final String summaryMode;

        public ChatSessionItem(String chatId, String title, String lastMessage, long timestamp,
                               boolean bookmarked, boolean legacy, String summaryMode) {
            this.chatId = chatId;
            this.title = title;
            this.lastMessage = lastMessage == null ? "" : lastMessage;
            this.timestamp = timestamp;
            this.bookmarked = bookmarked;
            this.legacy = legacy;
            this.summaryMode = summaryMode == null ? "" : summaryMode;
        }
    }
}
