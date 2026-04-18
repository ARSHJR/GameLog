package com.example.gamelog;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity that fetches and displays games from a REST API using Retrofit
 */
public class ApiGamesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private GameApiAdapter adapter;
    private ProgressBar loadingProgress;
    private TextView loadingText;
    private View errorContainer;
    private TextView errorText;
    private Button retryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api_games);

        recyclerView = findViewById(R.id.api_games_recycler_view);
        loadingProgress = findViewById(R.id.loading_progress);
        loadingText = findViewById(R.id.loading_text);
        errorContainer = findViewById(R.id.error_container);
        errorText = findViewById(R.id.error_text);
        retryButton = findViewById(R.id.retry_button);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GameApiAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        adapter.setOnGameClickListener(game -> {
            String gameId = game.getGameId();
            if (TextUtils.isEmpty(gameId)) {
                Toast.makeText(ApiGamesActivity.this, "This game is missing an id.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(ApiGamesActivity.this, GameDetailActivity.class);
            intent.putExtra(GameDetailActivity.EXTRA_GAME_ID, gameId);
            startActivity(intent);
        });

        retryButton.setOnClickListener(v -> fetchGames());

        fetchGames();
    }

    private void fetchGames() {
        showLoadingState();

        ApiService apiService = RetrofitClient.getApiService();
        Call<List<GameApiItem>> call = apiService.getGames();

        call.enqueue(new Callback<List<GameApiItem>>() {
            @Override
            public void onResponse(Call<List<GameApiItem>> call, Response<List<GameApiItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<GameApiItem> games = response.body();
                    adapter.updateGames(games);
                    showContentState();
                } else {
                    showErrorState("Could not load games right now.");
                    Toast.makeText(ApiGamesActivity.this, "Failed to load games", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<GameApiItem>> call, Throwable t) {
                showErrorState("Unable to connect to server.");
                Toast.makeText(ApiGamesActivity.this, "Failed to load games: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoadingState() {
        loadingProgress.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.VISIBLE);
        loadingText.setText("Syncing latest releases...");
        recyclerView.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
    }

    private void showContentState() {
        loadingProgress.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void showErrorState(String message) {
        loadingProgress.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        errorContainer.setVisibility(View.VISIBLE);
        errorText.setText(message);
    }
}
