package com.example.gamelog;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class NoteReminderDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TYPE = "extra_type";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_TEXT = "extra_text";
    public static final String EXTRA_GAME_TITLE = "extra_game_title";
    public static final String EXTRA_MEDIA_URI = "extra_media_uri";
    public static final String EXTRA_FREQUENCY = "extra_frequency";
    public static final String EXTRA_NEXT_TRIGGER_AT = "extra_next_trigger_at";
    public static final String EXTRA_STATUS = "extra_status";
    public static final String EXTRA_CREATED_AT = "extra_created_at";
    public static final String EXTRA_LATITUDE = "extra_latitude";
    public static final String EXTRA_LONGITUDE = "extra_longitude";
    public static final String EXTRA_TIMEZONE_ID = "extra_timezone_id";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_reminder_detail);

        ImageButton backButton = findViewById(R.id.note_detail_back_button);
        backButton.setOnClickListener(v -> finish());

        TextView headerTitle = findViewById(R.id.note_detail_title);
        TextView titleText = findViewById(R.id.note_detail_item_title);
        TextView gameText = findViewById(R.id.note_detail_game);
        TextView bodyText = findViewById(R.id.note_detail_body);
        View mediaCard = findViewById(R.id.note_detail_media_card);
        ImageView mediaPreview = findViewById(R.id.note_detail_media_preview);
        TextView mediaUriText = findViewById(R.id.note_detail_media_uri);
        TextView typeText = findViewById(R.id.note_detail_type);
        TextView frequencyText = findViewById(R.id.note_detail_frequency);
        TextView scheduleText = findViewById(R.id.note_detail_schedule);
        TextView statusText = findViewById(R.id.note_detail_status);
        TextView locationText = findViewById(R.id.note_detail_location);
        TextView timezoneText = findViewById(R.id.note_detail_timezone);
        TextView createdAtText = findViewById(R.id.note_detail_created_at);

        String type = normalize(getIntent().getStringExtra(EXTRA_TYPE));
        String title = normalize(getIntent().getStringExtra(EXTRA_TITLE));
        String noteText = normalize(getIntent().getStringExtra(EXTRA_TEXT));
        String gameTitle = normalize(getIntent().getStringExtra(EXTRA_GAME_TITLE));
        String mediaUri = normalize(getIntent().getStringExtra(EXTRA_MEDIA_URI));
        String frequency = normalize(getIntent().getStringExtra(EXTRA_FREQUENCY));
        String nextTriggerAt = normalize(getIntent().getStringExtra(EXTRA_NEXT_TRIGGER_AT));
        String status = normalize(getIntent().getStringExtra(EXTRA_STATUS));
        String createdAt = normalize(getIntent().getStringExtra(EXTRA_CREATED_AT));
        String latitude = normalize(getIntent().getStringExtra(EXTRA_LATITUDE));
        String longitude = normalize(getIntent().getStringExtra(EXTRA_LONGITUDE));
        String timezoneId = normalize(getIntent().getStringExtra(EXTRA_TIMEZONE_ID));

        boolean isReminder = "reminder".equalsIgnoreCase(type);
        headerTitle.setText(isReminder ? "Reminder Detail" : "Note Detail");
        titleText.setText(!TextUtils.isEmpty(title) ? title : (isReminder ? "Untitled reminder" : "Untitled note"));
        bodyText.setText(!TextUtils.isEmpty(noteText) ? noteText : (isReminder ? "No reminder text" : "No note text"));
        gameText.setText("Game: " + (!TextUtils.isEmpty(gameTitle) ? gameTitle : "Unknown game"));

        if (TextUtils.isEmpty(mediaUri)) {
            mediaCard.setVisibility(View.GONE);
        } else {
            mediaCard.setVisibility(View.VISIBLE);
            mediaUriText.setText(mediaUri);
            ImageLoader.loadCover(mediaPreview, mediaUri, R.drawable.ic_games);
        }

        typeText.setText("Type: " + (!TextUtils.isEmpty(type) ? type : "note"));

        if (isReminder && !TextUtils.isEmpty(frequency)) {
            frequencyText.setVisibility(View.VISIBLE);
            frequencyText.setText("Frequency: " + frequency);
        } else {
            frequencyText.setVisibility(View.GONE);
        }

        if (isReminder && !TextUtils.isEmpty(nextTriggerAt)) {
            scheduleText.setVisibility(View.VISIBLE);
            scheduleText.setText("Next trigger: " + nextTriggerAt);
        } else {
            scheduleText.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(status)) {
            statusText.setVisibility(View.VISIBLE);
            statusText.setText("Status: " + status);
        } else {
            statusText.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(latitude) && !TextUtils.isEmpty(longitude)) {
            locationText.setVisibility(View.VISIBLE);
            locationText.setText("Location: " + latitude + ", " + longitude);
        } else {
            locationText.setVisibility(View.GONE);
        }

        if (isReminder && !TextUtils.isEmpty(timezoneId)) {
            timezoneText.setVisibility(View.VISIBLE);
            timezoneText.setText("Timezone: " + timezoneId);
        } else {
            timezoneText.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(createdAt)) {
            createdAtText.setVisibility(View.VISIBLE);
            createdAtText.setText("Created: " + createdAt);
        } else {
            createdAtText.setVisibility(View.GONE);
        }
    }

    private String normalize(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
