package com.fastkeyboard;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private static final String PREFS_NAME = "VoiceKeyboardPrefs";
    private static final String KEY_API_URL = "whisper_api_url";
    private static final String KEY_API_KEY = "whisper_api_key";
    private static final String KEY_TRANSCRIPTION_PROMPT = "transcription_prompt";
    private static final String KEY_AUDIO_QUALITY = "audio_quality";
    private static final String KEY_WHISPER_MODEL = "whisper_model";

    private EditText urlInput;
    private EditText keyInput;
    private EditText transcriptionPromptInput;
    private Spinner qualitySpinner;
    private Spinner modelSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create main scrollable layout
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.parseColor("#1E1E1E"));
        scrollView.setFillViewport(true);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(24, 24, 24, 24);
        mainLayout.setBackgroundColor(Color.parseColor("#1E1E1E"));

        // Title
        TextView title = new TextView(this);
        title.setText("⚙️ VoiceKeyboard Settings");
        title.setTextSize(24);
        title.setTextColor(Color.WHITE);
        title.setPadding(0, 0, 0, 24);
        title.setGravity(Gravity.CENTER);
        mainLayout.addView(title);

        // API Settings Card
        LinearLayout apiCard = createCard("🔑 API Configuration");

        TextView urlLabel = new TextView(this);
        urlLabel.setText("OpenAI API URL");
        urlLabel.setTextSize(14);
        urlLabel.setTextColor(Color.parseColor("#CCCCCC"));
        urlLabel.setPadding(0, 0, 0, 8);
        apiCard.addView(urlLabel);

        urlInput = new EditText(this);
        urlInput.setHint("https://api.openai.com/v1/audio/transcriptions");
        urlInput.setTextSize(12);
        urlInput.setTextColor(Color.WHITE);
        urlInput.setHintTextColor(Color.parseColor("#666666"));
        urlInput.setBackgroundColor(Color.parseColor("#2C2C2C"));
        urlInput.setPadding(16, 16, 16, 16);
        urlInput.setSingleLine(true);
        apiCard.addView(urlInput);

        addVerticalSpace(apiCard, 16);

        TextView keyLabel = new TextView(this);
        keyLabel.setText("OpenAI API Key");
        keyLabel.setTextSize(14);
        keyLabel.setTextColor(Color.parseColor("#CCCCCC"));
        keyLabel.setPadding(0, 0, 0, 8);
        apiCard.addView(keyLabel);

        keyInput = new EditText(this);
        keyInput.setHint("sk-...");
        keyInput.setTextSize(12);
        keyInput.setTextColor(Color.WHITE);
        keyInput.setHintTextColor(Color.parseColor("#666666"));
        keyInput.setBackgroundColor(Color.parseColor("#2C2C2C"));
        keyInput.setPadding(16, 16, 16, 16);
        keyInput.setSingleLine(true);
        apiCard.addView(keyInput);

        addVerticalSpace(apiCard, 16);

        Button saveApiBtn = createButton("💾 Save API Settings", "#4CAF50");
        saveApiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });
        apiCard.addView(saveApiBtn);

        mainLayout.addView(apiCard);

        // Transcription Settings Card
        LinearLayout transcriptionCard = createCard("🎙️ Transcription Settings");

        TextView promptLabel = new TextView(this);
        promptLabel.setText("Transcription Instructions");
        promptLabel.setTextSize(14);
        promptLabel.setTextColor(Color.parseColor("#CCCCCC"));
        promptLabel.setPadding(0, 0, 0, 8);
        transcriptionCard.addView(promptLabel);

        transcriptionPromptInput = new EditText(this);
        transcriptionPromptInput.setHint("Punctuate and then grammatically correct and improve the given recorded audio");
        transcriptionPromptInput.setTextSize(12);
        transcriptionPromptInput.setTextColor(Color.WHITE);
        transcriptionPromptInput.setHintTextColor(Color.parseColor("#666666"));
        transcriptionPromptInput.setBackgroundColor(Color.parseColor("#2C2C2C"));
        transcriptionPromptInput.setPadding(16, 16, 16, 16);
        transcriptionPromptInput.setMinLines(3);
        transcriptionPromptInput.setMaxLines(5);
        transcriptionCard.addView(transcriptionPromptInput);

        addVerticalSpace(transcriptionCard, 16);

        TextView qualityLabel = new TextView(this);
        qualityLabel.setText("Audio Quality");
        qualityLabel.setTextSize(14);
        qualityLabel.setTextColor(Color.parseColor("#CCCCCC"));
        qualityLabel.setPadding(0, 0, 0, 8);
        transcriptionCard.addView(qualityLabel);

        qualitySpinner = createSpinner(new String[]{
            "Low (16kHz, Mono, 128kbps)",
            "Medium (22kHz, Mono, 192kbps)",
            "High (44kHz, Stereo, 256kbps)"
        });
        transcriptionCard.addView(qualitySpinner);

        addVerticalSpace(transcriptionCard, 16);

        TextView modelLabel = new TextView(this);
        modelLabel.setText("Whisper Model");
        modelLabel.setTextSize(14);
        modelLabel.setTextColor(Color.parseColor("#CCCCCC"));
        modelLabel.setPadding(0, 0, 0, 8);
        transcriptionCard.addView(modelLabel);

        modelSpinner = createSpinner(new String[]{
            "whisper-1",
            "gpt-4o-audio-preview",
            "gpt-4o-mini-audio-preview"
        });
        transcriptionCard.addView(modelSpinner);

        addVerticalSpace(transcriptionCard, 16);

        Button saveTranscriptionBtn = createButton("💾 Save Transcription Settings", "#2196F3");
        saveTranscriptionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTranscriptionSettings();
            }
        });
        transcriptionCard.addView(saveTranscriptionBtn);

        mainLayout.addView(transcriptionCard);

        // Info Card
        LinearLayout infoCard = createCard("ℹ️ Information");

        TextView infoText = new TextView(this);
        infoText.setText("VoiceKeyboard - Voice-only keyboard\n\n" +
                "Features:\n" +
                "• Voice recording with Whisper AI\n" +
                "• Text improvement via ChatGPT\n" +
                "• Voice edit mode\n" +
                "• Recording history with search\n" +
                "• Multiple quality settings\n" +
                "• Custom transcription prompts\n\n" +
                "Get your API key from:\n" +
                "https://platform.openai.com/api-keys");
        infoText.setTextSize(12);
        infoText.setTextColor(Color.parseColor("#AAAAAA"));
        infoText.setLineSpacing(4, 1);
        infoCard.addView(infoText);

        mainLayout.addView(infoCard);

        // History button
        Button historyBtn = createButton("📜 View History", "#00BCD4");
        historyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.content.Intent intent = new android.content.Intent(SettingsActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });
        LinearLayout.LayoutParams historyParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        historyParams.setMargins(0, 24, 0, 0);
        historyBtn.setLayoutParams(historyParams);
        mainLayout.addView(historyBtn);

        // Templates button
        Button templatesBtn = createButton("📝 Manage Templates", "#FFC107");
        templatesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.content.Intent intent = new android.content.Intent(SettingsActivity.this, TemplatesActivity.class);
                startActivity(intent);
            }
        });
        LinearLayout.LayoutParams templatesParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        templatesParams.setMargins(0, 12, 0, 0);
        templatesBtn.setLayoutParams(templatesParams);
        mainLayout.addView(templatesBtn);

        // Close button
        Button closeBtn = createButton("Close", "#607D8B");
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        closeParams.setMargins(0, 12, 0, 0);
        closeBtn.setLayoutParams(closeParams);
        mainLayout.addView(closeBtn);

        scrollView.addView(mainLayout);
        setContentView(scrollView);

        // Load saved settings
        loadSettings();
    }

    private LinearLayout createCard(String title) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#252525"));
        card.setPadding(20, 20, 20, 20);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor("#252525"));
        drawable.setCornerRadius(12);
        card.setBackground(drawable);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 16);
        card.setLayoutParams(cardParams);

        TextView cardTitle = new TextView(this);
        cardTitle.setText(title);
        cardTitle.setTextSize(16);
        cardTitle.setTextColor(Color.WHITE);
        cardTitle.setPadding(0, 0, 0, 16);
        card.addView(cardTitle);

        return card;
    }

    private Button createButton(String text, String colorHex) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setPadding(24, 16, 24, 16);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor(colorHex));
        drawable.setCornerRadius(8);
        button.setBackground(drawable);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        button.setLayoutParams(params);

        return button;
    }

    private Spinner createSpinner(String[] items) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setPadding(16, 16, 16, 16);
        spinner.setBackgroundColor(Color.parseColor("#2C2C2C"));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        spinner.setLayoutParams(params);

        return spinner;
    }

    private void addVerticalSpace(LinearLayout parent, int dp) {
        View space = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (int) (dp * getResources().getDisplayMetrics().density)
        );
        space.setLayoutParams(params);
        parent.addView(space);
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String url = prefs.getString(KEY_API_URL, "");
        String key = prefs.getString(KEY_API_KEY, "");
        String prompt = prefs.getString(KEY_TRANSCRIPTION_PROMPT, "Punctuate and then grammatically correct and improve the given recorded audio");
        String quality = prefs.getString(KEY_AUDIO_QUALITY, "Low");
        String model = prefs.getString(KEY_WHISPER_MODEL, "whisper-1");

        urlInput.setText(url);
        keyInput.setText(key);
        transcriptionPromptInput.setText(prompt);

        // Set quality spinner
        if (quality.equals("Medium")) {
            qualitySpinner.setSelection(1);
        } else if (quality.equals("High")) {
            qualitySpinner.setSelection(2);
        } else {
            qualitySpinner.setSelection(0);
        }

        // Set model spinner
        if (model.equals("gpt-4o-audio-preview")) {
            modelSpinner.setSelection(1);
        } else if (model.equals("gpt-4o-mini-audio-preview")) {
            modelSpinner.setSelection(2);
        } else {
            modelSpinner.setSelection(0);
        }
    }

    private void saveSettings() {
        String url = urlInput.getText().toString().trim();
        String key = keyInput.getText().toString().trim();

        if (url.isEmpty() || key.isEmpty()) {
            Toast.makeText(this, "Please fill in API fields", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_API_URL, url);
        editor.putString(KEY_API_KEY, key);
        editor.apply();

        Toast.makeText(this, "API settings saved!", Toast.LENGTH_SHORT).show();
    }

    private void saveTranscriptionSettings() {
        String prompt = transcriptionPromptInput.getText().toString().trim();
        String quality = qualitySpinner.getSelectedItem().toString();
        String model = modelSpinner.getSelectedItem().toString();

        // Parse quality
        String qualityKey = "Low";
        if (quality.startsWith("Medium")) {
            qualityKey = "Medium";
        } else if (quality.startsWith("High")) {
            qualityKey = "High";
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_TRANSCRIPTION_PROMPT, prompt);
        editor.putString(KEY_AUDIO_QUALITY, qualityKey);
        editor.putString(KEY_WHISPER_MODEL, model);
        editor.apply();

        Toast.makeText(this, "Transcription settings saved!", Toast.LENGTH_SHORT).show();
    }
}
