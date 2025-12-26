package com.fastkeyboard;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.ScrollView;

public class SettingsActivity extends Activity {
    private static final String PREFS_NAME = "FastKeyboardPrefs";
    private static final String KEY_API_URL = "whisper_api_url";
    private static final String KEY_API_KEY = "whisper_api_key";

    private EditText apiUrlInput;
    private EditText apiKeyInput;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Create main scrollable layout
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.parseColor("#1E1E1E"));
        scrollView.setFillViewport(true);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(40, 40, 40, 40);
        mainLayout.setBackgroundColor(Color.parseColor("#1E1E1E"));

        // Title
        TextView title = new TextView(this);
        title.setText("FastKeyboard Settings");
        title.setTextSize(24);
        title.setTextColor(Color.WHITE);
        title.setPadding(0, 0, 0, 30);
        title.setGravity(Gravity.CENTER);
        mainLayout.addView(title);

        // Whisper API Section
        TextView sectionTitle = new TextView(this);
        sectionTitle.setText("Whisper API Configuration");
        sectionTitle.setTextSize(18);
        sectionTitle.setTextColor(Color.parseColor("#4CAF50"));
        sectionTitle.setPadding(0, 20, 0, 10);
        mainLayout.addView(sectionTitle);

        // API URL Label
        TextView urlLabel = new TextView(this);
        urlLabel.setText("API URL:");
        urlLabel.setTextSize(14);
        urlLabel.setTextColor(Color.WHITE);
        urlLabel.setPadding(0, 10, 0, 5);
        mainLayout.addView(urlLabel);

        // API URL Input
        apiUrlInput = new EditText(this);
        apiUrlInput.setHint("https://api.openai.com/v1/audio/transcriptions");
        apiUrlInput.setTextSize(12);
        apiUrlInput.setTextColor(Color.WHITE);
        apiUrlInput.setHintTextColor(Color.GRAY);
        apiUrlInput.setBackgroundColor(Color.parseColor("#2C2C2C"));
        apiUrlInput.setPadding(20, 20, 20, 20);
        apiUrlInput.setSingleLine(true);
        String savedUrl = prefs.getString(KEY_API_URL, "");
        if (!savedUrl.isEmpty()) {
            apiUrlInput.setText(savedUrl);
        }
        mainLayout.addView(apiUrlInput);

        // API Key Label
        TextView keyLabel = new TextView(this);
        keyLabel.setText("API Key:");
        keyLabel.setTextSize(14);
        keyLabel.setTextColor(Color.WHITE);
        keyLabel.setPadding(0, 20, 0, 5);
        mainLayout.addView(keyLabel);

        // API Key Input
        apiKeyInput = new EditText(this);
        apiKeyInput.setHint("sk-proj-...");
        apiKeyInput.setTextSize(12);
        apiKeyInput.setTextColor(Color.WHITE);
        apiKeyInput.setHintTextColor(Color.GRAY);
        apiKeyInput.setBackgroundColor(Color.parseColor("#2C2C2C"));
        apiKeyInput.setPadding(20, 20, 20, 20);
        apiKeyInput.setSingleLine(true);
        String savedKey = prefs.getString(KEY_API_KEY, "");
        if (!savedKey.isEmpty()) {
            apiKeyInput.setText(savedKey);
        }
        mainLayout.addView(apiKeyInput);

        // Help text
        TextView helpText = new TextView(this);
        helpText.setText("\nHow to get API key:\n" +
            "1. OpenAI: https://platform.openai.com/api-keys\n" +
            "2. Create new secret key\n" +
            "3. Copy and paste above\n\n" +
            "Default URL for OpenAI:\n" +
            "https://api.openai.com/v1/audio/transcriptions");
        helpText.setTextSize(12);
        helpText.setTextColor(Color.parseColor("#AAAAAA"));
        helpText.setPadding(0, 20, 0, 20);
        mainLayout.addView(helpText);

        // Save Button
        Button saveButton = new Button(this);
        saveButton.setText("Save Settings");
        saveButton.setTextColor(Color.WHITE);
        saveButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        saveButton.setPadding(0, 30, 0, 30);
        saveButton.setTextSize(16);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        saveParams.setMargins(0, 20, 0, 10);
        saveButton.setLayoutParams(saveParams);
        saveButton.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                saveSettings();
            }
        });
        mainLayout.addView(saveButton);

        // Test Button
        Button testButton = new Button(this);
        testButton.setText("Test Configuration");
        testButton.setTextColor(Color.WHITE);
        testButton.setBackgroundColor(Color.parseColor("#2196F3"));
        testButton.setPadding(0, 30, 0, 30);
        testButton.setTextSize(16);
        LinearLayout.LayoutParams testParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        testButton.setLayoutParams(testParams);
        testButton.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                testConfiguration();
            }
        });
        mainLayout.addView(testButton);

        // Status text
        TextView statusText = new TextView(this);
        String status = savedUrl.isEmpty() || savedKey.isEmpty()
            ? "⚠️ Not configured - voice typing won't work"
            : "✅ Configured - voice typing ready!";
        statusText.setText("\n" + status);
        statusText.setTextSize(14);
        statusText.setTextColor(savedUrl.isEmpty() || savedKey.isEmpty()
            ? Color.parseColor("#FF9800")
            : Color.parseColor("#4CAF50"));
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 20, 0, 0);
        mainLayout.addView(statusText);

        scrollView.addView(mainLayout);
        setContentView(scrollView);
    }

    private void saveSettings() {
        String url = apiUrlInput.getText().toString().trim();
        String key = apiKeyInput.getText().toString().trim();

        if (url.isEmpty() || key.isEmpty()) {
            Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_API_URL, url);
        editor.putString(KEY_API_KEY, key);
        editor.apply();

        Toast.makeText(this, "Settings saved! Voice typing is now configured.", Toast.LENGTH_LONG).show();
    }

    private void testConfiguration() {
        String url = apiUrlInput.getText().toString().trim();
        String key = apiKeyInput.getText().toString().trim();

        if (url.isEmpty() || key.isEmpty()) {
            Toast.makeText(this, "Please fill in both fields first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!url.contains("/v1/audio/transcriptions")) {
            Toast.makeText(this, "Warning: URL should contain /v1/audio/transcriptions", Toast.LENGTH_LONG).show();
        } else if (!key.startsWith("sk-")) {
            Toast.makeText(this, "Warning: OpenAI keys usually start with 'sk-'", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Configuration looks good! Save and try voice typing.", Toast.LENGTH_LONG).show();
        }
    }

    public static String getApiUrl(Activity context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_API_URL, "");
    }

    public static String getApiKey(Activity context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_API_KEY, "");
    }
}
