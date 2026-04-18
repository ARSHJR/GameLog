package com.example.gamelog;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class DetailNotesPreviewAdapter extends RecyclerView.Adapter<DetailNotesPreviewAdapter.PreviewViewHolder> {

    private final List<CollectionNoteItem> items;

    public DetailNotesPreviewAdapter(List<CollectionNoteItem> initialItems) {
        items = initialItems != null ? initialItems : new ArrayList<>();
    }

    public void updateItems(List<CollectionNoteItem> updatedItems) {
        items.clear();
        if (updatedItems != null) {
            items.addAll(updatedItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PreviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_detail_note_preview, parent, false);
        return new PreviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PreviewViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class PreviewViewHolder extends RecyclerView.ViewHolder {

        private final TextView title;
        private final TextView type;
        private final TextView body;
        private final TextView meta;

        PreviewViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.detail_note_preview_title);
            type = itemView.findViewById(R.id.detail_note_preview_type);
            body = itemView.findViewById(R.id.detail_note_preview_body);
            meta = itemView.findViewById(R.id.detail_note_preview_meta);
        }

        void bind(CollectionNoteItem item) {
            String noteType = normalize(item.getNoteType());
            boolean reminder = "reminder".equals(noteType);

            title.setText(orFallback(item.getTitle(), reminder ? "Untitled reminder" : "Untitled note"));
            type.setText(reminder ? "REMINDER" : "NOTE");

            String noteBody = orFallback(item.getNoteText(), "No content available yet.");
            body.setText(noteBody);

            StringBuilder builder = new StringBuilder();
            if (Boolean.TRUE.equals(item.getIsPinned())) {
                builder.append("Pinned");
            }
            if (reminder && !TextUtils.isEmpty(normalize(item.getTaskStatus()))) {
                if (builder.length() > 0) {
                    builder.append("  |  ");
                }
                builder.append("Status: ").append(normalize(item.getTaskStatus()));
            }
            if (!TextUtils.isEmpty(normalize(item.getCreatedAt()))) {
                if (builder.length() > 0) {
                    builder.append("  |  ");
                }
                builder.append("Created: ").append(normalize(item.getCreatedAt()));
            }

            meta.setText(builder.length() > 0 ? builder.toString() : "No metadata available");
        }

        private String normalize(String value) {
            return value == null ? "" : value.trim();
        }

        private String orFallback(String value, String fallback) {
            String normalized = normalize(value);
            return normalized.isEmpty() ? fallback : normalized;
        }
    }
}
