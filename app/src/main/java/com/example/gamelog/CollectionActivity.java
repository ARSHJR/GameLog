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

public class CollectionActivity extends AppCompatActivity {

    public static final String EXTRA_USER_GAME_ID = "extra_user_game_id";
    public static final String EXTRA_GAME_ID = "extra_game_id";
    public static final String EXTRA_GAME_TITLE = "extra_game_title";

    private RecyclerView recyclerView;
    private CollectionAdapter adapter;
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
        setContentView(R.layout.activity_collection);

        recyclerView = findViewById(R.id.collection_recycler_view);
        loadingProgress = findViewById(R.id.collection_loading_progress);
        loadingText = findViewById(R.id.collection_loading_text);
        errorContainer = findViewById(R.id.collection_error_container);
        errorText = findViewById(R.id.collection_error_text);
        retryButton = findViewById(R.id.collection_retry_button);
        emptyContainer = findViewById(R.id.collection_empty_container);

        findViewById(R.id.collection_back_button).setOnClickListener(v -> finish());

        backendUserId = BackendUserHelper.getBackendUserId(this);
        if (TextUtils.isEmpty(backendUserId)) {
            showErrorState("Missing user context.");
            return;
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CollectionAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        adapter.setOnCollectionClickListener(item -> {
            if (TextUtils.isEmpty(item.getUserGameId())) {
                Toast.makeText(CollectionActivity.this, "This collection entry is missing an id.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(CollectionActivity.this, CollectionGameDetailActivity.class);
            intent.putExtra(CollectionGameDetailActivity.EXTRA_USER_GAME_ID, item.getUserGameId());
            intent.putExtra(CollectionGameDetailActivity.EXTRA_GAME_ID, item.getGameId());
            intent.putExtra(CollectionGameDetailActivity.EXTRA_GAME_TITLE, item.getTitle());
            startActivity(intent);
        });

        retryButton.setOnClickListener(v -> fetchCollection());
        fetchCollection();
    }

    private void fetchCollection() {
        showLoadingState();

        ApiService apiService = RetrofitClient.getApiService();
        Call<List<CollectionEntryItem>> call = apiService.getCollection(backendUserId);
        call.enqueue(new Callback<List<CollectionEntryItem>>() {
            @Override
            public void onResponse(Call<List<CollectionEntryItem>> call, Response<List<CollectionEntryItem>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    showErrorState("Could not load collection.");
                    Toast.makeText(CollectionActivity.this, "Failed to load collection", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<CollectionEntryItem> collectionEntries = response.body();
                adapter.updateItems(collectionEntries);

                if (collectionEntries.isEmpty()) {
                    showEmptyState();
                } else {
                    showContentState();
                }
            }

            @Override
            public void onFailure(Call<List<CollectionEntryItem>> call, Throwable t) {
                showErrorState("Unable to connect to server.");
                Toast.makeText(CollectionActivity.this, "Failed to load collection: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoadingState() {
        loadingProgress.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.VISIBLE);

        errorContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
    }

    private void showErrorState(String message) {
        loadingProgress.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);

        emptyContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        errorContainer.setVisibility(View.VISIBLE);
        errorText.setText(message);
    }

    private void showEmptyState() {
        loadingProgress.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);

        errorContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        emptyContainer.setVisibility(View.VISIBLE);
    }

    private void showContentState() {
        loadingProgress.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);

        errorContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);

        recyclerView.setVisibility(View.VISIBLE);
    }
}
