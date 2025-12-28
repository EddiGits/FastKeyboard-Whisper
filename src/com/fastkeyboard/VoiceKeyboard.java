package com.fastkeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VoiceKeyboard extends InputMethodService {
    private static final String PREFS_NAME = "VoiceKeyboardPrefs";
    private static final String KEY_HISTORY = "transcription_history";
    private static final String KEY_AUDIO_QUALITY = "audio_quality";

    private LinearLayout mainLayout;
    private TextView statusText;
    private ProgressBar processingIndicator;
    private Button recordBtn;
    private Button stopBtn;
    private Button cancelBtn;
    private Button pauseBtn;
    private View amplitudeIndicator;

    private AudioRecorder audioRecorder;
    private boolean isRecording = false;
    private boolean isPaused = false;
    private Handler mainHandler;
    private long recordingStartTime = 0;
    private long pausedTime = 0;
    private Runnable timerRunnable;
    private Runnable amplitudeRunnable;
    private android.os.Vibrator vibrator;

    @Override
    public void onCreate() {
        super.onCreate();
        audioRecorder = new AudioRecorder();

        // Load and apply audio quality settings
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String audioQuality = prefs.getString(KEY_AUDIO_QUALITY, "Low");
        audioRecorder.setQuality(audioQuality);

        mainHandler = new Handler(Looper.getMainLooper());

        // Initialize vibrator
        vibrator = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Initialize timer runnable
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording && !audioRecorder.isRecording()) {
                    return;
                }
                long seconds = (System.currentTimeMillis() - recordingStartTime) / 1000;
                statusText.setText("🔴 Recording... " + formatTime(seconds));
                mainHandler.postDelayed(this, 1000);
            }
        };

        // Initialize amplitude indicator runnable
        amplitudeRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording && !isPaused && audioRecorder.isRecording()) {
                    int amplitude = audioRecorder.getMaxAmplitude();
                    updateAmplitudeIndicator(amplitude);
                    mainHandler.postDelayed(this, 100); // Update 10 times per second
                }
            }
        };
    }

    @Override
    public View onCreateInputView() {
        // Create main container with dark theme
        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#1E1E1E"));
        mainLayout.setPadding(16, 16, 16, 16);
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dpToPx(280) // Height for voice keyboard with enhancement features
        ));

        createEditorView();
        return mainLayout;
    }

    private void createEditorView() {
        mainLayout.removeAllViews();

        // Top buttons row: Settings, History, Backspace
        LinearLayout topButtonRow = new LinearLayout(this);
        topButtonRow.setOrientation(LinearLayout.HORIZONTAL);
        topButtonRow.setGravity(Gravity.CENTER);

        Button settingsBtn = createButton("⚙ Settings", "#9C27B0");
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings();
            }
        });
        topButtonRow.addView(settingsBtn);

        addSpace(topButtonRow, 6);

        Button templatesBtn = createButton("📝", "#FFC107");
        templatesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTemplateSelector();
            }
        });
        templatesBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                openTemplates();
                return true;
            }
        });
        topButtonRow.addView(templatesBtn);

        addSpace(topButtonRow, 6);

        Button historyBtn = createButton("📜", "#00BCD4");
        historyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHistory();
            }
        });
        topButtonRow.addView(historyBtn);

        addSpace(topButtonRow, 6);

        Button backspaceBtn = createButton("⌫", "#FF5722");
        backspaceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteText();
            }
        });
        topButtonRow.addView(backspaceBtn);

        mainLayout.addView(topButtonRow);

        // Add vertical spacing
        addVerticalSpace(mainLayout, 20);

        // Status indicator
        statusText = new TextView(this);
        statusText.setText("⚫ Ready");
        statusText.setTextColor(Color.parseColor("#CCCCCC"));
        statusText.setTextSize(18);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 20, 0, 20);
        mainLayout.addView(statusText);

        // Amplitude indicator - horizontal waveform bar
        amplitudeIndicator = new View(this);
        amplitudeIndicator.setBackgroundColor(Color.parseColor("#4CAF50"));
        LinearLayout.LayoutParams ampParams = new LinearLayout.LayoutParams(
            dpToPx(100), // Start width - will grow with volume
            dpToPx(8)    // Height - thin horizontal bar
        );
        ampParams.gravity = Gravity.CENTER;
        ampParams.setMargins(0, 8, 0, 8);
        amplitudeIndicator.setLayoutParams(ampParams);
        amplitudeIndicator.setVisibility(View.GONE);

        // Rounded corners for the bar
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        shape.setColor(Color.parseColor("#4CAF50"));
        shape.setCornerRadius(dpToPx(4)); // Rounded edges
        amplitudeIndicator.setBackground(shape);

        mainLayout.addView(amplitudeIndicator);

        // Processing indicator
        processingIndicator = new ProgressBar(this);
        processingIndicator.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        progressParams.gravity = Gravity.CENTER;
        progressParams.setMargins(0, 20, 0, 20);
        processingIndicator.setLayoutParams(progressParams);
        mainLayout.addView(processingIndicator);

        addVerticalSpace(mainLayout, 20);

        // Control buttons - Row 1: Recording controls
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER);

        recordBtn = createButton("🎤 Start Recording", "#4CAF50");
        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording();
            }
        });
        row1.addView(recordBtn);

        addSpace(row1, 8);

        pauseBtn = createButton("⏸ Pause", "#FFC107");
        pauseBtn.setVisibility(View.GONE);
        pauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePause();
            }
        });
        row1.addView(pauseBtn);

        addSpace(row1, 8);

        cancelBtn = createButton("⊗ Cancel", "#FF5722");
        cancelBtn.setVisibility(View.GONE);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelRecording();
            }
        });
        row1.addView(cancelBtn);

        addSpace(row1, 8);

        stopBtn = createButton("✅ Process", "#4CAF50");
        stopBtn.setVisibility(View.GONE);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processRecording();
            }
        });
        row1.addView(stopBtn);

        mainLayout.addView(row1);

        // Add vertical spacing between rows
        addVerticalSpace(mainLayout, 12);

        // Control buttons - Row 2: Common keys
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER);

        Button periodBtn = createButton(".", "#607D8B");
        periodBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertText(".");
            }
        });
        row2.addView(periodBtn);

        addSpace(row2, 8);

        Button spaceBtn = createButton("Space", "#2196F3");
        spaceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertText(" ");
            }
        });
        row2.addView(spaceBtn);

        addSpace(row2, 8);

        Button enterBtn = createButton("↵ Enter", "#4CAF50");
        enterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertText("\n");
            }
        });
        row2.addView(enterBtn);

        mainLayout.addView(row2);
    }

    private Button createButton(String text, String colorHex) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(12);
        button.setPadding(16, 12, 16, 12);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor(colorHex));
        drawable.setCornerRadius(8);
        button.setBackground(drawable);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        button.setLayoutParams(params);

        return button;
    }

    private void addSpace(LinearLayout parent, int dp) {
        View space = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(dp), 0);
        space.setLayoutParams(params);
        parent.addView(space);
    }

    private void addVerticalSpace(LinearLayout parent, int dp) {
        View space = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dpToPx(dp)
        );
        space.setLayoutParams(params);
        parent.addView(space);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void startRecording() {
        recordBtn.setVisibility(View.GONE);
        pauseBtn.setVisibility(View.VISIBLE);
        cancelBtn.setVisibility(View.VISIBLE);
        stopBtn.setVisibility(View.VISIBLE);
        amplitudeIndicator.setVisibility(View.VISIBLE);

        isRecording = true;
        isPaused = false;
        pausedTime = 0;
        recordingStartTime = System.currentTimeMillis();
        mainHandler.post(timerRunnable);
        mainHandler.post(amplitudeRunnable); // Start amplitude updates

        // Haptic feedback on start
        vibrateHaptic(50);

        audioRecorder.startRecording(getCacheDir(), new AudioRecorder.RecordingCallback() {
            @Override
            public void onRecordingStarted() {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        statusText.setText("🔴 Recording... 00:00");
                        showToast("Recording started");
                    }
                });
            }

            @Override
            public void onRecordingStopped(File audioFile) {
                // Recording stopped
            }

            @Override
            public void onError(final String error) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        statusText.setText("❌ Error");
                        showToast("Recording error: " + error);
                        resetState();
                    }
                });
            }
        });
    }

    private void processRecording() {
        if (!isRecording) return;

        vibrateHaptic(50); // Haptic on process

        audioRecorder.stopRecording(new AudioRecorder.RecordingCallback() {
            @Override
            public void onRecordingStarted() {}

            @Override
            public void onRecordingStopped(final File audioFile) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        statusText.setText("⏳ Processing...");
                        processingIndicator.setVisibility(View.VISIBLE);
                        transcribeAndInsert(audioFile);
                    }
                });
            }

            @Override
            public void onError(final String error) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        statusText.setText("❌ Error");
                        showToast("Stop error: " + error);
                        resetState();
                    }
                });
            }
        });
    }

    private void transcribeAndInsert(final File audioFile) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                WhisperAPI.transcribeAudio(VoiceKeyboard.this, audioFile, new WhisperAPI.TranscriptionCallback() {
                    @Override
                    public void onSuccess(final String transcription) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                processingIndicator.setVisibility(View.GONE);
                                statusText.setText("✓ Inserted");

                                // Insert directly into input field
                                InputConnection ic = getCurrentInputConnection();
                                if (ic != null) {
                                    ic.commitText(transcription, 1);
                                    saveToHistory(transcription);
                                    vibrateHaptic(100); // Success haptic
                                    showToast("Text inserted");
                                } else {
                                    showToast("Cannot insert text");
                                }

                                audioFile.delete();
                                resetState();
                            }
                        });
                    }

                    @Override
                    public void onError(final String error) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                processingIndicator.setVisibility(View.GONE);
                                statusText.setText("❌ Error");
                                showToast("Transcription failed: " + error);
                                audioFile.delete();
                                resetState();
                            }
                        });
                    }
                });
            }
        }).start();
    }

    private void cancelRecording() {
        vibrateHaptic(30); // Haptic on cancel
        audioRecorder.release();
        resetState();
        showToast("Recording cancelled");
    }

    private void insertText(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(text, 1);
        }
    }

    private void deleteText() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.deleteSurroundingText(1, 0);
        }
    }

    private void togglePause() {
        if (isPaused) {
            // Resume
            audioRecorder.resumeRecording();
            isPaused = false;
            pauseBtn.setText("⏸ Pause");
            statusText.setText("🔴 Recording...");
            pausedTime += System.currentTimeMillis() - recordingStartTime;
            recordingStartTime = System.currentTimeMillis();
            mainHandler.post(timerRunnable);
            mainHandler.post(amplitudeRunnable); // Resume amplitude
            amplitudeIndicator.setVisibility(View.VISIBLE);
            vibrateHaptic(30);
            showToast("Recording resumed");
        } else {
            // Pause
            audioRecorder.pauseRecording();
            isPaused = true;
            pauseBtn.setText("▶️ Resume");
            statusText.setText("⏸ Paused");
            mainHandler.removeCallbacks(timerRunnable);
            mainHandler.removeCallbacks(amplitudeRunnable); // Pause amplitude
            amplitudeIndicator.setVisibility(View.GONE);
            vibrateHaptic(30);
            showToast("Recording paused");
        }
    }

    private void saveToHistory(String text) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String entry = timestamp + "|||" + text;

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String history = prefs.getString(KEY_HISTORY, "");

        if (!history.isEmpty()) {
            history += "\n\n===ENTRY===\n\n";
        }
        history += entry;

        prefs.edit().putString(KEY_HISTORY, history).apply();
    }

    private void openSettings() {
        android.content.Intent intent = new android.content.Intent(this, SettingsActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void showTemplateSelector() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String templates = prefs.getString("templates", "");

        if (templates.isEmpty()) {
            showToast("No templates yet! Long-press 📝 to create");
            return;
        }

        String[] entries = templates.split("\n===TEMPLATE===\n");
        final String[] names = new String[entries.length];
        final String[] texts = new String[entries.length];

        int count = 0;
        for (String entry : entries) {
            if (entry.trim().isEmpty()) continue;
            String[] parts = entry.split("\\|\\|\\|", 2);
            if (parts.length >= 2) {
                names[count] = parts[0];
                texts[count] = parts[1];
                count++;
            }
        }

        final String[] finalNames = new String[count];
        final String[] finalTexts = new String[count];
        System.arraycopy(names, 0, finalNames, 0, count);
        System.arraycopy(texts, 0, finalTexts, 0, count);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Insert Template");
        builder.setItems(finalNames, new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    ic.commitText(finalTexts[which], 1);
                    vibrateHaptic(50);
                    showToast("Template inserted!");
                }
            }
        });
        builder.setNegativeButton("Cancel", null);

        android.app.AlertDialog dialog = builder.create();
        dialog.getWindow().setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG);
        dialog.show();
    }

    private void openTemplates() {
        // Cancel any ongoing recording
        if (isRecording) {
            audioRecorder.release();
            resetState();
        }

        android.content.Intent intent = new android.content.Intent(this, TemplatesActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void showHistory() {
        // Cancel any ongoing recording
        if (isRecording) {
            audioRecorder.release();
            resetState();
        }

        android.content.Intent intent = new android.content.Intent(this, HistoryActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void resetState() {
        mainHandler.removeCallbacks(timerRunnable);
        mainHandler.removeCallbacks(amplitudeRunnable);
        isRecording = false;
        isPaused = false;
        pausedTime = 0;

        recordBtn.setVisibility(View.VISIBLE);
        pauseBtn.setVisibility(View.GONE);
        pauseBtn.setText("⏸ Pause");
        cancelBtn.setVisibility(View.GONE);
        stopBtn.setVisibility(View.GONE);
        amplitudeIndicator.setVisibility(View.GONE);

        processingIndicator.setVisibility(View.GONE);
        statusText.setText("⚫ Ready");
    }

    private String formatTime(long seconds) {
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, secs);
    }

    private void showToast(final String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(VoiceKeyboard.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void vibrateHaptic(long milliseconds) {
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(milliseconds);
        }
    }

    private void updateAmplitudeIndicator(int amplitude) {
        // Normalize amplitude (0-32767) to width (50-300 dp) - horizontal bar
        int maxAmplitude = 32767;
        int minWidth = 50;
        int maxWidth = 300;

        float normalized = (float) amplitude / maxAmplitude;
        int width = (int) (minWidth + (normalized * (maxWidth - minWidth)));
        width = Math.max(minWidth, Math.min(maxWidth, width));

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) amplitudeIndicator.getLayoutParams();
        params.width = dpToPx(width);
        params.height = dpToPx(8); // Keep height constant at 8dp
        amplitudeIndicator.setLayoutParams(params);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (audioRecorder != null) {
            audioRecorder.release();
        }
        mainHandler.removeCallbacks(timerRunnable);
    }
}
