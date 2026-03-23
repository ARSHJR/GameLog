package com.example.gamelog;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class GamesActivity extends AppCompatActivity {

    private ArrayList<GameItem> gameList = new ArrayList<>();
    private GameAdapter adapter;
    private TextView totalGames;
    private LinearLayout emptyState;
    private EditText titleInput;
    private EditText notesInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_games);

        totalGames = findViewById(R.id.total_games);
        emptyState = findViewById(R.id.empty_state);
        titleInput = findViewById(R.id.game_title_input);
        notesInput = findViewById(R.id.game_notes_input);

        RecyclerView recyclerView = findViewById(R.id.games_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GameAdapter(gameList);
        recyclerView.setAdapter(adapter);

        Button saveButton = findViewById(R.id.save_game_button);

        // Click listener
        saveButton.setOnClickListener(v -> saveGame(false));

        // Long-click listener
        saveButton.setOnLongClickListener(v -> {
            saveGame(true);
            return true;
        });

        adapter.setOnItemClickListener(position -> {
            GameItem item = gameList.get(position);
            new AlertDialog.Builder(this)
                    .setTitle("Game details")
                    .setMessage("Title: " + item.getTitle() + "\nNote: " + item.getNotes() + "\nStatus: " + item.getStatus())
                    .setPositiveButton("OK", null)
                    .show();
        });

        adapter.setOnItemLongClickListener(position -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete game")
                    .setMessage("Do you want to delete this entry?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        gameList.remove(position);
                        adapter.notifyItemRemoved(position);
                        updateGameCount();
                        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Swipe handler
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                GameItem item = gameList.get(position);
                if (direction == ItemTouchHelper.RIGHT) {
                    item.setStatus("Completed");
                    Toast.makeText(GamesActivity.this, "Status set to Completed", Toast.LENGTH_SHORT).show();
                } else {
                    item.setStatus("In progress");
                    Toast.makeText(GamesActivity.this, "Status set to In progress", Toast.LENGTH_SHORT).show();
                }
                adapter.notifyItemChanged(position);
            }
        }).attachToRecyclerView(recyclerView);

        updateGameCount();
    }

    private void saveGame(boolean isCompleted) {
        String title = titleInput.getText().toString();
        if (title.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        String notes = notesInput.getText().toString();
        String status = isCompleted ? "Completed" : "In progress";
        gameList.add(new GameItem(title, notes, status));
        adapter.notifyItemInserted(gameList.size() - 1);
        updateGameCount();
        titleInput.setText("");
        notesInput.setText("");
        Toast.makeText(this, isCompleted ? "Saved as completed" : "Game saved", Toast.LENGTH_SHORT).show();
    }

    private void updateGameCount() {
        totalGames.setText("Total games: " + gameList.size());
        if (gameList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
        }
    }
}
