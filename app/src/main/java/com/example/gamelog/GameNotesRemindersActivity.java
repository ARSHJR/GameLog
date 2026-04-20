package com.example.gamelog;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class GameNotesRemindersActivity extends AppCompatActivity {

    public static final String EXTRA_USER_GAME_ID = "extra_user_game_id";
    public static final String EXTRA_GAME_TITLE = "extra_game_title";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_notes_reminders);

        if (savedInstanceState != null) {
            return;
        }

        String userGameId = getIntent().getStringExtra(EXTRA_USER_GAME_ID);
        String gameTitle = getIntent().getStringExtra(EXTRA_GAME_TITLE);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.game_notes_reminders_container,
                        NotesRemindersTabFragment.newInstance(userGameId, gameTitle)
                )
                .commit();
    }
}
