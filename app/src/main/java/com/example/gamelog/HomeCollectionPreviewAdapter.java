package com.example.gamelog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class HomeCollectionPreviewAdapter extends RecyclerView.Adapter<HomeCollectionPreviewAdapter.PreviewViewHolder> {

    private final List<CollectionEntryItem> items;
    private OnPreviewClickListener onPreviewClickListener;

    public interface OnPreviewClickListener {
        void onPreviewClick(CollectionEntryItem item);
    }

    public HomeCollectionPreviewAdapter(List<CollectionEntryItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public void setOnPreviewClickListener(OnPreviewClickListener onPreviewClickListener) {
        this.onPreviewClickListener = onPreviewClickListener;
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
    public PreviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_collection_preview, parent, false);
        return new PreviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PreviewViewHolder holder, int position) {
        holder.bind(items.get(position), onPreviewClickListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class PreviewViewHolder extends RecyclerView.ViewHolder {

        private final TextView title;
        private final TextView subtitle;
        private final TextView status;

        PreviewViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.home_preview_title);
            subtitle = itemView.findViewById(R.id.home_preview_subtitle);
            status = itemView.findViewById(R.id.home_preview_status);
        }

        void bind(CollectionEntryItem item, OnPreviewClickListener listener) {
            title.setText(orFallback(item.getTitle(), "Untitled game"));

            String description = orFallback(item.getDescription(), "No description available yet.");
            subtitle.setText(description.length() > 90 ? description.substring(0, 87) + "..." : description);

            String statusText = orFallback(item.getStatus(), "unknown");
            status.setText("Status: " + statusText);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPreviewClick(item);
                }
            });
        }

        private static String orFallback(String value, String fallback) {
            return value == null || value.trim().isEmpty() ? fallback : value.trim();
        }
    }
}
