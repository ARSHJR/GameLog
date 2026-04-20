package com.example.gamelog;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class GlobalNotesAdapter extends RecyclerView.Adapter<GlobalNotesAdapter.NoteViewHolder> {

    private final List<CollectionNoteItem> items;
    private OnNoteActionListener noteActionListener;

    public interface OnNoteActionListener {
        void onNoteClicked(CollectionNoteItem item);
        void onDeleteRequested(CollectionNoteItem item);
    }

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

    public void setOnNoteActionListener(OnNoteActionListener noteActionListener) {
        this.noteActionListener = noteActionListener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_global_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        holder.bind(items.get(position), noteActionListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {

        private final TextView title;
        private final TextView body;
        private final TextView gameTitle;
        private final TextView metadata;
        private final MaterialButton deleteButton;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.global_note_title);
            body = itemView.findViewById(R.id.global_note_body);
            gameTitle = itemView.findViewById(R.id.global_note_game_title);
            metadata = itemView.findViewById(R.id.global_note_metadata);
            deleteButton = itemView.findViewById(R.id.global_note_delete_button);
        }

        void bind(CollectionNoteItem item, OnNoteActionListener noteActionListener) {
            title.setText(orFallback(item.getTitle(), "Untitled note"));
            body.setText(orFallback(item.getNoteText(), "No content available."));
            gameTitle.setText("Game: " + orFallback(item.getGameTitle(), "Unknown game"));

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

            deleteButton.setOnClickListener(v -> {
                if (noteActionListener != null) {
                    noteActionListener.onDeleteRequested(item);
                }
            });

            itemView.setOnClickListener(v -> {
                if (noteActionListener != null) {
                    noteActionListener.onNoteClicked(item);
                }
            });
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
