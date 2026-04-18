package com.example.gamelog;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotesRemindersTabFragment extends Fragment {

    private static final String TYPE_NOTE = "note";
    private static final String TYPE_REMINDER = "reminder";

    private String selectedType = TYPE_NOTE;
    private String backendUserId;
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

    private final Set<String> mutationInFlightNoteIds = new HashSet<>();

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_notes_reminders_tab, container, false);

        bindViews(root);
        setupToggles(root);
        setupAdapters();
        setupActions(root);

        backendUserId = BackendUserHelper.getBackendUserId(requireContext());
        if (TextUtils.isEmpty(backendUserId)) {
            showErrorState("Missing user context.");
            return root;
        }

        fetchSelectedType();
        return root;
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

        pendingAdapter = new GlobalRemindersAdapter(new ArrayList<>());
        completedAdapter = new GlobalRemindersAdapter(new ArrayList<>());
        pendingRecycler.setAdapter(pendingAdapter);
        completedRecycler.setAdapter(completedAdapter);

        GlobalRemindersAdapter.OnReminderActionListener reminderActionListener = new GlobalRemindersAdapter.OnReminderActionListener() {
            @Override
            public void onToggleTaskStatus(CollectionNoteItem item, String nextStatus) {
                toggleReminderStatus(item, nextStatus);
            }

            @Override
            public void onDeleteRequested(CollectionNoteItem item) {
                deleteReminder(item);
            }
        };

        pendingAdapter.setOnReminderActionListener(reminderActionListener);
        completedAdapter.setOnReminderActionListener(reminderActionListener);
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
                showCreateDialogInternal(options, collectionMap);
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

    private void showCreateDialogInternal(List<GameSelectionOption> options, Map<String, String> prefetchedCollectionMap) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_global_create_note, null);

        MaterialButtonToggleGroup typeGroup = dialogView.findViewById(R.id.global_create_type_group);
        AutoCompleteTextView gameDropdown = dialogView.findViewById(R.id.global_create_game_dropdown);
        EditText titleInput = dialogView.findViewById(R.id.global_create_title);
        EditText noteTextInput = dialogView.findViewById(R.id.global_create_note_text);
        EditText mediaUriInput = dialogView.findViewById(R.id.global_create_media_uri);
        EditText latitudeInput = dialogView.findViewById(R.id.global_create_latitude);
        EditText longitudeInput = dialogView.findViewById(R.id.global_create_longitude);
        TextView feedbackText = dialogView.findViewById(R.id.global_create_feedback);
        Button cancelButton = dialogView.findViewById(R.id.global_create_cancel);
        Button saveButton = dialogView.findViewById(R.id.global_create_save);

        if (TYPE_REMINDER.equals(selectedType)) {
            typeGroup.check(R.id.global_create_type_reminder);
        } else {
            typeGroup.check(R.id.global_create_type_note);
        }

        final Map<String, String> mutableCollectionMap = new HashMap<>(prefetchedCollectionMap);
        final GameSelectionOption[] selectedGame = new GameSelectionOption[1];

        ArrayAdapter<GameSelectionOption> gameAdapter = new ArrayAdapter<>(
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
        gameDropdown.setOnItemClickListener((parent, view, position, id) -> selectedGame[0] = gameAdapter.getItem(position));

        if (!options.isEmpty()) {
            selectedGame[0] = options.get(0);
            gameDropdown.setText(options.get(0).toString(), false);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

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

            GameSelectionOption resolvedSelectedGame = resolveSelectedGame(options, gameDropdown.getText() != null ? gameDropdown.getText().toString() : null, selectedGame[0]);
            if (resolvedSelectedGame == null || TextUtils.isEmpty(resolvedSelectedGame.getGameId())) {
                showDialogError(feedbackText, "Please choose a game.");
                return;
            }

            String title = toNullableText(titleInput.getText() != null ? titleInput.getText().toString() : null);
            String noteText = toNullableText(noteTextInput.getText() != null ? noteTextInput.getText().toString() : null);
            String mediaUri = toNullableText(mediaUriInput.getText() != null ? mediaUriInput.getText().toString() : null);

            if (title == null && noteText == null) {
                showDialogError(feedbackText, "Add at least a title or note text.");
                return;
            }

            Double latitude = parseNullableDouble(latitudeInput.getText() != null ? latitudeInput.getText().toString() : null);
            Double longitude = parseNullableDouble(longitudeInput.getText() != null ? longitudeInput.getText().toString() : null);
            String rawLat = toNullableText(latitudeInput.getText() != null ? latitudeInput.getText().toString() : null);
            String rawLng = toNullableText(longitudeInput.getText() != null ? longitudeInput.getText().toString() : null);
            if ((rawLat != null && latitude == null) || (rawLng != null && longitude == null)) {
                showDialogError(feedbackText, "Latitude/Longitude must be valid numbers.");
                return;
            }

            hideDialogError(feedbackText);
            setDialogLoadingState(saveButton, cancelButton, true);

            resolveUserGameIdForCreate(resolvedSelectedGame.getGameId(), mutableCollectionMap, new CollectionLookupCallback() {
                @Override
                public void onFoundUserGameId(String userGameId) {
                    createGlobalNoteOrReminder(dialog, saveButton, cancelButton, createType, userGameId,
                            title, noteText, mediaUri, latitude, longitude);
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
                                            String mediaUri, Double latitude, Double longitude) {
        String taskStatus = TYPE_REMINDER.equals(createType) ? "pending" : null;

        CreateCollectionNoteRequest request = new CreateCollectionNoteRequest(
                createType,
                title,
                noteText,
                mediaUri,
                latitude,
                longitude,
                taskStatus
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

    private void setDialogLoadingState(Button saveButton, Button cancelButton, boolean loading) {
        isCreateRequestInFlight = loading;
        saveButton.setEnabled(!loading);
        cancelButton.setEnabled(!loading);
        saveButton.setText(loading ? "Creating..." : "Create");
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
        apiService.getUserNotes(backendUserId, selectedType).enqueue(new Callback<List<CollectionNoteItem>>() {
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
                            ? "No reminders found across your collection."
                            : "No notes found across your collection.");
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
        notesAdapter.updateItems(notes);

        notesRecycler.setVisibility(View.VISIBLE);
        pendingHeader.setVisibility(View.GONE);
        pendingRecycler.setVisibility(View.GONE);
        completedHeader.setVisibility(View.GONE);
        completedRecycler.setVisibility(View.GONE);
    }

    private void bindReminders(List<CollectionNoteItem> reminders) {
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

    private void deleteReminder(CollectionNoteItem item) {
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
                    Toast.makeText(requireContext(), "Failed to delete reminder", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(requireContext(), "Reminder deleted", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(requireContext(), "Failed to delete reminder: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
