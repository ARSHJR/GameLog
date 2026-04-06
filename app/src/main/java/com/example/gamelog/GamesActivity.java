package com.example.gamelog;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;

public class GamesActivity extends AppCompatActivity {

    private ArrayList<GameItem> gameList = new ArrayList<>();
    private GameAdapter adapter;
    private TextView totalGames;
    private LinearLayout emptyState;
    private EditText titleInput;
    private EditText notesInput;

    // Feature 1: GPS Integration
    private FusedLocationProviderClient fusedLocationClient;
    private TextView textLatitude, textLongitude;
    private String currentFetchedLocation = "";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    // Feature 2: Media Handling
    private ImageView imagePreview;
    private Uri selectedImageUri;
    private static final int PICK_IMAGE_REQUEST = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_games);

        totalGames = findViewById(R.id.total_games);
        emptyState = findViewById(R.id.empty_state);
        titleInput = findViewById(R.id.game_title_input);
        notesInput = findViewById(R.id.game_notes_input);

        // Feature 1: Location Setup
        textLatitude = findViewById(R.id.text_latitude);
        textLongitude = findViewById(R.id.text_longitude);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        Button getLocationButton = findViewById(R.id.get_location_button);
        getLocationButton.setOnClickListener(v -> checkLocationPermissions());

        // Feature 2: Image Picker Setup
        imagePreview = findViewById(R.id.image_preview);
        Button chooseImageButton = findViewById(R.id.choose_image_button);
        chooseImageButton.setOnClickListener(v -> openGallery());

        RecyclerView recyclerView = findViewById(R.id.games_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GameAdapter(gameList);
        recyclerView.setAdapter(adapter);

        Button saveButton = findViewById(R.id.save_game_button);
        saveButton.setOnClickListener(v -> saveGame(false));
        saveButton.setOnLongClickListener(v -> {
            saveGame(true);
            return true;
        });

        adapter.setOnItemClickListener(position -> {
            GameItem item = gameList.get(position);
            StringBuilder message = new StringBuilder();
            message.append("Title: ").append(item.getTitle()).append("\n");
            message.append("Note: ").append(item.getNotes()).append("\n");
            message.append("Status: ").append(item.getStatus()).append("\n");
            if (item.getLocation() != null && !item.getLocation().isEmpty()) {
                message.append("Location: ").append(item.getLocation()).append("\n");
            }
            if (item.getImageUri() != null) {
                message.append("Image: Set\n");
            }

            new AlertDialog.Builder(this)
                    .setTitle("Game details")
                    .setMessage(message.toString())
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

    // --- Feature 1: Location Fetching Flow ---
    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLastLocation();
        }
    }

    private void getLastLocation() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();
                    textLatitude.setText("Lat: " + lat);
                    textLongitude.setText("Long: " + lon);
                    currentFetchedLocation = lat + ", " + lon;
                    Toast.makeText(GamesActivity.this, "Location fetched!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(GamesActivity.this, "Unable to get location. Ensure GPS is ON.", Toast.LENGTH_LONG).show();
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    // --- Feature 2: Image Picker Flow ---
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            imagePreview.setImageURI(selectedImageUri);
            imagePreview.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Image selected!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveGame(boolean isCompleted) {
        String title = titleInput.getText().toString();
        if (title.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        String notes = notesInput.getText().toString();
        String status = isCompleted ? "Completed" : "In progress";
        
        // Save with image and location
        String imgUriString = (selectedImageUri != null) ? selectedImageUri.toString() : null;
        gameList.add(new GameItem(title, notes, status, imgUriString, currentFetchedLocation));
        
        adapter.notifyItemInserted(gameList.size() - 1);
        updateGameCount();
        
        // Reset fields
        titleInput.setText("");
        notesInput.setText("");
        imagePreview.setVisibility(View.GONE);
        selectedImageUri = null;
        textLatitude.setText("Lat: --");
        textLongitude.setText("Long: --");
        currentFetchedLocation = "";
        
        Toast.makeText(this, isCompleted ? "Saved as completed" : "Game saved", Toast.LENGTH_SHORT).show();
    }

    private void updateGameCount() {
        totalGames.setText(gameList.size() + " Titles Tracked");
        if (gameList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
        }
    }
}
