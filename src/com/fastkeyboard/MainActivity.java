package com.fastkeyboard;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

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
        mainLayout.setPadding(40, 60, 40, 40);
        mainLayout.setGravity(Gravity.CENTER);

        // App icon/logo
        TextView appIcon = new TextView(this);
        appIcon.setText("🎤");
        appIcon.setTextSize(72);
        appIcon.setGravity(Gravity.CENTER);
        appIcon.setPadding(0, 0, 0, 20);
        mainLayout.addView(appIcon);

        // App title with glow
        TextView title = new TextView(this);
        title.setText("VoiceKeyboard");
        title.setTextSize(32);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 10);
        title.setShadowLayer(12, 0, 0, Color.parseColor("#66FFFFFF"));
        mainLayout.addView(title);

        // Subtitle
        TextView subtitle = new TextView(this);
        subtitle.setText("Voice-only keyboard with AI");
        subtitle.setTextSize(16);
        subtitle.setTextColor(Color.parseColor("#AAAAAA"));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 40);
        mainLayout.addView(subtitle);

        // Description
        TextView description = new TextView(this);
        description.setText("A lightweight keyboard powered by OpenAI Whisper for voice transcription.\n\n" +
                "Features:\n" +
                "• Voice-only interface (no QWERTY)\n" +
                "• Whisper AI transcription\n" +
                "• Text improvement with ChatGPT\n" +
                "• Voice edit mode\n" +
                "• Recording history\n" +
                "• Multiple quality settings\n");
        description.setTextSize(14);
        description.setTextColor(Color.parseColor("#CCCCCC"));
        description.setPadding(0, 0, 0, 30);
        description.setLineSpacing(6, 1);
        mainLayout.addView(description);

        // Configure button
        Button configureBtn = createButton("⚙️ Configure Voice Settings", "#4CAF50");
        configureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
        mainLayout.addView(configureBtn);

        addVerticalSpace(mainLayout, 16);

        // Enable keyboard button
        Button enableBtn = createButton("⌨️ Enable Keyboard", "#2196F3");
        enableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
                startActivity(intent);
            }
        });
        mainLayout.addView(enableBtn);

        addVerticalSpace(mainLayout, 40);

        // Instructions
        TextView instructions = new TextView(this);
        instructions.setText("Setup Instructions:\n\n" +
                "1. Tap 'Configure Voice Settings'\n" +
                "2. Enter your OpenAI API key\n" +
                "3. Tap 'Enable Keyboard'\n" +
                "4. Enable 'Voice Keyboard'\n" +
                "5. In any app, select VoiceKeyboard\n" +
                "6. Tap '🎤 Start Recording' to begin!\n");
        instructions.setTextSize(12);
        instructions.setTextColor(Color.parseColor("#888888"));
        instructions.setLineSpacing(4, 1);
        mainLayout.addView(instructions);

        scrollView.addView(mainLayout);
        setContentView(scrollView);
    }

    private Button createButton(String text, String colorHex) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(16);
        button.setPadding(32, 20, 32, 20);
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
            ViewGroup.LayoutParams.MATCH_PARENT,
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

    private void addVerticalSpace(LinearLayout parent, int dp) {
        android.view.View space = new android.view.View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (int) (dp * getResources().getDisplayMetrics().density)
        );
        space.setLayoutParams(params);
        parent.addView(space);
    }
}
