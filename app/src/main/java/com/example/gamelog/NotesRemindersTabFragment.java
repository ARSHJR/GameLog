package com.example.gamelog;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotesRemindersTabFragment extends Fragment {

    private static final String TYPE_NOTE = "note";
    private static final String TYPE_REMINDER = "reminder";
    public static final String ARG_FILTER_USER_GAME_ID = "arg_filter_user_game_id";
    public static final String ARG_FILTER_GAME_TITLE = "arg_filter_game_title";
    private static final int MAX_PINNED_REMINDERS = 2;

    private String selectedType = TYPE_NOTE;
    private String backendUserId;
    private String filteredUserGameId;
    private String filteredGameTitle;
    private boolean isCreateRequestInFlight;

    private ProgressBar loadingProgress;
    private TextView loadingText;
    private View errorContainer;
    private TextView errorText;
    private MaterialButton retryButton;
    private TextView emptyText;
    private View contentScroll;

    private RecyclerView notesRecycler;
    private RecyclerView pendingRecycler;
    private RecyclerView completedRecycler;
    private TextView pendingHeader;
    private TextView completedHeader;

    private GlobalNotesAdapter notesAdapter;
    private GlobalRemindersAdapter pendingAdapter;
    private GlobalRemindersAdapter completedAdapter;
    private MaterialButtonToggleGroup topToggleGroup;

    private FusedLocationProviderClient fusedLocationClient;
    private AlertDialog activeCreateDialog;
    private TextView activeLocationStateText;
    private TextView activeImageStateText;
    private ImageView activeImagePreview;
    private MaterialButton activeRemoveImageButton;
    private Uri selectedImageUri;
    private Double selectedLatitude;
    private Double selectedLongitude;
    private ReminderFrequencyOption selectedFrequencyOption;

    private final Set<String> mutationInFlightNoteIds = new HashSet<>();
    private final List<CollectionNoteItem> currentReminderItems = new ArrayList<>();
    private final List<ReminderFrequencyOption> reminderFrequencyOptions = buildReminderFrequencyOptions();

    public static NotesRemindersTabFragment newInstance(@Nullable String userGameId, @Nullable String gameTitle) {
        NotesRemindersTabFragment fragment = new NotesRemindersTabFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FILTER_USER_GAME_ID, userGameId);
        args.putString(ARG_FILTER_GAME_TITLE, gameTitle);
        fragment.setArguments(args);
        return fragment;
    }

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) {
                    return;
                }

                selectedImageUri = uri;
                updateActiveImagePreviewState();
            });

    private final ActivityResultLauncher<String> requestMediaPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    imagePickerLauncher.launch("image/*");
                } else if (isAdded()) {
                    Toast.makeText(requireContext(), "Media permission is required to choose an image.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String[]> requestLocationPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean fineGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                boolean coarseGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));

                if (fineGranted || coarseGranted) {
                    fetchCurrentLocation();
                } else if (isAdded()) {
                    Toast.makeText(requireContext(), "Location permission is required to fetch location.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted && isAdded()) {
                    Toast.makeText(requireContext(), "Notification permission denied. Reminder still scheduled.", Toast.LENGTH_SHORT).show();
                }
            });

    private interface CollectionLookupCallback {
        void onFoundUserGameId(String userGameId);

        void onFailure(String message);
    }

    private static class GameSelectionOption {
        private final String gameId;
        private final String title;

        GameSelectionOption(String gameId, String title) {
            this.gameId = gameId;
            this.title = title;
        }

        String getGameId() {
            return gameId;
        }

        @NonNull
        @Override
        public String toString() {
            return title;
        }
    }

    private static class ReminderFrequencyOption {
        private final String label;
        private final String backendValue;
        private final int intervalMinutes;

        ReminderFrequencyOption(String label, String backendValue, int intervalMinutes) {
            this.label = label;
            this.backendValue = backendValue;
            this.intervalMinutes = intervalMinutes;
        }

        String getLabel() {
            return label;
        }

        String getBackendValue() {
            return backendValue;
        }

        int getIntervalMinutes() {
            return intervalMinutes;
        }

        @NonNull
        @Override
        public String toString() {
            return label;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_notes_reminders_tab, container, false);

        Bundle args = getArguments();
        filteredUserGameId = args == null ? null : toNullableText(args.getString(ARG_FILTER_USER_GAME_ID));
        filteredGameTitle = args == null ? null : toNullableText(args.getString(ARG_FILTER_GAME_TITLE));

        bindViews(root);
        configureHeader(root);
        setupToggles(root);
        setupAdapters();
        setupActions(root);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        backendUserId = BackendUserHelper.getBackendUserId(requireContext());
        if (TextUtils.isEmpty(backendUserId)) {
            showErrorState("Missing user context.");
            return root;
        }

        fetchSelectedType();
        return root;
    }

    private void configureHeader(View root) {
        TextView subtitleView = root.findViewById(R.id.global_notes_subtitle);
        if (isFilteredMode()) {
            String gameTitle = TextUtils.isEmpty(filteredGameTitle) ? "selected game" : filteredGameTitle;
            subtitleView.setText("Filtered to " + gameTitle + ".");
        }
    }

    private boolean isFilteredMode() {
        return !TextUtils.isEmpty(filteredUserGameId);
    }

    private void bindViews(View root) {
        loadingProgress = root.findViewById(R.id.global_notes_loading);
        loadingText = root.findViewById(R.id.global_notes_loading_text);
        errorContainer = root.findViewById(R.id.global_notes_error_container);
        errorText = root.findViewById(R.id.global_notes_error_text);
        retryButton = root.findViewById(R.id.global_notes_retry_button);
        emptyText = root.findViewById(R.id.global_notes_empty_text);
        contentScroll = root.findViewById(R.id.global_notes_content_scroll);

        notesRecycler = root.findViewById(R.id.global_notes_recycler);
        pendingRecycler = root.findViewById(R.id.global_pending_recycler);
        completedRecycler = root.findViewById(R.id.global_completed_recycler);
        pendingHeader = root.findViewById(R.id.global_pending_header);
        completedHeader = root.findViewById(R.id.global_completed_header);
    }

    private void setupToggles(View root) {
        topToggleGroup = root.findViewById(R.id.global_notes_toggle_group);
        topToggleGroup.check(R.id.global_notes_tab_notes);
        topToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }

            if (checkedId == R.id.global_notes_tab_reminders) {
                selectedType = TYPE_REMINDER;
            } else {
                selectedType = TYPE_NOTE;
            }
            fetchSelectedType();
        });
    }

    private void setupAdapters() {
        notesRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        pendingRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        completedRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        notesAdapter = new GlobalNotesAdapter(new ArrayList<>());
        notesRecycler.setAdapter(notesAdapter);
        notesAdapter.setOnNoteActionListener(new GlobalNotesAdapter.OnNoteActionListener() {
            @Override
            public void onNoteClicked(CollectionNoteItem item) {
                openNoteReminderDetail(item);
            }

            @Override
            public void onDeleteRequested(CollectionNoteItem item) {
                deleteNote(item, "note");
            }
        });

        pendingAdapter = new GlobalRemindersAdapter(new ArrayList<>());
        completedAdapter = new GlobalRemindersAdapter(new ArrayList<>());
        pendingRecycler.setAdapter(pendingAdapter);
        completedRecycler.setAdapter(completedAdapter);

        GlobalRemindersAdapter.OnReminderActionListener reminderActionListener = new GlobalRemindersAdapter.OnReminderActionListener() {
            @Override
            public void onPinToggleRequested(CollectionNoteItem item, boolean nextPinned) {
                toggleReminderPin(item, nextPinned);
            }

            @Override
            public void onTaskStatusToggleRequested(CollectionNoteItem item, String nextStatus) {
                toggleReminderStatus(item, nextStatus);
            }

            @Override
            public void onDeleteRequested(CollectionNoteItem item) {
                deleteNote(item, "reminder");
            }
        };

        pendingAdapter.setOnReminderActionListener(reminderActionListener);
        completedAdapter.setOnReminderActionListener(reminderActionListener);
        pendingAdapter.setOnReminderItemClickListener(this::openNoteReminderDetail);
        completedAdapter.setOnReminderItemClickListener(this::openNoteReminderDetail);
    }

    private void setupActions(View root) {
        retryButton.setOnClickListener(v -> fetchSelectedType());

        FloatingActionButton addFab = root.findViewById(R.id.global_notes_add_fab);
        addFab.setOnClickListener(v -> showGlobalCreateDialog());
    }

    private void showGlobalCreateDialog() {
        if (!isAdded()) {
            return;
        }
        if (TextUtils.isEmpty(backendUserId)) {
            Toast.makeText(requireContext(), "Missing user context.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isFilteredMode()) {
            showCreateDialogInternal(
                    new ArrayList<>(),
                    new HashMap<>(),
                    filteredUserGameId,
                    TextUtils.isEmpty(filteredGameTitle) ? "Selected game" : filteredGameTitle
            );
            return;
        }

        Toast.makeText(requireContext(), "Loading games...", Toast.LENGTH_SHORT).show();

        ApiService apiService = RetrofitClient.getApiService();
        apiService.getGames().enqueue(new Callback<List<GameApiItem>>() {
            @Override
            public void onResponse(Call<List<GameApiItem>> call, Response<List<GameApiItem>> response) {
                if (!isAdded()) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(requireContext(), "Could not load games.", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<GameSelectionOption> options = buildGameOptions(response.body());
                if (options.isEmpty()) {
                    Toast.makeText(requireContext(), "No games available for selection.", Toast.LENGTH_SHORT).show();
                    return;
                }

                fetchCollectionMapAndShowDialog(options);
            }

            @Override
            public void onFailure(Call<List<GameApiItem>> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(), "Failed to load games: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchCollectionMapAndShowDialog(List<GameSelectionOption> options) {
        ApiService apiService = RetrofitClient.getApiService();
        apiService.getCollection(backendUserId).enqueue(new Callback<List<CollectionEntryItem>>() {
            @Override
            public void onResponse(Call<List<CollectionEntryItem>> call, Response<List<CollectionEntryItem>> response) {
                if (!isAdded()) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(requireContext(), "Could not load collection.", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, String> collectionMap = buildCollectionGameToUserGameMap(response.body());
                showCreateDialogInternal(options, collectionMap, null, null);
            }

            @Override
            public void onFailure(Call<List<CollectionEntryItem>> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(), "Failed to load collection: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCreateDialogInternal(List<GameSelectionOption> options,
                                          Map<String, String> prefetchedCollectionMap,
                                          @Nullable String fixedUserGameId,
                                          @Nullable String fixedGameTitle) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_global_create_note, null);

        MaterialButtonToggleGroup typeGroup = dialogView.findViewById(R.id.global_create_type_group);
        AutoCompleteTextView gameDropdown = dialogView.findViewById(R.id.global_create_game_dropdown);
        AutoCompleteTextView frequencyDropdown = dialogView.findViewById(R.id.global_create_frequency_dropdown);
        View frequencyContainer = dialogView.findViewById(R.id.global_create_frequency_container);
        EditText titleInput = dialogView.findViewById(R.id.global_create_title);
        EditText noteTextInput = dialogView.findViewById(R.id.global_create_note_text);
        MaterialButton pickImageButton = dialogView.findViewById(R.id.global_create_pick_image_button);
        ImageView imagePreview = dialogView.findViewById(R.id.global_create_image_preview);
        MaterialButton removeImageButton = dialogView.findViewById(R.id.global_create_remove_image_button);
        TextView imageStateText = dialogView.findViewById(R.id.global_create_image_state);
        MaterialButton getLocationButton = dialogView.findViewById(R.id.global_create_get_location_button);
        TextView locationStateText = dialogView.findViewById(R.id.global_create_location_state);
        TextView feedbackText = dialogView.findViewById(R.id.global_create_feedback);
        Button cancelButton = dialogView.findViewById(R.id.global_create_cancel);
        Button saveButton = dialogView.findViewById(R.id.global_create_save);

        selectedImageUri = null;
        selectedLatitude = null;
        selectedLongitude = null;
        selectedFrequencyOption = findFrequencyByLabel("1 hour");
        imageStateText.setText("Not set");
        locationStateText.setText("Not set");

        ArrayAdapter<ReminderFrequencyOption> frequencyAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                reminderFrequencyOptions
        );
        frequencyDropdown.setAdapter(frequencyAdapter);
        frequencyDropdown.setOnClickListener(v -> frequencyDropdown.showDropDown());
        frequencyDropdown.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                frequencyDropdown.showDropDown();
            }
        });
        if (selectedFrequencyOption != null) {
            frequencyDropdown.setText(selectedFrequencyOption.getLabel(), false);
        }
        frequencyDropdown.setOnItemClickListener((parent, view, position, id) -> {
            ReminderFrequencyOption option = frequencyAdapter.getItem(position);
            if (option != null) {
                selectedFrequencyOption = option;
            }
        });

        if (TYPE_REMINDER.equals(selectedType)) {
            typeGroup.check(R.id.global_create_type_reminder);
            frequencyContainer.setVisibility(View.VISIBLE);
        } else {
            typeGroup.check(R.id.global_create_type_note);
            frequencyContainer.setVisibility(View.GONE);
        }

        typeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.global_create_type_reminder) {
                frequencyContainer.setVisibility(View.VISIBLE);
                if (selectedFrequencyOption == null) {
                    selectedFrequencyOption = findFrequencyByLabel("1 hour");
                }
                if (selectedFrequencyOption != null) {
                    frequencyDropdown.setText(selectedFrequencyOption.getLabel(), false);
                }
            } else {
                frequencyContainer.setVisibility(View.GONE);
            }
        });

        pickImageButton.setOnClickListener(v -> startImageSelectionFlow());
        getLocationButton.setOnClickListener(v -> startLocationFetchFlow());

        final boolean isFixedMode = !TextUtils.isEmpty(fixedUserGameId);
        final Map<String, String> mutableCollectionMap = new HashMap<>(prefetchedCollectionMap);
        final GameSelectionOption[] selectedGame = new GameSelectionOption[1];

        ArrayAdapter<GameSelectionOption> gameAdapter = null;
        if (!isFixedMode) {
            gameAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    options
            );
            gameDropdown.setAdapter(gameAdapter);
            gameDropdown.setOnClickListener(v -> gameDropdown.showDropDown());
            gameDropdown.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    gameDropdown.showDropDown();
                }
            });
            ArrayAdapter<GameSelectionOption> finalGameAdapter = gameAdapter;
            gameDropdown.setOnItemClickListener((parent, view, position, id) -> selectedGame[0] = finalGameAdapter.getItem(position));

            if (!options.isEmpty()) {
                selectedGame[0] = options.get(0);
                gameDropdown.setText(options.get(0).toString(), false);
            }
        } else {
            gameDropdown.setText(TextUtils.isEmpty(fixedGameTitle) ? "Selected game" : fixedGameTitle, false);
            gameDropdown.setEnabled(false);
            gameDropdown.setFocusable(false);
            gameDropdown.setClickable(false);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();
        activeCreateDialog = dialog;
        activeImageStateText = imageStateText;
        activeImagePreview = imagePreview;
        activeRemoveImageButton = removeImageButton;
        activeLocationStateText = locationStateText;
        updateActiveImagePreviewState();

        removeImageButton.setOnClickListener(v -> {
            selectedImageUri = null;
            updateActiveImagePreviewState();
        });

        dialog.setOnDismissListener(d -> {
            activeCreateDialog = null;
            activeImageStateText = null;
            activeImagePreview = null;
            activeRemoveImageButton = null;
            activeLocationStateText = null;
            selectedImageUri = null;
            selectedLatitude = null;
            selectedLongitude = null;
            selectedFrequencyOption = null;
        });

        cancelButton.setOnClickListener(v -> {
            if (!isCreateRequestInFlight) {
                dialog.dismiss();
            }
        });

        saveButton.setOnClickListener(v -> {
            if (isCreateRequestInFlight) {
                return;
            }

            String createType = resolveCreateType(typeGroup.getCheckedButtonId(),
                    R.id.global_create_type_note,
                    R.id.global_create_type_reminder);
            if (TextUtils.isEmpty(createType)) {
                showDialogError(feedbackText, "Please choose Note or Reminder.");
                return;
            }

            String title = toNullableText(titleInput.getText() != null ? titleInput.getText().toString() : null);
            String noteText = toNullableText(noteTextInput.getText() != null ? noteTextInput.getText().toString() : null);
            String mediaUri = selectedImageUri != null ? selectedImageUri.toString() : null;
            Double latitude = selectedLatitude;
            Double longitude = selectedLongitude;
            ReminderFrequencyOption frequencyOption = null;

            if (TYPE_REMINDER.equals(createType)) {
                frequencyOption = resolveSelectedFrequency(frequencyDropdown.getText() != null ? frequencyDropdown.getText().toString() : null);
                if (frequencyOption == null) {
                    showDialogError(feedbackText, "Please choose a reminder frequency.");
                    return;
                }
            }
            final ReminderFrequencyOption finalFrequencyOption = frequencyOption;

            if (title == null && noteText == null) {
                showDialogError(feedbackText, "Add at least a title or note text.");
                return;
            }

            hideDialogError(feedbackText);
            setDialogLoadingState(saveButton, cancelButton, true);

            if (isFixedMode) {
                createGlobalNoteOrReminder(dialog, saveButton, cancelButton, createType, fixedUserGameId,
                        title, noteText, mediaUri, latitude, longitude,
                        finalFrequencyOption, fixedGameTitle);
                return;
            }

            GameSelectionOption resolvedSelectedGame = resolveSelectedGame(options,
                    gameDropdown.getText() != null ? gameDropdown.getText().toString() : null,
                    selectedGame[0]);
            if (resolvedSelectedGame == null || TextUtils.isEmpty(resolvedSelectedGame.getGameId())) {
                setDialogLoadingState(saveButton, cancelButton, false);
                showDialogError(feedbackText, "Please choose a game.");
                return;
            }

            resolveUserGameIdForCreate(resolvedSelectedGame.getGameId(), mutableCollectionMap, new CollectionLookupCallback() {
                @Override
                public void onFoundUserGameId(String userGameId) {
                    createGlobalNoteOrReminder(dialog, saveButton, cancelButton, createType, userGameId,
                            title, noteText, mediaUri, latitude, longitude,
                            finalFrequencyOption, resolvedSelectedGame.toString());
                }

                @Override
                public void onFailure(String message) {
                    if (!isAdded()) {
                        return;
                    }
                    setDialogLoadingState(saveButton, cancelButton, false);
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void resolveUserGameIdForCreate(String gameId, Map<String, String> collectionMap, CollectionLookupCallback callback) {
        String normalizedGameId = normalizeKey(gameId);
        if (TextUtils.isEmpty(normalizedGameId)) {
            callback.onFailure("Selected game is missing an id.");
            return;
        }

        String existingUserGameId = collectionMap.get(normalizedGameId);
        if (!TextUtils.isEmpty(existingUserGameId)) {
            callback.onFoundUserGameId(existingUserGameId);
            return;
        }

        ApiService apiService = RetrofitClient.getApiService();
        AddToCollectionRequest request = new AddToCollectionRequest(normalizedGameId, "planned");
        apiService.addToCollection(backendUserId, request).enqueue(new Callback<AddToCollectionResponse>() {
            @Override
            public void onResponse(Call<AddToCollectionResponse> call, Response<AddToCollectionResponse> response) {
                if (!isAdded()) {
                    return;
                }
                if (!response.isSuccessful()) {
                    callback.onFailure("Failed to add game to collection.");
                    return;
                }

                AddToCollectionResponse body = response.body();
                UserGameEntryResponse entry = body != null ? body.getEntry() : null;
                String userGameId = entry != null ? toNullableText(entry.getUserGameId()) : null;

                if (!TextUtils.isEmpty(userGameId)) {
                    collectionMap.put(normalizedGameId, userGameId);
                    callback.onFoundUserGameId(userGameId);
                    return;
                }

                fetchCollectionForGameLookup(normalizedGameId, collectionMap, callback);
            }

            @Override
            public void onFailure(Call<AddToCollectionResponse> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                callback.onFailure("Failed to add game to collection: " + t.getMessage());
            }
        });
    }

    private void fetchCollectionForGameLookup(String normalizedGameId, Map<String, String> collectionMap, CollectionLookupCallback callback) {
        ApiService apiService = RetrofitClient.getApiService();
        apiService.getCollection(backendUserId).enqueue(new Callback<List<CollectionEntryItem>>() {
            @Override
            public void onResponse(Call<List<CollectionEntryItem>> call, Response<List<CollectionEntryItem>> response) {
                if (!isAdded()) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onFailure("Could not verify collection entry.");
                    return;
                }

                Map<String, String> refreshedMap = buildCollectionGameToUserGameMap(response.body());
                collectionMap.putAll(refreshedMap);
                String resolvedUserGameId = refreshedMap.get(normalizedGameId);

                if (TextUtils.isEmpty(resolvedUserGameId)) {
                    callback.onFailure("Could not resolve collection entry for selected game.");
                    return;
                }

                callback.onFoundUserGameId(resolvedUserGameId);
            }

            @Override
            public void onFailure(Call<List<CollectionEntryItem>> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                callback.onFailure("Failed to verify collection entry: " + t.getMessage());
            }
        });
    }

    private void createGlobalNoteOrReminder(AlertDialog dialog, Button saveButton, Button cancelButton,
                                            String createType, String userGameId,
                                            String title, String noteText,
                            String mediaUri, Double latitude, Double longitude,
                            @Nullable ReminderFrequencyOption frequencyOption,
                            @Nullable String selectedGameTitle) {
        String taskStatus = TYPE_REMINDER.equals(createType) ? "pending" : null;
        String frequency = TYPE_REMINDER.equals(createType) && frequencyOption != null
            ? frequencyOption.getBackendValue()
            : null;

        CreateCollectionNoteRequest request = new CreateCollectionNoteRequest(
                createType,
                title,
                noteText,
                mediaUri,
                latitude,
                longitude,
            taskStatus,
            frequency
        );

        ApiService apiService = RetrofitClient.getApiService();
        apiService.createCollectionNote(userGameId, request).enqueue(new Callback<CollectionNoteItem>() {
            @Override
            public void onResponse(Call<CollectionNoteItem> call, Response<CollectionNoteItem> response) {
                if (!isAdded()) {
                    return;
                }
                setDialogLoadingState(saveButton, cancelButton, false);

                if (!response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Failed to create entry.", Toast.LENGTH_SHORT).show();
                    return;
                }

                CollectionNoteItem createdItem = response.body();
                if (TYPE_REMINDER.equals(createType) && frequencyOption != null && createdItem != null) {
                    scheduleReminderWork(createdItem, selectedGameTitle, title, noteText, frequencyOption.getIntervalMinutes());
                }

                dialog.dismiss();
                Toast.makeText(requireContext(), TYPE_REMINDER.equals(createType) ? "Reminder created" : "Note created", Toast.LENGTH_SHORT).show();

                if (!TextUtils.equals(selectedType, createType)) {
                    selectedType = createType;
                    if (TYPE_REMINDER.equals(createType)) {
                        topToggleGroup.check(R.id.global_notes_tab_reminders);
                    } else {
                        topToggleGroup.check(R.id.global_notes_tab_notes);
                    }
                } else {
                    fetchSelectedType();
                }
            }

            @Override
            public void onFailure(Call<CollectionNoteItem> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                setDialogLoadingState(saveButton, cancelButton, false);
                Toast.makeText(requireContext(), "Failed to create entry: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<GameSelectionOption> buildGameOptions(List<GameApiItem> games) {
        List<GameSelectionOption> options = new ArrayList<>();
        for (GameApiItem game : games) {
            String gameId = toNullableText(game.getGameId());
            if (TextUtils.isEmpty(gameId)) {
                continue;
            }

            String title = toNullableText(game.getTitle());
            if (TextUtils.isEmpty(title)) {
                title = "Game #" + gameId;
            }
            options.add(new GameSelectionOption(gameId, title));
        }

        Collections.sort(options, Comparator.comparing(GameSelectionOption::toString, String.CASE_INSENSITIVE_ORDER));
        return options;
    }

    private Map<String, String> buildCollectionGameToUserGameMap(List<CollectionEntryItem> entries) {
        Map<String, String> map = new HashMap<>();
        for (CollectionEntryItem entry : entries) {
            String gameId = normalizeKey(entry.getGameId());
            String userGameId = toNullableText(entry.getUserGameId());
            if (TextUtils.isEmpty(gameId) || TextUtils.isEmpty(userGameId)) {
                continue;
            }
            map.put(gameId, userGameId);
        }
        return map;
    }

    private GameSelectionOption resolveSelectedGame(List<GameSelectionOption> options, String rawInput, GameSelectionOption selectedFallback) {
        String normalizedInput = toNullableText(rawInput);
        if (TextUtils.isEmpty(normalizedInput)) {
            return selectedFallback;
        }

        for (GameSelectionOption option : options) {
            if (normalizedInput.equalsIgnoreCase(option.toString())) {
                return option;
            }
        }

        if (selectedFallback != null && normalizedInput.equalsIgnoreCase(selectedFallback.toString())) {
            return selectedFallback;
        }

        return null;
    }

    private List<ReminderFrequencyOption> buildReminderFrequencyOptions() {
        List<ReminderFrequencyOption> options = new ArrayList<>();
        options.add(new ReminderFrequencyOption("5 min", "5_min", 5));
        options.add(new ReminderFrequencyOption("20 min", "20_min", 20));
        options.add(new ReminderFrequencyOption("1 hour", "1_hour", 60));
        options.add(new ReminderFrequencyOption("6 hours", "6_hour", 360));
        options.add(new ReminderFrequencyOption("24 hours", "1_day", 1440));
        return options;
    }

    @Nullable
    private ReminderFrequencyOption findFrequencyByLabel(String label) {
        if (TextUtils.isEmpty(label)) {
            return null;
        }

        for (ReminderFrequencyOption option : reminderFrequencyOptions) {
            if (label.equalsIgnoreCase(option.getLabel())) {
                return option;
            }
        }
        return null;
    }

    @Nullable
    private ReminderFrequencyOption resolveSelectedFrequency(String rawInput) {
        ReminderFrequencyOption byLabel = findFrequencyByLabel(rawInput == null ? null : rawInput.trim());
        if (byLabel != null) {
            selectedFrequencyOption = byLabel;
            return byLabel;
        }
        return selectedFrequencyOption;
    }

    private void startImageSelectionFlow() {
        if (!isAdded()) {
            return;
        }

        String mediaPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(requireContext(), mediaPermission) == PackageManager.PERMISSION_GRANTED) {
            imagePickerLauncher.launch("image/*");
            return;
        }

        requestMediaPermissionLauncher.launch(mediaPermission);
    }

    private void startLocationFetchFlow() {
        if (!isAdded()) {
            return;
        }

        boolean fineGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (fineGranted || coarseGranted) {
            fetchCurrentLocation();
            return;
        }

        requestLocationPermissionsLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void fetchCurrentLocation() {
        if (!isAdded()) {
            return;
        }

        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener(location -> {
                        if (!isAdded()) {
                            return;
                        }

                        if (location == null) {
                            Toast.makeText(requireContext(), "Could not determine current location.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        selectedLatitude = location.getLatitude();
                        selectedLongitude = location.getLongitude();
                        if (activeLocationStateText != null) {
                            activeLocationStateText.setText(String.format(Locale.US, "%.5f, %.5f", selectedLatitude, selectedLongitude));
                        }
                    })
                    .addOnFailureListener(error -> {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Failed to fetch location.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (SecurityException ex) {
            if (isAdded()) {
                Toast.makeText(requireContext(), "Location permission error.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void scheduleReminderWork(@NonNull CollectionNoteItem createdItem,
                                      @Nullable String gameTitle,
                                      @Nullable String reminderTitle,
                                      @Nullable String reminderBody,
                                      int intervalMinutes) {
        if (!isAdded()) {
            return;
        }

        String noteId = toNullableText(createdItem.getNoteId());
        if (TextUtils.isEmpty(noteId)) {
            Toast.makeText(requireContext(), "Reminder created, but scheduling id is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }

        Data inputData = new Data.Builder()
                .putString(ReminderWorker.INPUT_NOTE_ID, noteId)
                .putString(ReminderWorker.INPUT_GAME_TITLE, toNullableText(gameTitle))
                .putString(ReminderWorker.INPUT_REMINDER_TITLE, toNullableText(reminderTitle))
                .putString(ReminderWorker.INPUT_REMINDER_BODY, toNullableText(reminderBody))
                .build();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                ReminderWorker.class,
                intervalMinutes,
                TimeUnit.MINUTES
        ).setInputData(inputData).build();

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                "global_reminder_" + noteId,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
        );
    }

    private void setDialogLoadingState(Button saveButton, Button cancelButton, boolean loading) {
        isCreateRequestInFlight = loading;
        saveButton.setEnabled(!loading);
        cancelButton.setEnabled(!loading);
        saveButton.setText(loading ? "Creating..." : "Create");
    }

    private void updateActiveImagePreviewState() {
        if (activeImageStateText == null || activeImagePreview == null || activeRemoveImageButton == null) {
            return;
        }

        if (selectedImageUri == null) {
            activeImageStateText.setText("Not set");
            activeImagePreview.setVisibility(View.GONE);
            activeRemoveImageButton.setVisibility(View.GONE);
            return;
        }

        String label = selectedImageUri.getLastPathSegment();
        activeImageStateText.setText(TextUtils.isEmpty(label) ? "Selected image" : "Selected: " + label);
        activeImagePreview.setVisibility(View.VISIBLE);
        activeRemoveImageButton.setVisibility(View.VISIBLE);
        ImageLoader.loadCover(activeImagePreview, selectedImageUri.toString(), R.drawable.ic_games);
    }

    private void openNoteReminderDetail(CollectionNoteItem item) {
        if (!isAdded() || item == null) {
            return;
        }

        Intent intent = new Intent(requireContext(), NoteReminderDetailActivity.class);
        intent.putExtra(NoteReminderDetailActivity.EXTRA_TYPE, item.getNoteType());
        intent.putExtra(NoteReminderDetailActivity.EXTRA_TITLE, item.getTitle());
        intent.putExtra(NoteReminderDetailActivity.EXTRA_TEXT, item.getNoteText());
        intent.putExtra(NoteReminderDetailActivity.EXTRA_GAME_TITLE, item.getGameTitle());
        intent.putExtra(NoteReminderDetailActivity.EXTRA_MEDIA_URI, item.getMediaUri());
        intent.putExtra(NoteReminderDetailActivity.EXTRA_FREQUENCY, item.getFrequency());
        intent.putExtra(NoteReminderDetailActivity.EXTRA_NEXT_TRIGGER_AT, item.getNextTriggerAt());
        intent.putExtra(NoteReminderDetailActivity.EXTRA_STATUS, item.getTaskStatus());
        intent.putExtra(NoteReminderDetailActivity.EXTRA_CREATED_AT, item.getCreatedAt());
        intent.putExtra(NoteReminderDetailActivity.EXTRA_LATITUDE, item.getLatitude() == null ? null : String.valueOf(item.getLatitude()));
        intent.putExtra(NoteReminderDetailActivity.EXTRA_LONGITUDE, item.getLongitude() == null ? null : String.valueOf(item.getLongitude()));
        startActivity(intent);
    }

    private String resolveCreateType(int checkedId, int noteButtonId, int reminderButtonId) {
        if (checkedId == noteButtonId) {
            return TYPE_NOTE;
        }
        if (checkedId == reminderButtonId) {
            return TYPE_REMINDER;
        }
        return null;
    }

    private String normalizeKey(String input) {
        String value = toNullableText(input);
        return value == null ? null : value.trim();
    }

    private String toNullableText(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Double parseNullableDouble(String raw) {
        String normalized = toNullableText(raw);
        if (normalized == null) {
            return null;
        }
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void showDialogError(TextView feedbackText, String message) {
        feedbackText.setVisibility(View.VISIBLE);
        feedbackText.setText(message);
    }

    private void hideDialogError(TextView feedbackText) {
        feedbackText.setVisibility(View.GONE);
        feedbackText.setText("");
    }

    private void fetchSelectedType() {
        showLoadingState();

        ApiService apiService = RetrofitClient.getApiService();
        Call<List<CollectionNoteItem>> requestCall = isFilteredMode()
                ? apiService.getCollectionNotes(filteredUserGameId, selectedType)
                : apiService.getUserNotes(backendUserId, selectedType);

        requestCall.enqueue(new Callback<List<CollectionNoteItem>>() {
            @Override
            public void onResponse(Call<List<CollectionNoteItem>> call, Response<List<CollectionNoteItem>> response) {
                if (!isAdded()) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    showErrorState("Could not load " + selectedType + " items.");
                    return;
                }

                List<CollectionNoteItem> items = response.body();
                if (items.isEmpty()) {
                    showEmptyState(TYPE_REMINDER.equals(selectedType)
                            ? (isFilteredMode() ? "No reminders found for this game." : "No reminders found across your collection.")
                            : (isFilteredMode() ? "No notes found for this game." : "No notes found across your collection."));
                    return;
                }

                if (TYPE_REMINDER.equals(selectedType)) {
                    bindReminders(items);
                } else {
                    bindNotes(items);
                }
                showContentState();
            }

            @Override
            public void onFailure(Call<List<CollectionNoteItem>> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                showErrorState("Unable to connect to server.");
            }
        });
    }

    private void bindNotes(List<CollectionNoteItem> notes) {
        currentReminderItems.clear();
        notesAdapter.updateItems(notes);

        notesRecycler.setVisibility(View.VISIBLE);
        pendingHeader.setVisibility(View.GONE);
        pendingRecycler.setVisibility(View.GONE);
        completedHeader.setVisibility(View.GONE);
        completedRecycler.setVisibility(View.GONE);
    }

    private void bindReminders(List<CollectionNoteItem> reminders) {
        currentReminderItems.clear();
        currentReminderItems.addAll(reminders);

        List<CollectionNoteItem> pending = new ArrayList<>();
        List<CollectionNoteItem> completed = new ArrayList<>();

        for (CollectionNoteItem item : reminders) {
            String taskStatus = item.getTaskStatus() == null ? "" : item.getTaskStatus().trim().toLowerCase();
            if ("completed".equals(taskStatus)) {
                completed.add(item);
            } else {
                pending.add(item);
            }
        }

        pendingAdapter.updateItems(pending);
        completedAdapter.updateItems(completed);
        pendingAdapter.setMutationInFlightIds(mutationInFlightNoteIds);
        completedAdapter.setMutationInFlightIds(mutationInFlightNoteIds);

        notesRecycler.setVisibility(View.GONE);

        pendingHeader.setVisibility(View.VISIBLE);
        pendingRecycler.setVisibility(pending.isEmpty() ? View.GONE : View.VISIBLE);

        completedHeader.setVisibility(View.VISIBLE);
        completedRecycler.setVisibility(completed.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void toggleReminderPin(CollectionNoteItem item, boolean nextPinned) {
        String noteId = item != null ? item.getNoteId() : null;
        if (TextUtils.isEmpty(noteId) || mutationInFlightNoteIds.contains(noteId)) {
            return;
        }

        if (nextPinned && countPinnedReminders() >= MAX_PINNED_REMINDERS) {
            Toast.makeText(requireContext(), "You can pin up to 2 reminders.", Toast.LENGTH_SHORT).show();
            return;
        }

        mutationInFlightNoteIds.add(noteId);
        pendingAdapter.setMutationInFlightIds(mutationInFlightNoteIds);
        completedAdapter.setMutationInFlightIds(mutationInFlightNoteIds);

        ApiService apiService = RetrofitClient.getApiService();
        apiService.toggleNotePin(noteId).enqueue(new Callback<CollectionNoteItem>() {
            @Override
            public void onResponse(Call<CollectionNoteItem> call, Response<CollectionNoteItem> response) {
                if (!isAdded()) {
                    return;
                }

                mutationInFlightNoteIds.remove(noteId);
                pendingAdapter.setMutationInFlightIds(mutationInFlightNoteIds);
                completedAdapter.setMutationInFlightIds(mutationInFlightNoteIds);

                if (!response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Failed to update reminder pin.", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(requireContext(), nextPinned ? "Reminder pinned" : "Reminder unpinned", Toast.LENGTH_SHORT).show();
                fetchSelectedType();
            }

            @Override
            public void onFailure(Call<CollectionNoteItem> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }

                mutationInFlightNoteIds.remove(noteId);
                pendingAdapter.setMutationInFlightIds(mutationInFlightNoteIds);
                completedAdapter.setMutationInFlightIds(mutationInFlightNoteIds);
                Toast.makeText(requireContext(), "Failed to update reminder pin: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int countPinnedReminders() {
        int pinnedCount = 0;
        for (CollectionNoteItem item : currentReminderItems) {
            if (Boolean.TRUE.equals(item.getIsPinned())) {
                pinnedCount++;
            }
        }
        return pinnedCount;
    }

    private void toggleReminderStatus(CollectionNoteItem item, String nextStatus) {
        String noteId = item.getNoteId();
        if (TextUtils.isEmpty(noteId) || mutationInFlightNoteIds.contains(noteId)) {
            return;
        }

        mutationInFlightNoteIds.add(noteId);
        pendingAdapter.setMutationInFlightIds(mutationInFlightNoteIds);
        completedAdapter.setMutationInFlightIds(mutationInFlightNoteIds);

        ApiService apiService = RetrofitClient.getApiService();
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(nextStatus);
        apiService.updateReminderTaskStatus(noteId, request).enqueue(new Callback<CollectionNoteItem>() {
            @Override
            public void onResponse(Call<CollectionNoteItem> call, Response<CollectionNoteItem> response) {
                if (!isAdded()) {
                    return;
                }
                mutationInFlightNoteIds.remove(noteId);
                pendingAdapter.setMutationInFlightIds(mutationInFlightNoteIds);
                completedAdapter.setMutationInFlightIds(mutationInFlightNoteIds);

                if (!response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Failed to update reminder status", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(requireContext(), "Reminder status updated", Toast.LENGTH_SHORT).show();
                fetchSelectedType();
            }

            @Override
            public void onFailure(Call<CollectionNoteItem> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                mutationInFlightNoteIds.remove(noteId);
                pendingAdapter.setMutationInFlightIds(mutationInFlightNoteIds);
                completedAdapter.setMutationInFlightIds(mutationInFlightNoteIds);
                Toast.makeText(requireContext(), "Failed to update reminder: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteNote(CollectionNoteItem item, String typeLabel) {
        String noteId = item != null ? item.getNoteId() : null;
        if (TextUtils.isEmpty(noteId) || mutationInFlightNoteIds.contains(noteId)) {
            return;
        }

        mutationInFlightNoteIds.add(noteId);
        pendingAdapter.setMutationInFlightIds(mutationInFlightNoteIds);
        completedAdapter.setMutationInFlightIds(mutationInFlightNoteIds);

        ApiService apiService = RetrofitClient.getApiService();
        apiService.deleteNote(noteId).enqueue(new Callback<CollectionNoteItem>() {
            @Override
            public void onResponse(Call<CollectionNoteItem> call, Response<CollectionNoteItem> response) {
                if (!isAdded()) {
                    return;
                }

                mutationInFlightNoteIds.remove(noteId);
                pendingAdapter.setMutationInFlightIds(mutationInFlightNoteIds);
                completedAdapter.setMutationInFlightIds(mutationInFlightNoteIds);

                if (!response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Failed to delete " + typeLabel, Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(requireContext(), Character.toUpperCase(typeLabel.charAt(0)) + typeLabel.substring(1) + " deleted", Toast.LENGTH_SHORT).show();
                fetchSelectedType();
            }

            @Override
            public void onFailure(Call<CollectionNoteItem> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }

                mutationInFlightNoteIds.remove(noteId);
                pendingAdapter.setMutationInFlightIds(mutationInFlightNoteIds);
                completedAdapter.setMutationInFlightIds(mutationInFlightNoteIds);
                Toast.makeText(requireContext(), "Failed to delete " + typeLabel + ": " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoadingState() {
        loadingProgress.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.VISIBLE);
        errorContainer.setVisibility(View.GONE);
        emptyText.setVisibility(View.GONE);
        contentScroll.setVisibility(View.GONE);
    }

    private void showErrorState(String message) {
        loadingProgress.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);
        contentScroll.setVisibility(View.GONE);
        emptyText.setVisibility(View.GONE);

        errorContainer.setVisibility(View.VISIBLE);
        errorText.setText(message);
    }

    private void showEmptyState(String message) {
        loadingProgress.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);
        contentScroll.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);

        emptyText.setVisibility(View.VISIBLE);
        emptyText.setText(message);
    }

    private void showContentState() {
        loadingProgress.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        emptyText.setVisibility(View.GONE);
        contentScroll.setVisibility(View.VISIBLE);
    }
}
