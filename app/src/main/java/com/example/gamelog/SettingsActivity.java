package com.example.gamelog;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        // Updated IDs to match the revamped layout
        TextView securityText = findViewById(R.id.security_text);
        TextView helpText = findViewById(R.id.help_text);
        TextView aboutText = findViewById(R.id.about_text);

        securityText.setOnClickListener(v -> Toast.makeText(this, "Security clicked", Toast.LENGTH_SHORT).show());
        helpText.setOnClickListener(v -> Toast.makeText(this, "Help clicked", Toast.LENGTH_SHORT).show());
        aboutText.setOnClickListener(v -> Toast.makeText(this, "About clicked", Toast.LENGTH_SHORT).show());
    }
}
