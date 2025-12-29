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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
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
    private LinearLayout topButtonRow; // Top bar with Settings, Templates, History, Backspace
    private LinearLayout contentContainer; // Container for recording controls
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
    private boolean isKeyboardMode = false; // Toggle between keyboard and voice mode
    private boolean isShiftPressed = false; // Caps lock state
    private boolean isEmojiMode = false; // Emoji picker state
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
        // Create main container with glassmorphism background
        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        // Glassmorphism background with gradient
        GradientDrawable glassBackground = new GradientDrawable();
        glassBackground.setColors(new int[]{
            Color.parseColor("#CC1A1A2E"), // Semi-transparent dark blue
            Color.parseColor("#CC16213E")  // Semi-transparent darker blue
        });
        glassBackground.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        glassBackground.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        glassBackground.setCornerRadius(dpToPx(20));
        glassBackground.setStroke(dpToPx(1), Color.parseColor("#33FFFFFF")); // Subtle white border
        mainLayout.setBackground(glassBackground);

        mainLayout.setPadding(8, 8, 8, 8);
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dpToPx(300) // Height for voice keyboard with enhancement features
        ));

        createEditorView();
        return mainLayout;
    }

    private void createEditorView() {
        mainLayout.removeAllViews();

        // Top buttons row: Settings, Templates, History, Backspace
        topButtonRow = new LinearLayout(this);
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

        Button templatesBtn = createButton("📝 Templates", "#FFC107");
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

        Button historyBtn = createButton("📜 History", "#00BCD4");
        historyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHistoryOverlay(); // Tap = view/insert in keyboard
            }
        });
        historyBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showHistory(); // Long-press = full screen management
                return true;
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

        // Add vertical spacing (reduced)
        addVerticalSpace(mainLayout, 8);

        // Create content container for recording controls that can be replaced
        contentContainer = new LinearLayout(this);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        contentContainer.setLayoutParams(contentParams);
        mainLayout.addView(contentContainer);

        // Add recording controls to content container
        createRecordingControls();
    }

    private void createRecordingControls() {
        contentContainer.removeAllViews();

        // Status indicator with glassmorphism
        statusText = new TextView(this);
        statusText.setText("⚫ Ready");
        statusText.setTextColor(Color.parseColor("#FFFFFF"));
        statusText.setTextSize(18);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 20, 0, 20);
        statusText.setShadowLayer(8, 0, 0, Color.parseColor("#66FFFFFF")); // Subtle glow
        contentContainer.addView(statusText);

        // Amplitude indicator - horizontal waveform bar with gradient
        amplitudeIndicator = new View(this);
        LinearLayout.LayoutParams ampParams = new LinearLayout.LayoutParams(
            dpToPx(100), // Start width - will grow with volume
            dpToPx(8)    // Height - thin horizontal bar
        );
        ampParams.gravity = Gravity.CENTER;
        ampParams.setMargins(0, 8, 0, 8);
        amplitudeIndicator.setLayoutParams(ampParams);
        amplitudeIndicator.setVisibility(View.GONE);

        // Initial gradient background for the bar - red color
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        int red = Color.parseColor("#FF5722");
        int lightRed = lightenColor(red, 0.3f);
        shape.setColors(new int[]{lightRed, red});
        shape.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        shape.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        shape.setCornerRadius(dpToPx(4)); // Rounded edges
        amplitudeIndicator.setBackground(shape);

        contentContainer.addView(amplitudeIndicator);

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
        contentContainer.addView(processingIndicator);

        addVerticalSpace(contentContainer, 20);

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

        contentContainer.addView(row1);

        // Add vertical spacing between rows
        addVerticalSpace(contentContainer, 12);

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

        addSpace(row2, 8);

        // Add keyboard toggle button
        Button keyboardToggleBtn = createButton("⌨ Keyboard", "#9C27B0");
        keyboardToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isKeyboardMode = true;
                createKeyboardLayout();
            }
        });
        row2.addView(keyboardToggleBtn);

        contentContainer.addView(row2);
    }

    private void createKeyboardLayout() {
        // Hide top button row in keyboard mode
        topButtonRow.setVisibility(View.GONE);

        contentContainer.removeAllViews();

        if (isEmojiMode) {
            createEmojiPicker();
            return;
        }

        // Keyboard toolbar with settings and voice toggle
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(8, 0, 8, 0);

        // Settings icon
        Button settingsIcon = createCompactButton("⚙", "#607D8B");
        settingsIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings();
            }
        });
        toolbar.addView(settingsIcon);

        addSpace(toolbar, 8);

        // Templates icon
        Button templatesIcon = createCompactButton("📝", "#607D8B");
        templatesIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                topButtonRow.setVisibility(View.VISIBLE);
                showTemplateSelector();
            }
        });
        toolbar.addView(templatesIcon);

        addSpace(toolbar, 8);

        // History icon
        Button historyIcon = createCompactButton("📜", "#607D8B");
        historyIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                topButtonRow.setVisibility(View.VISIBLE);
                showHistoryOverlay();
            }
        });
        toolbar.addView(historyIcon);

        // Spacer
        View spacer = new View(this);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(0, 1);
        spacerParams.weight = 1.0f;
        spacer.setLayoutParams(spacerParams);
        toolbar.addView(spacer);

        // Voice mode button
        Button voiceBtn = createCompactButton("🎤", "#9C27B0");
        voiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isKeyboardMode = false;
                isEmojiMode = false;
                vibrateHaptic(30);
                topButtonRow.setVisibility(View.VISIBLE);
                createRecordingControls();
            }
        });
        toolbar.addView(voiceBtn);

        contentContainer.addView(toolbar);
        addVerticalSpace(contentContainer, 6);

        // Number row
        LinearLayout numberRow = new LinearLayout(this);
        numberRow.setOrientation(LinearLayout.HORIZONTAL);
        numberRow.setGravity(Gravity.CENTER);
        String[] numbers = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
        for (String num : numbers) {
            Button numBtn = createKeyButton(num, "#607D8B");
            numberRow.addView(numBtn);
        }
        contentContainer.addView(numberRow);
        addVerticalSpace(contentContainer, 6);

        // First row - QWERTY
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER);
        String[] row1Keys = {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"};
        for (String key : row1Keys) {
            Button keyBtn = createKeyButton(isShiftPressed ? key.toUpperCase() : key, "#2196F3");
            row1.addView(keyBtn);
        }
        contentContainer.addView(row1);
        addVerticalSpace(contentContainer, 6);

        // Second row - ASDFGH
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER);
        String[] row2Keys = {"a", "s", "d", "f", "g", "h", "j", "k", "l"};
        for (String key : row2Keys) {
            Button keyBtn = createKeyButton(isShiftPressed ? key.toUpperCase() : key, "#2196F3");
            row2.addView(keyBtn);
        }
        contentContainer.addView(row2);
        addVerticalSpace(contentContainer, 6);

        // Third row - ZXCVBN with Shift
        LinearLayout row3 = new LinearLayout(this);
        row3.setOrientation(LinearLayout.HORIZONTAL);
        row3.setGravity(Gravity.CENTER);

        // Shift button
        Button shiftBtn = createKeyButton(isShiftPressed ? "⇧" : "⇧", "#FFC107");
        shiftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isShiftPressed = !isShiftPressed;
                vibrateHaptic(30);
                createKeyboardLayout(); // Refresh to update case
            }
        });
        row3.addView(shiftBtn);

        String[] row3Keys = {"z", "x", "c", "v", "b", "n", "m"};
        for (String key : row3Keys) {
            Button keyBtn = createKeyButton(isShiftPressed ? key.toUpperCase() : key, "#2196F3");
            row3.addView(keyBtn);
        }

        // Backspace button
        Button backspaceBtn = createKeyButton("⌫", "#FF5722");
        backspaceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteText();
            }
        });
        row3.addView(backspaceBtn);

        contentContainer.addView(row3);
        addVerticalSpace(contentContainer, 6);

        // Fourth row - Special keys
        LinearLayout row4 = new LinearLayout(this);
        row4.setOrientation(LinearLayout.HORIZONTAL);
        row4.setGravity(Gravity.CENTER);

        // Symbols button (?123)
        Button symbolsBtn = createKeyButton("?123", "#607D8B");
        LinearLayout.LayoutParams symbolsParams = new LinearLayout.LayoutParams(
            0,
            dpToPx(42)
        );
        symbolsParams.weight = 1.5f;
        symbolsParams.setMargins(3, 0, 3, 0);
        symbolsBtn.setLayoutParams(symbolsParams);
        symbolsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrateHaptic(30);
                showToast("Symbol keyboard coming soon!");
            }
        });
        row4.addView(symbolsBtn);

        // Emoji button
        Button emojiBtn = createKeyButton("😊", "#4CAF50");
        emojiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isEmojiMode = true;
                vibrateHaptic(30);
                createKeyboardLayout();
            }
        });
        row4.addView(emojiBtn);

        // Comma
        Button commaBtn = createKeyButton(",", "#607D8B");
        row4.addView(commaBtn);

        // Space bar (wider)
        Button spaceBtn = createKeyButton("Space", "#2196F3");
        LinearLayout.LayoutParams spaceParams = new LinearLayout.LayoutParams(
            0,
            dpToPx(42)
        );
        spaceParams.weight = 4.0f;
        spaceParams.setMargins(3, 0, 3, 0);
        spaceBtn.setLayoutParams(spaceParams);
        spaceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertText(" ");
            }
        });
        row4.addView(spaceBtn);

        // Period
        Button periodBtn = createKeyButton(".", "#607D8B");
        row4.addView(periodBtn);

        // Enter
        Button enterBtn = createKeyButton("↵", "#4CAF50");
        LinearLayout.LayoutParams enterParams = new LinearLayout.LayoutParams(
            0,
            dpToPx(42)
        );
        enterParams.weight = 1.5f;
        enterParams.setMargins(3, 0, 3, 0);
        enterBtn.setLayoutParams(enterParams);
        enterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertText("\n");
            }
        });
        row4.addView(enterBtn);

        contentContainer.addView(row4);
    }

    private void createEmojiPicker() {
        contentContainer.removeAllViews();

        TextView title = new TextView(this);
        title.setText("😊 Emoji Picker");
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 10, 0, 10);
        title.setShadowLayer(8, 0, 0, Color.parseColor("#66FFFFFF"));
        contentContainer.addView(title);

        // Scrollable emoji grid
        ScrollView scrollView = new ScrollView(this);
        LinearLayout emojiContainer = new LinearLayout(this);
        emojiContainer.setOrientation(LinearLayout.VERTICAL);

        // Popular emojis organized by category
        String[][] emojiCategories = {
            {"😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "🙃"},
            {"😉", "😊", "😇", "🥰", "😍", "🤩", "😘", "😗", "😚", "😙"},
            {"😋", "😛", "😜", "🤪", "😝", "🤑", "🤗", "🤭", "🤫", "🤔"},
            {"🤐", "🤨", "😐", "😑", "😶", "😏", "😒", "🙄", "😬", "🤥"},
            {"😌", "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕", "🤢", "🤮"},
            {"👍", "👎", "👌", "✌", "🤞", "🤟", "🤘", "🤙", "👈", "👉"},
            {"👆", "👇", "☝", "✋", "🤚", "🖐", "🖖", "👋", "🤝", "💪"},
            {"❤", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔"},
            {"⭐", "✨", "💫", "🌟", "💥", "🔥", "💯", "✅", "❌", "⚠"}
        };

        for (String[] row : emojiCategories) {
            LinearLayout emojiRow = new LinearLayout(this);
            emojiRow.setOrientation(LinearLayout.HORIZONTAL);
            emojiRow.setGravity(Gravity.CENTER);

            for (String emoji : row) {
                Button emojiBtn = createKeyButton(emoji, "#2196F3");
                emojiRow.addView(emojiBtn);
            }
            emojiContainer.addView(emojiRow);
            addVerticalSpace(emojiContainer, 4);
        }

        scrollView.addView(emojiContainer);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dpToPx(120)
        );
        scrollView.setLayoutParams(scrollParams);
        contentContainer.addView(scrollView);

        // Back button
        Button backBtn = createButton("← Back to Keyboard", "#607D8B");
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isEmojiMode = false;
                vibrateHaptic(30);
                createKeyboardLayout();
            }
        });
        contentContainer.addView(backBtn);
    }

    private Button createCompactButton(final String text, String colorHex) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setPadding(12, 6, 12, 6);
        button.setElevation(dpToPx(2));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            dpToPx(36),
            dpToPx(32)
        );
        button.setLayoutParams(params);

        // Create gradient background
        GradientDrawable drawable = new GradientDrawable();
        int baseColor = Color.parseColor(colorHex);
        int lighterColor = lightenColor(baseColor, 0.2f);
        drawable.setColors(new int[]{lighterColor, baseColor});
        drawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        drawable.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        drawable.setCornerRadius(dpToPx(6));
        drawable.setStroke(dpToPx(1), Color.parseColor("#44FFFFFF"));
        button.setBackground(drawable);

        // Add press animation
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(50)
                            .setInterpolator(new AccelerateDecelerateInterpolator()).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(50)
                            .setInterpolator(new AccelerateDecelerateInterpolator()).start();
                        break;
                }
                return false;
            }
        });

        return button;
    }

    private Button createKeyButton(final String text, String colorHex) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(16);
        button.setPadding(4, 8, 4, 8);
        button.setElevation(dpToPx(2));

        // Full width button size for keyboard - use weight for equal distribution
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0,
            dpToPx(42)
        );
        params.weight = 1.0f;
        params.setMargins(3, 0, 3, 0);
        button.setLayoutParams(params);

        // Create gradient background
        GradientDrawable drawable = new GradientDrawable();
        int baseColor = Color.parseColor(colorHex);
        int lighterColor = lightenColor(baseColor, 0.2f);
        drawable.setColors(new int[]{lighterColor, baseColor});
        drawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        drawable.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        drawable.setCornerRadius(dpToPx(8));
        drawable.setStroke(dpToPx(1), Color.parseColor("#44FFFFFF"));
        button.setBackground(drawable);

        // Default click behavior - insert text
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertText(text);
                if (isShiftPressed && text.length() == 1 && Character.isLetter(text.charAt(0))) {
                    isShiftPressed = false; // Auto-disable shift after one letter
                }
            }
        });

        // Add press animation
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(50)
                            .setInterpolator(new AccelerateDecelerateInterpolator()).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(50)
                            .setInterpolator(new AccelerateDecelerateInterpolator()).start();
                        break;
                }
                return false;
            }
        });

        return button;
    }

    private Button createButton(String text, String colorHex) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(12);
        button.setPadding(16, 12, 16, 12);
        button.setElevation(dpToPx(4));

        // Create gradient background
        GradientDrawable drawable = new GradientDrawable();
        int baseColor = Color.parseColor(colorHex);
        int lighterColor = lightenColor(baseColor, 0.2f);
        drawable.setColors(new int[]{lighterColor, baseColor});
        drawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        drawable.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        drawable.setCornerRadius(dpToPx(12));

        // Add subtle border for glassmorphism
        drawable.setStroke(dpToPx(1), Color.parseColor("#44FFFFFF"));
        button.setBackground(drawable);

        // Add press animation
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Scale down on press
                        v.animate()
                            .scaleX(0.95f)
                            .scaleY(0.95f)
                            .setDuration(100)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        // Scale back up on release
                        v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start();
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

    // Helper method to lighten a color
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

        // Add pulsing glow to status text
        statusText.animate()
            .alpha(0.7f)
            .setDuration(800)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    if (isRecording && !isPaused) {
                        statusText.animate()
                            .alpha(1.0f)
                            .setDuration(800)
                            .withEndAction(this)
                            .start();
                    }
                }
            })
            .start();

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
        // Hide top button bar for more space
        topButtonRow.setVisibility(View.GONE);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String templates = prefs.getString("templates", "");

        String[] entries = templates.isEmpty() ? new String[0] : templates.split("\n===TEMPLATE===\n");
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

        // Create a custom view for template selection within the keyboard
        final LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(20, 20, 20, 20);

        // Glassmorphism background for dialog
        GradientDrawable dialogBg = new GradientDrawable();
        dialogBg.setColors(new int[]{
            Color.parseColor("#DD1A1A2E"),
            Color.parseColor("#DD16213E")
        });
        dialogBg.setCornerRadius(dpToPx(16));
        dialogBg.setStroke(dpToPx(1), Color.parseColor("#44FFFFFF"));
        dialogLayout.setBackground(dialogBg);

        // Title
        TextView titleView = new TextView(this);
        titleView.setText("📝 Templates");
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(18);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 20);
        titleView.setShadowLayer(8, 0, 0, Color.parseColor("#66FFFFFF"));
        dialogLayout.addView(titleView);

        // Create scrollable list of templates
        ScrollView scrollView = new ScrollView(this);
        LinearLayout templateList = new LinearLayout(this);
        templateList.setOrientation(LinearLayout.VERTICAL);


        // Add existing templates
        if (count == 0) {
            // Show message when no templates
            TextView emptyMsg = new TextView(this);
            emptyMsg.setText("No templates yet.\nTap 'Add New Template' above to create one!");
            emptyMsg.setTextColor(Color.parseColor("#AAAAAA"));
            emptyMsg.setTextSize(14);
            emptyMsg.setGravity(Gravity.CENTER);
            emptyMsg.setPadding(0, 20, 0, 20);
            templateList.addView(emptyMsg);
        } else {
            for (int i = 0; i < finalNames.length; i++) {
                final int index = i;
                Button templateBtn = createButton(finalNames[i], "#2196F3");
                templateBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) {
                            ic.commitText(finalTexts[index], 1);
                            vibrateHaptic(50);
                            showToast("Template inserted!");
                            topButtonRow.setVisibility(View.VISIBLE); // Show top bar
                            createRecordingControls(); // Restore recording controls
                        }
                    }
                });
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
                btnParams.setMargins(0, 0, 0, 8);
                templateBtn.setLayoutParams(btnParams);
                templateList.addView(templateBtn);
            }
        }

        scrollView.addView(templateList);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dpToPx(220)  // Fixed height to show ~5 items (44dp per item approx)
        );
        scrollView.setLayoutParams(scrollParams);
        dialogLayout.addView(scrollView);

        // Manage Templates button (opens full activity)
        Button manageBtn = createButton("⚙ Manage Templates", "#9C27B0");
        manageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                topButtonRow.setVisibility(View.VISIBLE); // Show top bar
                createRecordingControls(); // Restore recording controls
                openTemplates(); // Open full templates activity
            }
        });
        LinearLayout.LayoutParams manageParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        manageParams.setMargins(0, 12, 0, 0);
        manageBtn.setLayoutParams(manageParams);
        dialogLayout.addView(manageBtn);

        // Close button
        Button closeBtn = createButton("← Back to Keyboard", "#607D8B");
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                topButtonRow.setVisibility(View.VISIBLE); // Show top bar
                createRecordingControls(); // Restore recording controls
            }
        });
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        closeParams.setMargins(0, 8, 0, 0);
        closeBtn.setLayoutParams(closeParams);
        dialogLayout.addView(closeBtn);

        // Replace content container with dialog
        contentContainer.removeAllViews();
        contentContainer.addView(dialogLayout);
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

    private void showHistoryOverlay() {
        // Hide top button bar for more space
        topButtonRow.setVisibility(View.GONE);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String history = prefs.getString(KEY_HISTORY, "");

        String[] entries = history.isEmpty() ? new String[0] : history.split("\n\n===ENTRY===\n\n");
        final String[] timestamps = new String[entries.length];
        final String[] texts = new String[entries.length];

        int count = 0;
        for (String entry : entries) {
            if (entry.trim().isEmpty()) continue;
            String[] parts = entry.split("\\|\\|\\|", 2);
            if (parts.length >= 2) {
                timestamps[count] = parts[0];
                texts[count] = parts[1];
                count++;
            }
        }

        final String[] finalTimestamps = new String[count];
        final String[] finalTexts = new String[count];
        System.arraycopy(timestamps, 0, finalTimestamps, 0, count);
        System.arraycopy(texts, 0, finalTexts, 0, count);

        // Create a custom view for history within the keyboard
        final LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(20, 20, 20, 20);

        // Glassmorphism background for dialog
        GradientDrawable dialogBg = new GradientDrawable();
        dialogBg.setColors(new int[]{
            Color.parseColor("#DD1A1A2E"),
            Color.parseColor("#DD16213E")
        });
        dialogBg.setCornerRadius(dpToPx(16));
        dialogBg.setStroke(dpToPx(1), Color.parseColor("#44FFFFFF"));
        dialogLayout.setBackground(dialogBg);

        // Title
        TextView titleView = new TextView(this);
        titleView.setText("📜 History");
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(18);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 20);
        titleView.setShadowLayer(8, 0, 0, Color.parseColor("#66FFFFFF"));
        dialogLayout.addView(titleView);

        // Create scrollable list of history entries
        ScrollView scrollView = new ScrollView(this);
        LinearLayout historyList = new LinearLayout(this);
        historyList.setOrientation(LinearLayout.VERTICAL);

        // Add history entries
        if (count == 0) {
            // Show message when no history
            TextView emptyMsg = new TextView(this);
            emptyMsg.setText("No transcription history yet.\nStart recording to build your history!");
            emptyMsg.setTextColor(Color.parseColor("#AAAAAA"));
            emptyMsg.setTextSize(14);
            emptyMsg.setGravity(Gravity.CENTER);
            emptyMsg.setPadding(0, 20, 0, 20);
            historyList.addView(emptyMsg);
        } else {
            // Show entries in reverse order (newest first)
            for (int i = count - 1; i >= 0; i--) {
                final int index = i;

                // Create container for each history entry
                LinearLayout entryContainer = new LinearLayout(this);
                entryContainer.setOrientation(LinearLayout.VERTICAL);
                entryContainer.setPadding(12, 12, 12, 12);

                // Entry background
                GradientDrawable entryBg = new GradientDrawable();
                entryBg.setColor(Color.parseColor("#33FFFFFF"));
                entryBg.setCornerRadius(dpToPx(8));
                entryBg.setStroke(dpToPx(1), Color.parseColor("#55FFFFFF"));
                entryContainer.setBackground(entryBg);

                // Timestamp
                TextView timestampView = new TextView(this);
                timestampView.setText(finalTimestamps[index]);
                timestampView.setTextColor(Color.parseColor("#AAAAAA"));
                timestampView.setTextSize(10);
                timestampView.setPadding(0, 0, 0, 4);
                entryContainer.addView(timestampView);

                // Text preview (truncated)
                TextView textView = new TextView(this);
                String preview = finalTexts[index].length() > 80
                    ? finalTexts[index].substring(0, 80) + "..."
                    : finalTexts[index];
                textView.setText(preview);
                textView.setTextColor(Color.WHITE);
                textView.setTextSize(12);
                textView.setMaxLines(2);
                entryContainer.addView(textView);

                // Insert button for entry
                entryContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) {
                            ic.commitText(finalTexts[index], 1);
                            vibrateHaptic(50);
                            showToast("History entry inserted!");
                            topButtonRow.setVisibility(View.VISIBLE); // Show top bar
                            createRecordingControls(); // Restore recording controls
                        }
                    }
                });

                LinearLayout.LayoutParams entryParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
                entryParams.setMargins(0, 0, 0, 8);
                entryContainer.setLayoutParams(entryParams);
                historyList.addView(entryContainer);
            }
        }

        scrollView.addView(historyList);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dpToPx(250)  // Fixed height to show ~5 history entries
        );
        scrollView.setLayoutParams(scrollParams);
        dialogLayout.addView(scrollView);

        // Manage History button (opens full activity)
        Button manageBtn = createButton("⚙ Manage History", "#9C27B0");
        manageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                topButtonRow.setVisibility(View.VISIBLE); // Show top bar
                createRecordingControls(); // Restore recording controls
                showHistory(); // Open full history activity
            }
        });
        LinearLayout.LayoutParams manageParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        manageParams.setMargins(0, 12, 0, 0);
        manageBtn.setLayoutParams(manageParams);
        dialogLayout.addView(manageBtn);

        // Close button
        Button closeBtn = createButton("← Back to Keyboard", "#607D8B");
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                topButtonRow.setVisibility(View.VISIBLE); // Show top bar
                createRecordingControls(); // Restore recording controls
            }
        });
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        closeParams.setMargins(0, 8, 0, 0);
        closeBtn.setLayoutParams(closeParams);
        dialogLayout.addView(closeBtn);

        // Replace content container with dialog
        contentContainer.removeAllViews();
        contentContainer.addView(dialogLayout);
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

        // Stop any ongoing animations and reset alpha
        statusText.animate().cancel();
        statusText.setAlpha(1.0f);
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

        // Animate color based on amplitude: red -> yellow -> green
        int color;
        if (normalized < 0.4f) {
            // Red zone (low volume)
            color = Color.parseColor("#FF5722");
        } else if (normalized < 0.7f) {
            // Yellow zone (medium volume) - transition from red to yellow
            float localNorm = (normalized - 0.4f) / 0.3f;
            int red = Color.parseColor("#FF5722");
            int yellow = Color.parseColor("#FFC107");
            color = blendColors(red, yellow, localNorm);
        } else {
            // Green zone (high volume) - transition from yellow to green
            float localNorm = (normalized - 0.7f) / 0.3f;
            int yellow = Color.parseColor("#FFC107");
            int green = Color.parseColor("#4CAF50");
            color = blendColors(yellow, green, localNorm);
        }

        // Update the waveform bar with gradient and glow effect
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        int lighterColor = lightenColor(color, 0.3f);
        shape.setColors(new int[]{lighterColor, color});
        shape.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        shape.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        shape.setCornerRadius(dpToPx(4));

        // Add glow effect for higher amplitudes
        if (normalized > 0.5f) {
            int glowAlpha = (int) ((normalized - 0.5f) * 2 * 150); // 0-150 alpha
            int glowColor = Color.argb(glowAlpha, Color.red(color), Color.green(color), Color.blue(color));
            shape.setStroke(dpToPx(2), glowColor);
        }

        amplitudeIndicator.setBackground(shape);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) amplitudeIndicator.getLayoutParams();
        params.width = dpToPx(width);
        params.height = dpToPx(8); // Keep height constant at 8dp
        amplitudeIndicator.setLayoutParams(params);
    }

    // Helper method to blend two colors
    private int blendColors(int color1, int color2, float ratio) {
        float inverseRatio = 1 - ratio;
        int r = (int) (Color.red(color1) * inverseRatio + Color.red(color2) * ratio);
        int g = (int) (Color.green(color1) * inverseRatio + Color.green(color2) * ratio);
        int b = (int) (Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio);
        return Color.rgb(r, g, b);
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        super.onFinishInputView(finishingInput);
        // Cancel any ongoing recording when keyboard is closed
        if (isRecording) {
            audioRecorder.release();
            resetState();
            showToast("Recording cancelled - keyboard closed");
        }
    }

    @Override
    public void onWindowHidden() {
        super.onWindowHidden();
        // Cancel any ongoing recording when keyboard is hidden
        if (isRecording) {
            audioRecorder.release();
            resetState();
            showToast("Recording cancelled - keyboard hidden");
        }
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();
        // Cancel any ongoing recording when input finishes
        if (isRecording) {
            audioRecorder.release();
            resetState();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (isRecording && audioRecorder != null) {
            audioRecorder.release();
        }
        if (audioRecorder != null) {
            audioRecorder.release();
        }
        if (mainHandler != null) {
            mainHandler.removeCallbacks(timerRunnable);
            mainHandler.removeCallbacks(amplitudeRunnable);
        }
    }
}
