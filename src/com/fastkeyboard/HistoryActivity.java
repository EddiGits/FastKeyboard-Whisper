package com.fastkeyboard;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class HistoryActivity extends Activity {
    private static final String PREFS_NAME = "VoiceKeyboardPrefs";
    private static final String KEY_HISTORY = "transcription_history";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create main scrollable layout
        ScrollView scrollView = new ScrollView(this);
        // Glassmorphism gradient background
        GradientDrawable scrollBg = new GradientDrawable();
        scrollBg.setColors(new int[]{
            Color.parseColor("#1A1A2E"),
            Color.parseColor("#16213E")
        });
        scrollBg.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        scrollBg.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        scrollView.setBackground(scrollBg);
        scrollView.setFillViewport(true);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(24, 24, 24, 24);

        // Title with glow
        TextView title = new TextView(this);
        title.setText("📜 Recording History");
        title.setTextSize(24);
        title.setTextColor(Color.WHITE);
        title.setPadding(0, 0, 0, 24);
        title.setGravity(Gravity.CENTER);
        title.setShadowLayer(12, 0, 0, Color.parseColor("#66FFFFFF"));
        mainLayout.addView(title);

        // Search box with glassmorphism
        final EditText searchBox = new EditText(this);
        searchBox.setHint("🔍 Search history...");
        searchBox.setTextColor(Color.WHITE);
        searchBox.setHintTextColor(Color.parseColor("#888888"));
        searchBox.setPadding(16, 16, 16, 16);
        searchBox.setTextSize(14);
        searchBox.setSingleLine(true);
        // Glassmorphism input field
        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(Color.parseColor("#33FFFFFF"));
        searchBg.setCornerRadius((int) (8 * getResources().getDisplayMetrics().density));
        searchBg.setStroke((int) (1 * getResources().getDisplayMetrics().density), Color.parseColor("#55FFFFFF"));
        searchBox.setBackground(searchBg);
        mainLayout.addView(searchBox);

        addVerticalSpace(mainLayout, 16);

        // History list container
        final ScrollView historyScroll = new ScrollView(this);
        historyScroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1.0f
        ));

        final LinearLayout historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        historyScroll.addView(historyContainer);

        // Load and display history
        final Runnable loadHistory = new Runnable() {
            @Override
            public void run() {
                historyContainer.removeAllViews();

                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                String history = prefs.getString(KEY_HISTORY, "");

                if (history.isEmpty()) {
                    TextView emptyText = new TextView(HistoryActivity.this);
                    emptyText.setText("No history yet");
                    emptyText.setTextColor(Color.parseColor("#666666"));
                    emptyText.setGravity(Gravity.CENTER);
                    emptyText.setPadding(0, 40, 0, 0);
                    historyContainer.addView(emptyText);
                    return;
                }

                String searchQuery = searchBox.getText().toString().toLowerCase();
                String[] entries = history.split("\n\n===ENTRY===\n\n");

                for (int i = entries.length - 1; i >= 0; i--) {
                    String entry = entries[i];
                    if (entry.trim().isEmpty()) continue;

                    String[] parts = entry.split("\\|\\|\\|");
                    if (parts.length < 2) continue;

                    String timestamp = parts[0];
                    final String text = parts[1];

                    if (!searchQuery.isEmpty() && !text.toLowerCase().contains(searchQuery)) {
                        continue;
                    }

                    addHistoryEntry(historyContainer, timestamp, text, i);
                }
            }
        };

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadHistory.run();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        mainLayout.addView(historyScroll);

        addVerticalSpace(mainLayout, 16);

        // Buttons
        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.CENTER);

        Button clearAllBtn = createButton("🗑 Clear All", "#f44336");
        clearAllBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                prefs.edit().putString(KEY_HISTORY, "").apply();
                Toast.makeText(HistoryActivity.this, "History cleared", Toast.LENGTH_SHORT).show();
                loadHistory.run();
            }
        });
        buttonRow.addView(clearAllBtn);

        addSpace(buttonRow, 16);

        Button closeBtn = createButton("Close", "#607D8B");
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        buttonRow.addView(closeBtn);

        mainLayout.addView(buttonRow);

        scrollView.addView(mainLayout);
        setContentView(scrollView);

        loadHistory.run();
    }

    private void addHistoryEntry(LinearLayout container, String timestamp, final String text, final int index) {
        LinearLayout entryLayout = new LinearLayout(this);
        entryLayout.setOrientation(LinearLayout.VERTICAL);
        entryLayout.setPadding(20, 20, 20, 20);
        entryLayout.setElevation((int) (4 * getResources().getDisplayMetrics().density));

        // Glassmorphism card with gradient
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColors(new int[]{
            Color.parseColor("#CC2A2A4E"),
            Color.parseColor("#CC1F1F3E")
        });
        drawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        drawable.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        drawable.setCornerRadius((int) (16 * getResources().getDisplayMetrics().density));
        drawable.setStroke((int) (1 * getResources().getDisplayMetrics().density), Color.parseColor("#44FFFFFF"));
        entryLayout.setBackground(drawable);

        LinearLayout.LayoutParams entryParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        entryParams.setMargins(0, 0, 0, 16);
        entryLayout.setLayoutParams(entryParams);

        // Timestamp
        TextView timeText = new TextView(this);
        timeText.setText(timestamp);
        timeText.setTextColor(Color.parseColor("#00BCD4"));
        timeText.setTextSize(12);
        entryLayout.addView(timeText);

        // Text content
        TextView contentText = new TextView(this);
        contentText.setText(text);
        contentText.setTextColor(Color.WHITE);
        contentText.setTextSize(14);
        contentText.setPadding(0, 8, 0, 12);
        entryLayout.addView(contentText);

        // Buttons
        LinearLayout buttonsRow = new LinearLayout(this);
        buttonsRow.setOrientation(LinearLayout.HORIZONTAL);

        Button copyBtn = createButton("📋 Copy", "#4CAF50");
        copyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("transcription", text);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(HistoryActivity.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });
        buttonsRow.addView(copyBtn);

        addSpace(buttonsRow, 8);

        Button improveBtn = createButton("✨ Improve", "#FFC107");
        improveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                improveText(text, improveBtn);
            }
        });
        buttonsRow.addView(improveBtn);

        addSpace(buttonsRow, 8);

        Button deleteBtn = createButton("🗑 Delete", "#f44336");
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteHistoryEntry(index);
                recreate(); // Reload activity
            }
        });
        buttonsRow.addView(deleteBtn);

        entryLayout.addView(buttonsRow);
        container.addView(entryLayout);
    }

    private void improveText(final String originalText, final Button button) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String apiKey = prefs.getString("whisper_api_key", "");

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "API key not configured", Toast.LENGTH_SHORT).show();
            return;
        }

        button.setEnabled(false);
        button.setText("⏳ Improving...");

        ChatGPTAPI.improveText(this, apiKey, originalText, new ChatGPTAPI.ChatGPTCallback() {
            @Override
            public void onSuccess(final String improvedText) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        button.setEnabled(true);
                        button.setText("✨ Improve");

                        // Copy improved text to clipboard
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData.newPlainText("improved", improvedText);
                        clipboard.setPrimaryClip(clip);

                        Toast.makeText(HistoryActivity.this, "Improved text copied to clipboard!", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        button.setEnabled(true);
                        button.setText("✨ Improve");
                        Toast.makeText(HistoryActivity.this, "Improvement failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void deleteHistoryEntry(int index) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String history = prefs.getString(KEY_HISTORY, "");

        String[] entries = history.split("\n\n===ENTRY===\n\n");
        StringBuilder newHistory = new StringBuilder();

        for (int i = 0; i < entries.length; i++) {
            if (i != index && !entries[i].trim().isEmpty()) {
                if (newHistory.length() > 0) {
                    newHistory.append("\n\n===ENTRY===\n\n");
                }
                newHistory.append(entries[i]);
            }
        }

        prefs.edit().putString(KEY_HISTORY, newHistory.toString()).apply();
        Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show();
    }

    private Button createButton(String text, String colorHex) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setPadding(24, 16, 24, 16);
        button.setElevation((int) (4 * getResources().getDisplayMetrics().density));

        // Gradient button background
        GradientDrawable drawable = new GradientDrawable();
        int baseColor = Color.parseColor(colorHex);
        int lighterColor = lightenColor(baseColor, 0.2f);
        drawable.setColors(new int[]{lighterColor, baseColor});
        drawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        drawable.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        drawable.setCornerRadius((int) (12 * getResources().getDisplayMetrics().density));
        drawable.setStroke((int) (1 * getResources().getDisplayMetrics().density), Color.parseColor("#44FFFFFF"));
        button.setBackground(drawable);

        // Add press animation
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                            .setInterpolator(new AccelerateDecelerateInterpolator()).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100)
                            .setInterpolator(new AccelerateDecelerateInterpolator()).start();
                        break;
                }
                return false;
            }
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        button.setLayoutParams(params);

        return button;
    }

    private int lightenColor(int color, float factor) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        red = Math.min(255, (int)(red + (255 - red) * factor));
        green = Math.min(255, (int)(green + (255 - green) * factor));
        blue = Math.min(255, (int)(blue + (255 - blue) * factor));

        return Color.rgb(red, green, blue);
    }

    private void addSpace(LinearLayout parent, int dp) {
        View space = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            (int) (dp * getResources().getDisplayMetrics().density),
            0
        );
        space.setLayoutParams(params);
        parent.addView(space);
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
}
