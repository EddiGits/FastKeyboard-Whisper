package com.fastkeyboard;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class TemplatesActivity extends Activity {
    private static final String PREFS_NAME = "VoiceKeyboardPrefs";
    private static final String KEY_TEMPLATES = "templates";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.parseColor("#1E1E1E"));
        scrollView.setFillViewport(true);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(24, 24, 24, 24);
        mainLayout.setBackgroundColor(Color.parseColor("#1E1E1E"));

        // Title
        TextView title = new TextView(this);
        title.setText("📝 Templates");
        title.setTextSize(24);
        title.setTextColor(Color.WHITE);
        title.setPadding(0, 0, 0, 24);
        title.setGravity(Gravity.CENTER);
        mainLayout.addView(title);

        // Add new template section
        final EditText nameInput = new EditText(this);
        nameInput.setHint("Template name (e.g., Greeting)");
        nameInput.setTextColor(Color.WHITE);
        nameInput.setHintTextColor(Color.parseColor("#666666"));
        nameInput.setBackgroundColor(Color.parseColor("#2C2C2C"));
        nameInput.setPadding(16, 16, 16, 16);
        nameInput.setSingleLine(true);
        mainLayout.addView(nameInput);

        addVerticalSpace(mainLayout, 8);

        final EditText textInput = new EditText(this);
        textInput.setHint("Template text (e.g., Hello! How are you?)");
        textInput.setTextColor(Color.WHITE);
        textInput.setHintTextColor(Color.parseColor("#666666"));
        textInput.setBackgroundColor(Color.parseColor("#2C2C2C"));
        textInput.setPadding(16, 16, 16, 16);
        textInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        textInput.setMinLines(3);
        mainLayout.addView(textInput);

        addVerticalSpace(mainLayout, 12);

        Button addBtn = createButton("➕ Add Template", "#4CAF50");
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nameInput.getText().toString().trim();
                String text = textInput.getText().toString().trim();

                if (name.isEmpty() || text.isEmpty()) {
                    Toast.makeText(TemplatesActivity.this, "Please fill both fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                addTemplate(name, text);
                nameInput.setText("");
                textInput.setText("");
                recreate();
            }
        });
        mainLayout.addView(addBtn);

        addVerticalSpace(mainLayout, 24);

        // Templates list
        final ScrollView templatesScroll = new ScrollView(this);
        templatesScroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1.0f
        ));

        final LinearLayout templatesContainer = new LinearLayout(this);
        templatesContainer.setOrientation(LinearLayout.VERTICAL);
        templatesScroll.addView(templatesContainer);

        loadTemplates(templatesContainer);

        mainLayout.addView(templatesScroll);

        addVerticalSpace(mainLayout, 16);

        // Close button
        Button closeBtn = createButton("Close", "#607D8B");
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mainLayout.addView(closeBtn);

        scrollView.addView(mainLayout);
        setContentView(scrollView);
    }

    private void loadTemplates(LinearLayout container) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String templates = prefs.getString(KEY_TEMPLATES, "");

        if (templates.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No templates yet\n\nCreate templates for phrases you use often!");
            emptyText.setTextColor(Color.parseColor("#666666"));
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, 40, 0, 0);
            container.addView(emptyText);
            return;
        }

        String[] entries = templates.split("\n===TEMPLATE===\n");

        for (int i = 0; i < entries.length; i++) {
            String entry = entries[i];
            if (entry.trim().isEmpty()) continue;

            String[] parts = entry.split("\\|\\|\\|", 2);
            if (parts.length < 2) continue;

            final String name = parts[0];
            final String text = parts[1];
            final int index = i;

            addTemplateCard(container, name, text, index);
        }
    }

    private void addTemplateCard(LinearLayout container, final String name, final String text, final int index) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#2C2C2C"));
        card.setPadding(16, 16, 16, 16);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor("#2C2C2C"));
        drawable.setCornerRadius(8);
        card.setBackground(drawable);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 12);
        card.setLayoutParams(cardParams);

        // Name
        TextView nameText = new TextView(this);
        nameText.setText(name);
        nameText.setTextColor(Color.parseColor("#4CAF50"));
        nameText.setTextSize(16);
        nameText.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(nameText);

        // Text preview
        TextView textPreview = new TextView(this);
        textPreview.setText(text.length() > 100 ? text.substring(0, 100) + "..." : text);
        textPreview.setTextColor(Color.WHITE);
        textPreview.setTextSize(14);
        textPreview.setPadding(0, 8, 0, 12);
        card.addView(textPreview);

        // Buttons
        LinearLayout buttonsRow = new LinearLayout(this);
        buttonsRow.setOrientation(LinearLayout.HORIZONTAL);

        Button copyBtn = createButton("📋 Copy", "#4CAF50");
        copyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("template", text);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(TemplatesActivity.this, "Template copied to clipboard!", Toast.LENGTH_SHORT).show();
            }
        });
        buttonsRow.addView(copyBtn);

        addSpace(buttonsRow, 8);

        Button deleteBtn = createButton("🗑 Delete", "#f44336");
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteTemplate(index);
                recreate();
            }
        });
        buttonsRow.addView(deleteBtn);

        card.addView(buttonsRow);
        container.addView(card);
    }

    private void addTemplate(String name, String text) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String templates = prefs.getString(KEY_TEMPLATES, "");

        String entry = name + "|||" + text;

        if (!templates.isEmpty()) {
            templates += "\n===TEMPLATE===\n";
        }
        templates += entry;

        prefs.edit().putString(KEY_TEMPLATES, templates).apply();
        Toast.makeText(this, "Template added!", Toast.LENGTH_SHORT).show();
    }

    private void deleteTemplate(int index) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String templates = prefs.getString(KEY_TEMPLATES, "");

        String[] entries = templates.split("\n===TEMPLATE===\n");
        StringBuilder newTemplates = new StringBuilder();

        for (int i = 0; i < entries.length; i++) {
            if (i != index && !entries[i].trim().isEmpty()) {
                if (newTemplates.length() > 0) {
                    newTemplates.append("\n===TEMPLATE===\n");
                }
                newTemplates.append(entries[i]);
            }
        }

        prefs.edit().putString(KEY_TEMPLATES, newTemplates.toString()).apply();
        Toast.makeText(this, "Template deleted", Toast.LENGTH_SHORT).show();
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
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        button.setLayoutParams(params);

        return button;
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
