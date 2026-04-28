package com.example.gamelog;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity that fetches and displays games from a REST API using Retrofit
 */
public class ApiGamesActivity extends AppCompatActivity {

    private static final float SHAKE_THRESHOLD_G = 2.5f;
    private static final long SHAKE_COOLDOWN_MS = 2000;

    private RecyclerView recyclerView;
    private GameApiAdapter adapter;
    private ProgressBar loadingProgress;
    private TextView loadingText;
    private View errorContainer;
    private TextView errorText;
    private Button retryButton;
    private long screenStartMs;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeMs = 0;
    private AlertDialog randomPickDialog;

    private final SensorEventListener shakeListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            float gForce = (float) Math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;
            if (gForce > SHAKE_THRESHOLD_G) {
                long now = System.currentTimeMillis();
                if (now - lastShakeMs > SHAKE_COOLDOWN_MS) {
                    lastShakeMs = now;
                    showRandomPickDialog();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    };

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

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        fetchGames();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(shakeListener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(shakeListener);
        }
        if (randomPickDialog != null && randomPickDialog.isShowing()) {
            randomPickDialog.dismiss();
        }
        super.onPause();
    }

    private void showRandomPickDialog() {
        List<GameApiItem> games = adapter.getGames();
        if (games.isEmpty()) {
            Toast.makeText(this, "No games loaded yet — try shaking once the list appears!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (randomPickDialog != null && randomPickDialog.isShowing()) {
            randomPickDialog.dismiss();
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_random_game, null);
        TextView titleView = dialogView.findViewById(R.id.dialog_pick_title);
        TextView genresView = dialogView.findViewById(R.id.dialog_pick_genres);
        TextView metaView = dialogView.findViewById(R.id.dialog_pick_meta);
        MaterialButton viewButton = dialogView.findViewById(R.id.dialog_pick_view_game);
        MaterialButton rollButton = dialogView.findViewById(R.id.dialog_pick_roll_again);

        final GameApiItem[] currentPick = { pickRandom(games) };
        bindPickedGame(titleView, genresView, metaView, currentPick[0]);

        randomPickDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        viewButton.setOnClickListener(v -> {
            randomPickDialog.dismiss();
            String gameId = currentPick[0].getGameId();
            if (TextUtils.isEmpty(gameId)) {
                Toast.makeText(this, "This game is missing an id.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, GameDetailActivity.class);
            intent.putExtra(GameDetailActivity.EXTRA_GAME_ID, gameId);
            startActivity(intent);
        });

        rollButton.setOnClickListener(v -> {
            List<GameApiItem> refreshed = adapter.getGames();
            if (!refreshed.isEmpty()) {
                currentPick[0] = pickRandom(refreshed);
                bindPickedGame(titleView, genresView, metaView, currentPick[0]);
            }
        });

        randomPickDialog.show();
    }

    private GameApiItem pickRandom(List<GameApiItem> games) {
        return games.get(new Random().nextInt(games.size()));
    }

    private void bindPickedGame(TextView titleView, TextView genresView, TextView metaView, GameApiItem game) {
        String title = game.getTitle();
        titleView.setText(isBlank(title) ? "Untitled game" : title.trim());

        List<String> genres = game.getGenres();
        if (genres == null || genres.isEmpty()) {
            genresView.setVisibility(View.GONE);
        } else {
            genresView.setVisibility(View.VISIBLE);
            genresView.setText(joinGenres(genres));
        }

        String meta = buildMetaLine(game);
        metaView.setText(meta);
    }

    private static String buildMetaLine(GameApiItem game) {
        boolean hasDate = !isBlank(game.getReleaseDate());
        boolean hasPlatform = !isBlank(game.getPlatform());
        if (hasDate && hasPlatform) return game.getReleaseDate().trim() + "  |  " + game.getPlatform().trim();
        if (hasDate) return game.getReleaseDate().trim();
        if (hasPlatform) return game.getPlatform().trim();
        return "";
    }

    private static String joinGenres(List<String> genres) {
        StringBuilder sb = new StringBuilder();
        for (String g : genres) {
            if (isBlank(g)) continue;
            if (sb.length() > 0) sb.append("  |  ");
            sb.append(g.trim());
        }
        return sb.toString();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
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

    @Override
    protected void onStart() {
        super.onStart();
        screenStartMs = System.currentTimeMillis();
    }

    @Override
    protected void onStop() {
        if (screenStartMs > 0) {
            UserActivityLogger.logDuration(
                    this,
                    "screen_view",
                    "screen",
                    "explore",
                    "Explore games",
                    System.currentTimeMillis() - screenStartMs
            );
            screenStartMs = 0;
        }
        super.onStop();
    }
}
