package com.example.gamelog;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainShellActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private String currentTabEntityId;
    private long currentTabStartMs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_shell);

        bottomNavigationView = findViewById(R.id.main_bottom_nav);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            flushCurrentTabDuration();

            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                currentTabEntityId = "home_tab";
                currentTabStartMs = System.currentTimeMillis();
                switchToFragment(new HomeTabFragment());
                return true;
            }
            if (itemId == R.id.nav_notes_reminders) {
                currentTabEntityId = "notes_tab";
                currentTabStartMs = System.currentTimeMillis();
                switchToFragment(new NotesRemindersTabFragment());
                return true;
            }
            if (itemId == R.id.nav_user) {
                currentTabEntityId = "user_tab";
                currentTabStartMs = System.currentTimeMillis();
                switchToFragment(new UserTabFragment());
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    public void openUserTab() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_user);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (currentTabStartMs <= 0 && currentTabEntityId != null) {
            currentTabStartMs = System.currentTimeMillis();
        }
    }

    @Override
    protected void onStop() {
        flushCurrentTabDuration();
        super.onStop();
    }

    private void flushCurrentTabDuration() {
        if (currentTabEntityId == null || currentTabStartMs <= 0) {
            return;
        }

        long duration = System.currentTimeMillis() - currentTabStartMs;
        currentTabStartMs = 0;
        if (duration <= 0) {
            return;
        }

        UserActivityLogger.logDuration(
                this,
                "screen_view",
                "shell_tab",
                currentTabEntityId,
                "Main shell tab usage",
                duration
        );
    }

    private void switchToFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_shell_fragment_container, fragment)
                .commit();
    }
}
