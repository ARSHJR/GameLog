package com.example.gamelog;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainShellActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_shell);

        bottomNavigationView = findViewById(R.id.main_bottom_nav);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                switchToFragment(new HomeTabFragment());
                return true;
            }
            if (itemId == R.id.nav_notes_reminders) {
                switchToFragment(new NotesRemindersTabFragment());
                return true;
            }
            if (itemId == R.id.nav_user) {
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

    private void switchToFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_shell_fragment_container, fragment)
                .commit();
    }
}
