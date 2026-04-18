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

public class FavouritesActivity extends AppCompatActivity {

    private RecyclerView favouritesRecyclerView;
    private FavouriteGameAdapter adapter;

    private ProgressBar loadingProgress;
    private TextView loadingText;
    private View errorContainer;
    private TextView errorText;
    private Button retryButton;
    private View emptyContainer;

    private String backendUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourites);

        favouritesRecyclerView = findViewById(R.id.favourites_recycler_view);
        loadingProgress = findViewById(R.id.favourites_loading_progress);
        loadingText = findViewById(R.id.favourites_loading_text);
        errorContainer = findViewById(R.id.favourites_error_container);
        errorText = findViewById(R.id.favourites_error_text);
        retryButton = findViewById(R.id.favourites_retry_button);
        emptyContainer = findViewById(R.id.favourites_empty_container);

        findViewById(R.id.favourites_back_button).setOnClickListener(v -> finish());

        backendUserId = BackendUserHelper.getBackendUserId(this);
        if (TextUtils.isEmpty(backendUserId)) {
            showErrorState("Missing user context.");
            return;
        }

        favouritesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FavouriteGameAdapter(new ArrayList<>());
        favouritesRecyclerView.setAdapter(adapter);

        adapter.setOnFavouriteClickListener(item -> {
            String gameId = item.getGameId();
            if (TextUtils.isEmpty(gameId)) {
                Toast.makeText(FavouritesActivity.this, "This game is missing an id.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(FavouritesActivity.this, GameDetailActivity.class);
            intent.putExtra(GameDetailActivity.EXTRA_GAME_ID, gameId);
            startActivity(intent);
        });

        retryButton.setOnClickListener(v -> fetchFavourites());

        fetchFavourites();
    }

    private void fetchFavourites() {
        showLoadingState();

        ApiService apiService = RetrofitClient.getApiService();
        Call<List<FavouriteGameItem>> call = apiService.getFavourites(backendUserId);
        call.enqueue(new Callback<List<FavouriteGameItem>>() {
            @Override
            public void onResponse(Call<List<FavouriteGameItem>> call, Response<List<FavouriteGameItem>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    showErrorState("Could not load favourites.");
                    Toast.makeText(FavouritesActivity.this, "Failed to load favourites", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<FavouriteGameItem> favourites = response.body();
                adapter.updateItems(favourites);

                if (favourites.isEmpty()) {
                    showEmptyState();
                } else {
                    showContentState();
                }
            }

            @Override
            public void onFailure(Call<List<FavouriteGameItem>> call, Throwable t) {
                showErrorState("Unable to connect to server.");
                Toast.makeText(FavouritesActivity.this, "Failed to load favourites: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoadingState() {
        loadingProgress.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.VISIBLE);

        errorContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);
        favouritesRecyclerView.setVisibility(View.GONE);
    }

    private void showErrorState(String message) {
        loadingProgress.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);

        emptyContainer.setVisibility(View.GONE);
        favouritesRecyclerView.setVisibility(View.GONE);

        errorContainer.setVisibility(View.VISIBLE);
        errorText.setText(message);
    }

    private void showEmptyState() {
        loadingProgress.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);

        errorContainer.setVisibility(View.GONE);
        favouritesRecyclerView.setVisibility(View.GONE);

        emptyContainer.setVisibility(View.VISIBLE);
    }

    private void showContentState() {
        loadingProgress.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);

        errorContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);

        favouritesRecyclerView.setVisibility(View.VISIBLE);
    }
}
