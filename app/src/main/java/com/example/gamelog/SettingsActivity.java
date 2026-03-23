package com.example.gamelog;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        MaterialCardView securityCard = findViewById(R.id.security_card);
        MaterialCardView helpCard = findViewById(R.id.help_card);
        MaterialCardView aboutCard = findViewById(R.id.about_card);

        securityCard.setOnClickListener(v -> Toast.makeText(this, "Security clicked", Toast.LENGTH_SHORT).show());
        helpCard.setOnClickListener(v -> Toast.makeText(this, "Help clicked", Toast.LENGTH_SHORT).show());
        aboutCard.setOnClickListener(v -> Toast.makeText(this, "About clicked", Toast.LENGTH_SHORT).show());
    }
}
