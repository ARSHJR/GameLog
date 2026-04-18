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

public class GlobalNotesAdapter extends RecyclerView.Adapter<GlobalNotesAdapter.NoteViewHolder> {

    private final List<CollectionNoteItem> items;

    public GlobalNotesAdapter(List<CollectionNoteItem> items) {
        this.items = items != null ? items : new ArrayList<>();
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
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_global_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {

        private final TextView title;
        private final TextView body;
        private final TextView gameTitle;
        private final TextView pinnedBadge;
        private final TextView metadata;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.global_note_title);
            body = itemView.findViewById(R.id.global_note_body);
            gameTitle = itemView.findViewById(R.id.global_note_game_title);
            pinnedBadge = itemView.findViewById(R.id.global_note_pinned_badge);
            metadata = itemView.findViewById(R.id.global_note_metadata);
        }

        void bind(CollectionNoteItem item) {
            title.setText(orFallback(item.getTitle(), "Untitled note"));
            body.setText(orFallback(item.getNoteText(), "No content available."));
            gameTitle.setText("Game: " + orFallback(item.getGameTitle(), "Unknown game"));

            boolean pinned = Boolean.TRUE.equals(item.getIsPinned());
            pinnedBadge.setVisibility(pinned ? View.VISIBLE : View.GONE);

            StringBuilder builder = new StringBuilder();
            if (!TextUtils.isEmpty(orBlank(item.getCreatedAt()))) {
                builder.append("Created: ").append(item.getCreatedAt().trim());
            }
            if (!TextUtils.isEmpty(orBlank(item.getMediaUri()))) {
                if (builder.length() > 0) {
                    builder.append("  |  ");
                }
                builder.append("Has media");
            }
            metadata.setText(builder.length() > 0 ? builder.toString() : "No metadata available");
        }

        private static String orBlank(String value) {
            return value == null ? "" : value;
        }

        private static String orFallback(String value, String fallback) {
            String normalized = orBlank(value).trim();
            return normalized.isEmpty() ? fallback : normalized;
        }
    }
}
