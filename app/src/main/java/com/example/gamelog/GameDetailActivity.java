package com.example.gamelog;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GameDetailActivity extends AppCompatActivity {

    public static final String EXTRA_GAME_ID = "extra_game_id";
    public static final String EXTRA_USER_GAME_ID = "extra_user_game_id";
    public static final String EXTRA_GAME_TITLE = "extra_game_title";

    private String gameId;
    private String userGameId;
    private String gameTitleHint;
    private String backendUserId;

    private ProgressBar loadingProgress;
    private TextView loadingText;
    private View errorContainer;
    private TextView errorText;
    private Button retryButton;
    private NestedScrollView contentContainer;

    private TextView titleText;
    private TextView genresText;
    private TextView descriptionText;
    private TextView releaseDateValue;
    private TextView developerValue;
    private TextView publisherValue;
    private TextView platformValue;
    private TextView actionStatusText;
    private ImageView heroImage;
    private TextView heroCaption;
    private MaterialButton addToCollectionButton;
    private MaterialButton favouriteButton;

    private View collectionNotesCard;
    private TextView collectionNotesTitle;
    private ProgressBar collectionNotesLoading;
    private TextView collectionNotesError;
    private TextView collectionNotesEmpty;
    private RecyclerView collectionNotesRecycler;
    private MaterialButton manageCollectionNotesButton;
    private DetailNotesPreviewAdapter detailNotesPreviewAdapter;

    private boolean isCollectionRequestInFlight;
    private boolean isFavouriteRequestInFlight;
    private boolean isFavouriteSelected;
    private boolean fromCollectionContext;
    private long screenStartMs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_detail);

        loadingProgress = findViewById(R.id.detail_loading_progress);
        loadingText = findViewById(R.id.detail_loading_text);
        errorContainer = findViewById(R.id.detail_error_container);
        errorText = findViewById(R.id.detail_error_text);
        retryButton = findViewById(R.id.detail_retry_button);
        contentContainer = findViewById(R.id.detail_content_container);

        titleText = findViewById(R.id.detail_title);
        genresText = findViewById(R.id.detail_genres);
        descriptionText = findViewById(R.id.detail_description);
        releaseDateValue = findViewById(R.id.detail_release_date_value);
        developerValue = findViewById(R.id.detail_developer_value);
        publisherValue = findViewById(R.id.detail_publisher_value);
        platformValue = findViewById(R.id.detail_platform_value);
        actionStatusText = findViewById(R.id.detail_action_status);
        heroImage = findViewById(R.id.detail_hero_image);
        heroCaption = findViewById(R.id.detail_hero_caption);
        addToCollectionButton = findViewById(R.id.detail_add_collection_button);
        favouriteButton = findViewById(R.id.detail_favourite_button);
        collectionNotesCard = findViewById(R.id.detail_collection_notes_card);
        collectionNotesTitle = findViewById(R.id.detail_collection_notes_title);
        collectionNotesLoading = findViewById(R.id.detail_collection_notes_loading);
        collectionNotesError = findViewById(R.id.detail_collection_notes_error);
        collectionNotesEmpty = findViewById(R.id.detail_collection_notes_empty);
        collectionNotesRecycler = findViewById(R.id.detail_collection_notes_recycler);
        manageCollectionNotesButton = findViewById(R.id.detail_manage_collection_notes_button);

        collectionNotesRecycler.setLayoutManager(new LinearLayoutManager(this));
        detailNotesPreviewAdapter = new DetailNotesPreviewAdapter(new ArrayList<>());
        collectionNotesRecycler.setAdapter(detailNotesPreviewAdapter);

        findViewById(R.id.detail_back_button).setOnClickListener(v -> finish());

        gameId = getIntent().getStringExtra(EXTRA_GAME_ID);
        userGameId = getIntent().getStringExtra(EXTRA_USER_GAME_ID);
        gameTitleHint = getIntent().getStringExtra(EXTRA_GAME_TITLE);
        backendUserId = BackendUserHelper.getBackendUserId(this);
        if (TextUtils.isEmpty(gameId)) {
            showErrorState("Missing game id.");
            return;
        }

        if (TextUtils.isEmpty(backendUserId)) {
            showErrorState("Missing user context.");
            return;
        }

        retryButton.setOnClickListener(v -> fetchGameDetails());
        manageCollectionNotesButton.setOnClickListener(v -> openCollectionNotesManager());
        addToCollectionButton.setOnClickListener(v -> addGameToCollection());
        favouriteButton.setOnClickListener(v -> toggleFavourite());
        updateActionButtonsState();
        updateFavouriteVisualState();
        setupContextBehavior();
        setActionStatus("Ready", R.color.text_med_emp);
        fetchGameDetails();
    }

    private void setupContextBehavior() {
        fromCollectionContext = !TextUtils.isEmpty(userGameId);
        if (fromCollectionContext) {
            addToCollectionButton.setText("In Collection");
            addToCollectionButton.setEnabled(false);
            addToCollectionButton.setAlpha(0.7f);

            collectionNotesCard.setVisibility(View.VISIBLE);
            collectionNotesTitle.setText("Collection notes & reminders");
            if (!TextUtils.isEmpty(gameTitleHint)) {
                heroCaption.setText(gameTitleHint);
            }
        } else {
            collectionNotesCard.setVisibility(View.GONE);
            heroCaption.setText("Game landing view");
        }
    }

    private void fetchGameDetails() {
        showLoadingState();

        ApiService apiService = RetrofitClient.getApiService();
        Call<GameApiItem> call = apiService.getGameById(gameId);
        call.enqueue(new Callback<GameApiItem>() {
            @Override
            public void onResponse(Call<GameApiItem> call, Response<GameApiItem> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bindGame(response.body());
                    if (!TextUtils.isEmpty(userGameId)) {
                        fetchCollectionNotesPreview();
                    }
                    showContentState();
                } else {
                    showErrorState("Could not load game details.");
                    Toast.makeText(GameDetailActivity.this, "Failed to load game details", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GameApiItem> call, Throwable t) {
                showErrorState("Unable to connect to server.");
                Toast.makeText(GameDetailActivity.this, "Failed to load game details: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindGame(GameApiItem game) {
        titleText.setText(orFallback(game.getTitle(), "Untitled game"));

        List<String> genres = game.getGenres();
        if (genres == null || genres.isEmpty()) {
            genresText.setText("Genres unavailable");
        } else {
            genresText.setText(joinGenres(genres));
        }

        descriptionText.setText(orFallback(game.getDescription(), "No description available for this title yet."));
        releaseDateValue.setText(orFallback(game.getReleaseDate(), "Unknown"));
        developerValue.setText(orFallback(game.getDeveloper(), "Unknown"));
        publisherValue.setText(orFallback(game.getPublisher(), "Unknown"));
        platformValue.setText(orFallback(game.getPlatform(), "Unknown"));

        ImageLoader.loadCover(heroImage, game.getCoverImageUrl(), R.drawable.ic_games);

        if (TextUtils.isEmpty(gameTitleHint)) {
            heroCaption.setText(orFallback(game.getTitle(), "Game landing view"));
        }
    }

    private void fetchCollectionNotesPreview() {
        if (TextUtils.isEmpty(userGameId)) {
            return;
        }

        showCollectionNotesLoading();

        ApiService apiService = RetrofitClient.getApiService();
        apiService.getCollectionNotes(userGameId, null).enqueue(new Callback<List<CollectionNoteItem>>() {
            @Override
            public void onResponse(Call<List<CollectionNoteItem>> call, Response<List<CollectionNoteItem>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    showCollectionNotesError("Could not load collection notes/reminders.");
                    return;
                }

                List<CollectionNoteItem> items = new ArrayList<>(response.body());
                if (items.isEmpty()) {
                    showCollectionNotesEmpty();
                    return;
                }

                sortNotesPinnedFirst(items);
                detailNotesPreviewAdapter.updateItems(items);
                showCollectionNotesContent();
            }

            @Override
            public void onFailure(Call<List<CollectionNoteItem>> call, Throwable t) {
                showCollectionNotesError("Unable to load collection notes/reminders.");
            }
        });
    }

    private void openCollectionNotesManager() {
        if (TextUtils.isEmpty(userGameId)) {
            return;
        }

        Intent intent = new Intent(this, GameNotesRemindersActivity.class);
        intent.putExtra(GameNotesRemindersActivity.EXTRA_USER_GAME_ID, userGameId);
        intent.putExtra(GameNotesRemindersActivity.EXTRA_GAME_TITLE, titleText.getText() != null ? titleText.getText().toString() : gameTitleHint);
        startActivity(intent);
    }

    private void addGameToCollection() {
        if (isCollectionRequestInFlight || TextUtils.isEmpty(gameId) || TextUtils.isEmpty(backendUserId)) {
            return;
        }

        isCollectionRequestInFlight = true;
        updateActionButtonsState();
        addToCollectionButton.setText("Adding...");
        setActionStatus("Adding game to your collection...", R.color.text_med_emp);

        ApiService apiService = RetrofitClient.getApiService();
        AddToCollectionRequest request = new AddToCollectionRequest(gameId, "planned");
        apiService.addToCollection(backendUserId, request).enqueue(new Callback<AddToCollectionResponse>() {
            @Override
            public void onResponse(Call<AddToCollectionResponse> call, Response<AddToCollectionResponse> response) {
                isCollectionRequestInFlight = false;
                addToCollectionButton.setText("Add to Collection");
                updateActionButtonsState();

                if (!response.isSuccessful()) {
                    setActionStatus("Collection update failed.", R.color.status_error_soft);
                    Toast.makeText(GameDetailActivity.this, "Failed to add to collection", Toast.LENGTH_SHORT).show();
                    return;
                }

                AddToCollectionResponse body = response.body();
                UserGameEntryResponse entry = body != null ? body.getEntry() : null;
                Boolean wasCreated = body != null ? body.getCreated() : null;
                if (entry != null && entry.getIsFavourite() != null) {
                    isFavouriteSelected = entry.getIsFavourite();
                    updateFavouriteVisualState();
                }

                if (Boolean.TRUE.equals(wasCreated)) {
                    setActionStatus("Added to collection.", R.color.status_success);
                    Toast.makeText(GameDetailActivity.this, "Added to collection", Toast.LENGTH_SHORT).show();
                } else {
                    setActionStatus("Already in collection.", R.color.text_soft);
                    Toast.makeText(GameDetailActivity.this, "Already in collection", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AddToCollectionResponse> call, Throwable t) {
                isCollectionRequestInFlight = false;
                addToCollectionButton.setText("Add to Collection");
                updateActionButtonsState();
                setActionStatus("Could not reach server.", R.color.status_error_soft);
                Toast.makeText(GameDetailActivity.this, "Failed to add to collection: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleFavourite() {
        if (isFavouriteRequestInFlight || TextUtils.isEmpty(gameId) || TextUtils.isEmpty(backendUserId)) {
            return;
        }

        isFavouriteRequestInFlight = true;
        updateActionButtonsState();
        favouriteButton.setText("Updating...");
        setActionStatus("Updating favourite...", R.color.text_med_emp);

        ApiService apiService = RetrofitClient.getApiService();
        apiService.toggleFavourite(backendUserId, gameId).enqueue(new Callback<FavouriteToggleResponse>() {
            @Override
            public void onResponse(Call<FavouriteToggleResponse> call, Response<FavouriteToggleResponse> response) {
                isFavouriteRequestInFlight = false;
                updateActionButtonsState();

                if (!response.isSuccessful()) {
                    favouriteButton.setText(isFavouriteSelected ? "Favourited" : "Favourite");
                    setActionStatus("Favourite update failed.", R.color.status_error_soft);
                    Toast.makeText(GameDetailActivity.this, "Failed to update favourite", Toast.LENGTH_SHORT).show();
                    return;
                }

                FavouriteToggleResponse body = response.body();
                if (body != null && body.getIsFavourite() != null) {
                    isFavouriteSelected = body.getIsFavourite();
                    updateFavouriteVisualState();
                    setActionStatus(isFavouriteSelected ? "Marked as favourite." : "Removed from favourites.", R.color.status_success);
                    Toast.makeText(GameDetailActivity.this, isFavouriteSelected ? "Added to favourites" : "Removed from favourites", Toast.LENGTH_SHORT).show();
                } else {
                    // Conservative handling: only update local state when backend explicitly returns is_favourite.
                    favouriteButton.setText(isFavouriteSelected ? "Favourited" : "Favourite");
                    setActionStatus("Favourite updated on server.", R.color.text_soft);
                    Toast.makeText(GameDetailActivity.this, "Favourite updated", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FavouriteToggleResponse> call, Throwable t) {
                isFavouriteRequestInFlight = false;
                updateActionButtonsState();
                favouriteButton.setText(isFavouriteSelected ? "Favourited" : "Favourite");
                setActionStatus("Could not reach server.", R.color.status_error_soft);
                Toast.makeText(GameDetailActivity.this, "Failed to update favourite: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoadingState() {
        loadingProgress.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.VISIBLE);
        errorContainer.setVisibility(View.GONE);
        contentContainer.setVisibility(View.GONE);
    }

    private void showContentState() {
        loadingProgress.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        contentContainer.setVisibility(View.VISIBLE);
    }

    private void showErrorState(String message) {
        loadingProgress.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);
        contentContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.VISIBLE);
        errorText.setText(message);
    }

    private void showCollectionNotesLoading() {
        collectionNotesLoading.setVisibility(View.VISIBLE);
        collectionNotesError.setVisibility(View.GONE);
        collectionNotesEmpty.setVisibility(View.GONE);
        collectionNotesRecycler.setVisibility(View.GONE);
    }

    private void showCollectionNotesError(String message) {
        collectionNotesLoading.setVisibility(View.GONE);
        collectionNotesEmpty.setVisibility(View.GONE);
        collectionNotesRecycler.setVisibility(View.GONE);
        collectionNotesError.setVisibility(View.VISIBLE);
        collectionNotesError.setText(message);
    }

    private void showCollectionNotesEmpty() {
        collectionNotesLoading.setVisibility(View.GONE);
        collectionNotesError.setVisibility(View.GONE);
        collectionNotesRecycler.setVisibility(View.GONE);
        collectionNotesEmpty.setVisibility(View.VISIBLE);
    }

    private void showCollectionNotesContent() {
        collectionNotesLoading.setVisibility(View.GONE);
        collectionNotesError.setVisibility(View.GONE);
        collectionNotesEmpty.setVisibility(View.GONE);
        collectionNotesRecycler.setVisibility(View.VISIBLE);
    }

    private void sortNotesPinnedFirst(List<CollectionNoteItem> items) {
        Collections.sort(items, Comparator.comparing(item -> !Boolean.TRUE.equals(item.getIsPinned())));
    }

    private String joinGenres(List<String> genres) {
        StringBuilder builder = new StringBuilder();
        for (String genre : genres) {
            if (TextUtils.isEmpty(genre)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("  |  ");
            }
            builder.append(genre.trim());
        }
        return builder.length() > 0 ? builder.toString() : "Genres unavailable";
    }

    private String orFallback(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private void updateActionButtonsState() {
        addToCollectionButton.setEnabled(!fromCollectionContext && !isCollectionRequestInFlight);
        favouriteButton.setEnabled(!isFavouriteRequestInFlight);

        if (fromCollectionContext) {
            addToCollectionButton.setAlpha(0.7f);
        } else {
            addToCollectionButton.setAlpha(isCollectionRequestInFlight ? 0.7f : 1f);
        }
        favouriteButton.setAlpha(isFavouriteRequestInFlight ? 0.7f : 1f);
    }

    private void updateFavouriteVisualState() {
        favouriteButton.setText(isFavouriteSelected ? "Favourited" : "Favourite");
        int activeText = ContextCompat.getColor(this, R.color.text_high_emp);
        int inactiveText = ContextCompat.getColor(this, R.color.accent_pink);

        favouriteButton.setTextColor(isFavouriteSelected ? activeText : inactiveText);
        favouriteButton.setIconTint(ContextCompat.getColorStateList(this, isFavouriteSelected ? R.color.text_high_emp : R.color.accent_pink));
        favouriteButton.setBackgroundTintList(ContextCompat.getColorStateList(this, isFavouriteSelected ? R.color.accent_pink : R.color.surface_card_soft));
        favouriteButton.setStrokeColor(ContextCompat.getColorStateList(this, isFavouriteSelected ? R.color.card_stroke_pink : R.color.accent_pink_soft));
    }

    private void setActionStatus(String text, int colorRes) {
        actionStatusText.setText(text);
        actionStatusText.setTextColor(ContextCompat.getColor(this, colorRes));
    }

    @Override
    protected void onStart() {
        super.onStart();
        screenStartMs = System.currentTimeMillis();
    }

    @Override
    protected void onStop() {
        if (screenStartMs > 0) {
            String entityId = TextUtils.isEmpty(gameId) ? "game_detail" : gameId;
            String details = fromCollectionContext ? "Game detail from collection" : "Game detail from explore";
            UserActivityLogger.logDuration(
                    this,
                    "screen_view",
                    "game",
                    entityId,
                    details,
                    System.currentTimeMillis() - screenStartMs
            );
            screenStartMs = 0;
        }
        super.onStop();
    }
}
