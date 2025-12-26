package com.fastkeyboard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.Toast;
import android.graphics.Color;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.FrameLayout;
import java.io.File;

public class FastKeyboard extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private LinearLayout mainLayout;
    private AudioRecorder audioRecorder;
    private Button voiceButton;
    private Button pauseButton;
    private boolean isRecording = false;
    private boolean isPaused = false;
    private Handler mainHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        audioRecorder = new AudioRecorder();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public View onCreateInputView() {
        // Create main container
        FrameLayout container = new FrameLayout(this);
        container.setBackgroundColor(Color.parseColor("#2C2C2C"));

        // Create keyboard layout
        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#2C2C2C"));
        mainLayout.setPadding(10, 10, 10, 10);

        // Top row with voice and pause buttons (right-aligned)
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.END);
        topRow.setPadding(5, 5, 5, 10);
        topRow.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Pause button (hidden initially)
        pauseButton = new Button(this);
        pauseButton.setText("⏸");
        pauseButton.setTextColor(Color.WHITE);
        pauseButton.setBackgroundColor(Color.parseColor("#FF9800"));
        pauseButton.setTextSize(18);
        pauseButton.setPadding(20, 10, 20, 10);
        pauseButton.setVisibility(View.GONE);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePause();
            }
        });
        topRow.addView(pauseButton);

        // Voice button (top-right)
        voiceButton = new Button(this);
        voiceButton.setText("🎤");
        voiceButton.setTextColor(Color.WHITE);
        voiceButton.setBackgroundColor(Color.parseColor("#424242"));
        voiceButton.setTextSize(18);
        voiceButton.setPadding(20, 10, 20, 10);
        LinearLayout.LayoutParams voiceParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        voiceParams.setMargins(10, 0, 0, 0);
        voiceButton.setLayoutParams(voiceParams);
        voiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleVoiceInput();
            }
        });
        topRow.addView(voiceButton);

        mainLayout.addView(topRow);

        // Row 1: Q W E R T Y U I O P
        addKeyRow(new String[]{"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"});

        // Row 2: A S D F G H J K L
        addKeyRow(new String[]{"A", "S", "D", "F", "G", "H", "J", "K", "L"});

        // Row 3: Z X C V B N M DEL
        addKeyRow(new String[]{"Z", "X", "C", "V", "B", "N", "M", "⌫"});

        // Row 4: Space, Enter
        LinearLayout bottomRow = createRow();

        Button spaceBtn = createKey("SPACE");
        LinearLayout.LayoutParams spaceParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 3.0f);
        spaceParams.setMargins(5, 5, 5, 5);
        spaceBtn.setLayoutParams(spaceParams);
        bottomRow.addView(spaceBtn);

        Button enterBtn = createKey("↵");
        LinearLayout.LayoutParams enterParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        enterParams.setMargins(5, 5, 5, 5);
        enterBtn.setLayoutParams(enterParams);
        bottomRow.addView(enterBtn);

        mainLayout.addView(bottomRow);

        container.addView(mainLayout);
        return container;
    }

    private LinearLayout createRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return row;
    }

    private void addKeyRow(String[] keys) {
        LinearLayout row = createRow();

        for (String key : keys) {
            Button btn = createKey(key);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
            );
            params.setMargins(5, 5, 5, 5);
            btn.setLayoutParams(params);
            row.addView(btn);
        }

        mainLayout.addView(row);
    }

    private Button createKey(String label) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(Color.parseColor("#424242"));
        btn.setAllCaps(false);
        btn.setPadding(0, 30, 0, 30);
        btn.setTextSize(16);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleKeyPress(label);
            }
        });

        return btn;
    }

    private void handleKeyPress(String key) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        switch (key) {
            case "⌫":
                CharSequence selectedText = ic.getSelectedText(0);
                if (selectedText == null || selectedText.length() == 0) {
                    ic.deleteSurroundingText(1, 0);
                } else {
                    ic.commitText("", 1);
                }
                break;

            case "SPACE":
                ic.commitText(" ", 1);
                break;

            case "↵":
                ic.commitText("\n", 1);
                break;

            default:
                ic.commitText(key.toLowerCase(), 1);
                break;
        }
    }

    private void handleVoiceInput() {
        if (!isRecording) {
            startRecording();
        } else {
            stopRecordingAndTranscribe();
        }
    }

    private void togglePause() {
        if (isPaused) {
            // Resume recording
            audioRecorder.resumeRecording();
            isPaused = false;
            pauseButton.setText("⏸");
            pauseButton.setBackgroundColor(Color.parseColor("#FF9800"));
            showToast("Recording resumed");
        } else {
            // Pause recording
            audioRecorder.pauseRecording();
            isPaused = true;
            pauseButton.setText("▶");
            pauseButton.setBackgroundColor(Color.parseColor("#4CAF50"));
            showToast("Recording paused");
        }
    }

    private void startRecording() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            showToast("Microphone permission required. Please grant in Settings.");
            return;
        }

        File cacheDir = getCacheDir();
        audioRecorder.startRecording(cacheDir, new AudioRecorder.RecordingCallback() {
            @Override
            public void onRecordingStarted() {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        isRecording = true;
                        isPaused = false;
                        voiceButton.setBackgroundColor(Color.RED);
                        voiceButton.setText("⏹");
                        pauseButton.setVisibility(View.VISIBLE);
                        pauseButton.setText("⏸");
                        pauseButton.setBackgroundColor(Color.parseColor("#FF9800"));
                        showToast("Recording... Tap ⏹ to stop or ⏸ to pause");
                    }
                });
            }

            @Override
            public void onRecordingStopped(File audioFile) {
                // Not used here
            }

            @Override
            public void onError(String error) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        showToast("Error: " + error);
                        resetVoiceButton();
                    }
                });
            }
        });
    }

    private void stopRecordingAndTranscribe() {
        audioRecorder.stopRecording(new AudioRecorder.RecordingCallback() {
            @Override
            public void onRecordingStarted() {
                // Not used
            }

            @Override
            public void onRecordingStopped(final File audioFile) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        resetVoiceButton();
                        showToast("Transcribing...");

                        WhisperAPI.transcribeAudio(FastKeyboard.this, audioFile, new WhisperAPI.TranscriptionCallback() {
                            @Override
                            public void onSuccess(final String transcription) {
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        InputConnection ic = getCurrentInputConnection();
                                        if (ic != null) {
                                            ic.commitText(transcription, 1);
                                        }
                                        showToast("Transcribed!");
                                        audioFile.delete();
                                    }
                                });
                            }

                            @Override
                            public void onError(final String error) {
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        showToast("Error: " + error);
                                        audioFile.delete();
                                    }
                                });
                            }
                        });
                    }
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        showToast("Recording error: " + error);
                        resetVoiceButton();
                    }
                });
            }
        });
    }

    private void resetVoiceButton() {
        isRecording = false;
        isPaused = false;
        voiceButton.setBackgroundColor(Color.parseColor("#424242"));
        voiceButton.setText("🎤");
        pauseButton.setVisibility(View.GONE);
    }

    private void showToast(final String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (audioRecorder != null) {
            audioRecorder.release();
        }
    }

    @Override
    public void onPress(int primaryCode) {}

    @Override
    public void onRelease(int primaryCode) {}

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {}

    @Override
    public void onText(CharSequence text) {}

    @Override
    public void swipeLeft() {}

    @Override
    public void swipeRight() {}

    @Override
    public void swipeDown() {}

    @Override
    public void swipeUp() {}
}
