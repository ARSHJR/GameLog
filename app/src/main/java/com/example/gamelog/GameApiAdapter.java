package com.example.gamelog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying games from the API in a RecyclerView
 */
public class GameApiAdapter extends RecyclerView.Adapter<GameApiAdapter.GameViewHolder> {

    private final List<GameApiItem> gamesList;
    private OnGameClickListener onGameClickListener;

    public interface OnGameClickListener {
        void onGameClick(GameApiItem game);
    }

    public GameApiAdapter(List<GameApiItem> gamesList) {
        this.gamesList = gamesList != null ? gamesList : new ArrayList<>();
    }

    public void setOnGameClickListener(OnGameClickListener onGameClickListener) {
        this.onGameClickListener = onGameClickListener;
    }

    public void updateGames(List<GameApiItem> updatedGames) {
        gamesList.clear();
        if (updatedGames != null) {
            gamesList.addAll(updatedGames);
        }
        notifyDataSetChanged();
    }

    public List<GameApiItem> getGames() {
        return new ArrayList<>(gamesList);
    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_game_api, parent, false);
        return new GameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        GameApiItem game = gamesList.get(position);
        holder.bind(game, onGameClickListener);
    }

    @Override
    public int getItemCount() {
        return gamesList != null ? gamesList.size() : 0;
    }

    public static class GameViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView descriptionText;
        TextView genresText;
        TextView metaText;
        ImageView coverImage;

        public GameViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.text_title);
            descriptionText = itemView.findViewById(R.id.text_description);
            genresText = itemView.findViewById(R.id.text_genres);
            metaText = itemView.findViewById(R.id.text_meta);
            coverImage = itemView.findViewById(R.id.image_cover);
        }

        void bind(GameApiItem game, OnGameClickListener clickListener) {
            String title = game.getTitle();
            titleText.setText(isBlank(title) ? "Untitled game" : title);

            String description = game.getDescription();
            if (isBlank(description)) {
                descriptionText.setText("No description available yet.");
            } else {
                String compact = description.trim();
                descriptionText.setText(compact.length() > 140 ? compact.substring(0, 137) + "..." : compact);
            }

            List<String> genres = game.getGenres();
            if (genres == null || genres.isEmpty()) {
                genresText.setVisibility(View.GONE);
            } else {
                genresText.setVisibility(View.VISIBLE);
                genresText.setText(joinGenres(genres));
            }

            String meta = buildMetaLine(game);
            metaText.setText(meta);

            ImageLoader.loadCover(coverImage, game.getCoverImageUrl(), R.drawable.ic_explore);

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onGameClick(game);
                }
            });
        }

        private static String buildMetaLine(GameApiItem game) {
            boolean hasReleaseDate = !isBlank(game.getReleaseDate());
            boolean hasPlatform = !isBlank(game.getPlatform());
            if (hasReleaseDate && hasPlatform) {
                return game.getReleaseDate().trim() + "  |  " + game.getPlatform().trim();
            }
            if (hasReleaseDate) {
                return game.getReleaseDate().trim();
            }
            if (hasPlatform) {
                return game.getPlatform().trim();
            }
            return "Platform and release info unavailable";
        }

        private static String joinGenres(List<String> genres) {
            StringBuilder builder = new StringBuilder();
            for (String genre : genres) {
                if (isBlank(genre)) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append("  |  ");
                }
                builder.append(genre.trim());
            }
            return builder.length() > 0 ? builder.toString() : "Genres unavailable";
        }

        private static boolean isBlank(String value) {
            return value == null || value.trim().isEmpty();
        }
    }
}
