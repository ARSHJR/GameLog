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

public class FavouriteGameAdapter extends RecyclerView.Adapter<FavouriteGameAdapter.FavouriteGameViewHolder> {

    private final List<FavouriteGameItem> items;
    private OnFavouriteClickListener onFavouriteClickListener;

    public interface OnFavouriteClickListener {
        void onFavouriteClick(FavouriteGameItem item);
    }

    public FavouriteGameAdapter(List<FavouriteGameItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public void setOnFavouriteClickListener(OnFavouriteClickListener onFavouriteClickListener) {
        this.onFavouriteClickListener = onFavouriteClickListener;
    }

    public void updateItems(List<FavouriteGameItem> updatedItems) {
        items.clear();
        if (updatedItems != null) {
            items.addAll(updatedItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FavouriteGameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favourite_game, parent, false);
        return new FavouriteGameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavouriteGameViewHolder holder, int position) {
        FavouriteGameItem item = items.get(position);
        holder.bind(item, onFavouriteClickListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class FavouriteGameViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleText;
        private final TextView descriptionText;
        private final TextView metadataText;
        private final TextView badgeText;
        private final ImageView coverIcon;

        FavouriteGameViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.favourite_title);
            descriptionText = itemView.findViewById(R.id.favourite_description);
            metadataText = itemView.findViewById(R.id.favourite_metadata);
            badgeText = itemView.findViewById(R.id.favourite_badge);
            coverIcon = itemView.findViewById(R.id.favourite_cover_icon);
        }

        void bind(FavouriteGameItem item, OnFavouriteClickListener listener) {
            titleText.setText(orFallback(item.getTitle(), "Untitled game"));

            String description = item.getDescription();
            if (isBlank(description)) {
                descriptionText.setText("No description available yet.");
            } else {
                String compact = description.trim();
                descriptionText.setText(compact.length() > 140 ? compact.substring(0, 137) + "..." : compact);
            }

            badgeText.setText("FAVOURITE");
            metadataText.setText(buildMetadata(item));

            coverIcon.setImageResource(R.drawable.ic_favourite);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFavouriteClick(item);
                }
            });
        }

        private static String buildMetadata(FavouriteGameItem item) {
            StringBuilder builder = new StringBuilder();

            if (!isBlank(item.getReleaseDate())) {
                builder.append(item.getReleaseDate().trim());
            }
            if (!isBlank(item.getPlatform())) {
                appendSeparator(builder);
                builder.append(item.getPlatform().trim());
            }
            if (!isBlank(item.getStatus())) {
                appendSeparator(builder);
                builder.append("Status: ").append(item.getStatus().trim());
            }

            return builder.length() > 0 ? builder.toString() : "No metadata available";
        }

        private static void appendSeparator(StringBuilder builder) {
            if (builder.length() > 0) {
                builder.append("  |  ");
            }
        }

        private static String orFallback(String value, String fallback) {
            return isBlank(value) ? fallback : value.trim();
        }

        private static boolean isBlank(String value) {
            return value == null || value.trim().isEmpty();
        }
    }
}
