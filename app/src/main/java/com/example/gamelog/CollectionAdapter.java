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

public class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.CollectionViewHolder> {

    private final List<CollectionEntryItem> items;
    private OnCollectionClickListener onCollectionClickListener;

    public interface OnCollectionClickListener {
        void onCollectionClick(CollectionEntryItem item);
    }

    public CollectionAdapter(List<CollectionEntryItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public void setOnCollectionClickListener(OnCollectionClickListener onCollectionClickListener) {
        this.onCollectionClickListener = onCollectionClickListener;
    }

    public void updateItems(List<CollectionEntryItem> updatedItems) {
        items.clear();
        if (updatedItems != null) {
            items.addAll(updatedItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CollectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_collection_entry, parent, false);
        return new CollectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CollectionViewHolder holder, int position) {
        holder.bind(items.get(position), onCollectionClickListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CollectionViewHolder extends RecyclerView.ViewHolder {
        private final ImageView icon;
        private final TextView title;
        private final TextView description;
        private final TextView statusBadge;
        private final TextView favouriteBadge;
        private final TextView metadata;

        CollectionViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.collection_icon);
            title = itemView.findViewById(R.id.collection_title);
            description = itemView.findViewById(R.id.collection_description);
            statusBadge = itemView.findViewById(R.id.collection_status_badge);
            favouriteBadge = itemView.findViewById(R.id.collection_favourite_badge);
            metadata = itemView.findViewById(R.id.collection_metadata);
        }

        void bind(CollectionEntryItem item, OnCollectionClickListener listener) {
            title.setText(orFallback(item.getTitle(), "Untitled game"));

            String rawDescription = item.getDescription();
            if (isBlank(rawDescription)) {
                description.setText("No description available yet.");
            } else {
                String trimmed = rawDescription.trim();
                description.setText(trimmed.length() > 140 ? trimmed.substring(0, 137) + "..." : trimmed);
            }

            statusBadge.setText(buildStatusText(item.getStatus()));
            boolean isFavourite = Boolean.TRUE.equals(item.getIsFavourite());
            favouriteBadge.setVisibility(isFavourite ? View.VISIBLE : View.GONE);

            metadata.setText(buildMetadata(item));
            ImageLoader.loadCover(icon, item.getCoverImageUrl(), R.drawable.ic_games);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCollectionClick(item);
                }
            });
        }

        private static String buildStatusText(String status) {
            if (isBlank(status)) {
                return "Status: unknown";
            }
            return "Status: " + status.trim();
        }

        private static String buildMetadata(CollectionEntryItem item) {
            StringBuilder builder = new StringBuilder();

            if (!isBlank(item.getReleaseDate())) {
                builder.append(item.getReleaseDate().trim());
            }
            if (!isBlank(item.getPlatform())) {
                appendSeparator(builder);
                builder.append(item.getPlatform().trim());
            }
            if (!isBlank(item.getDeveloper())) {
                appendSeparator(builder);
                builder.append(item.getDeveloper().trim());
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
