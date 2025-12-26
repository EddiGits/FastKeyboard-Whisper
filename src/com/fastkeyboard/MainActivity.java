package com.fastkeyboard;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.view.Gravity;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#2C2C2C"));
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(40, 40, 40, 40);

        TextView title = new TextView(this);
        title.setText("FastKeyboard");
        title.setTextSize(28);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView instructions = new TextView(this);
        instructions.setText("\n\nTo enable FastKeyboard:\n\n" +
            "1. Go to Settings\n" +
            "2. System → Languages & input\n" +
            "3. On-screen keyboard\n" +
            "4. Manage keyboards\n" +
            "5. Enable 'Fast Keyboard'\n\n" +
            "Then tap any text field and select FastKeyboard!");
        instructions.setTextSize(16);
        instructions.setTextColor(Color.WHITE);
        instructions.setGravity(Gravity.CENTER);
        layout.addView(instructions);

        // Settings button
        android.widget.Button settingsBtn = new android.widget.Button(this);
        settingsBtn.setText("⚙️ Configure Voice Typing");
        settingsBtn.setTextSize(18);
        settingsBtn.setTextColor(Color.WHITE);
        settingsBtn.setBackgroundColor(Color.parseColor("#4CAF50"));
        settingsBtn.setPadding(40, 40, 40, 40);
        android.widget.LinearLayout.LayoutParams btnParams = new android.widget.LinearLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );
        btnParams.setMargins(0, 40, 0, 0);
        settingsBtn.setLayoutParams(btnParams);
        settingsBtn.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                android.content.Intent intent = new android.content.Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
        layout.addView(settingsBtn);

        setContentView(layout);
    }
}
