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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectionNotesAdapter extends RecyclerView.Adapter<CollectionNotesAdapter.NoteViewHolder> {

    private final List<CollectionNoteItem> items;
    private final Set<String> mutationInFlightNoteIds = new HashSet<>();
    private OnNoteMutationListener onNoteMutationListener;

    public interface OnNoteMutationListener {
        void onTogglePin(CollectionNoteItem item);
        void onToggleTaskStatus(CollectionNoteItem item, String nextStatus);
    }

    public CollectionNotesAdapter(List<CollectionNoteItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public void updateItems(List<CollectionNoteItem> updatedItems) {
        items.clear();
        if (updatedItems != null) {
            items.addAll(updatedItems);
        }
        notifyDataSetChanged();
    }

    public void setOnNoteMutationListener(OnNoteMutationListener onNoteMutationListener) {
        this.onNoteMutationListener = onNoteMutationListener;
    }

    public void setMutationInFlightNoteIds(Set<String> noteIds) {
        mutationInFlightNoteIds.clear();
        if (noteIds != null) {
            mutationInFlightNoteIds.addAll(noteIds);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_collection_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        CollectionNoteItem item = items.get(position);
        boolean inFlight = item.getNoteId() != null && mutationInFlightNoteIds.contains(item.getNoteId());
        holder.bind(item, onNoteMutationListener, inFlight);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView body;
        private final TextView pinnedBadge;
        private final TextView statusBadge;
        private final TextView indicators;
        private final TextView metadata;
        private final MaterialButton pinButton;
        private final MaterialButton taskButton;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.note_item_title);
            body = itemView.findViewById(R.id.note_item_body);
            pinnedBadge = itemView.findViewById(R.id.note_item_pinned_badge);
            statusBadge = itemView.findViewById(R.id.note_item_status_badge);
            indicators = itemView.findViewById(R.id.note_item_indicators);
            metadata = itemView.findViewById(R.id.note_item_metadata);
            pinButton = itemView.findViewById(R.id.note_item_pin_button);
            taskButton = itemView.findViewById(R.id.note_item_task_button);
        }

        void bind(CollectionNoteItem item, OnNoteMutationListener mutationListener, boolean inFlight) {
            boolean isReminder = "reminder".equalsIgnoreCase(orBlank(item.getNoteType()));
            title.setText(orFallback(item.getTitle(), isReminder ? "Untitled reminder" : "Untitled note"));
            body.setText(orFallback(item.getNoteText(), "No content available yet."));

            boolean pinned = Boolean.TRUE.equals(item.getIsPinned());
            pinnedBadge.setVisibility(pinned ? View.VISIBLE : View.GONE);

            bindMutationButtons(item, mutationListener, inFlight, isReminder, pinned);

            bindStatus(item, isReminder);
            bindIndicators(item, isReminder);
            bindMetadata(item, isReminder);
        }

        private void bindMutationButtons(CollectionNoteItem item,
                                         OnNoteMutationListener mutationListener,
                                         boolean inFlight,
                                         boolean isReminder,
                                         boolean pinned) {
            pinButton.setText(inFlight ? "Updating..." : (pinned ? "Unpin" : "Pin"));
            pinButton.setEnabled(!inFlight && !TextUtils.isEmpty(orBlank(item.getNoteId())));
            pinButton.setAlpha(pinButton.isEnabled() ? 1f : 0.6f);
            pinButton.setOnClickListener(v -> {
                if (mutationListener != null && !TextUtils.isEmpty(orBlank(item.getNoteId()))) {
                    mutationListener.onTogglePin(item);
                }
            });

            if (!isReminder) {
                taskButton.setVisibility(View.GONE);
                return;
            }

            String normalized = orBlank(item.getTaskStatus()).trim().toLowerCase();
            String nextStatus = "completed".equals(normalized) ? "pending" : "completed";

            taskButton.setVisibility(View.VISIBLE);
            taskButton.setText(inFlight ? "Updating..." : ("completed".equals(normalized) ? "Mark Pending" : "Mark Completed"));
            taskButton.setEnabled(!inFlight && !TextUtils.isEmpty(orBlank(item.getNoteId())));
            taskButton.setAlpha(taskButton.isEnabled() ? 1f : 0.6f);
            taskButton.setOnClickListener(v -> {
                if (mutationListener != null && !TextUtils.isEmpty(orBlank(item.getNoteId()))) {
                    mutationListener.onToggleTaskStatus(item, nextStatus);
                }
            });
        }

        private void bindStatus(CollectionNoteItem item, boolean isReminder) {
            if (!isReminder) {
                statusBadge.setVisibility(View.GONE);
                return;
            }

            String taskStatus = orBlank(item.getTaskStatus());
            if (TextUtils.isEmpty(taskStatus)) {
                statusBadge.setVisibility(View.GONE);
                return;
            }

            statusBadge.setVisibility(View.VISIBLE);
            String normalized = taskStatus.trim().toLowerCase();
            if ("completed".equals(normalized)) {
                statusBadge.setText("COMPLETED");
                statusBadge.setBackgroundResource(R.drawable.bg_completed_badge);
                statusBadge.setTextColor(itemView.getContext().getColor(R.color.status_success));
            } else {
                statusBadge.setText("PENDING");
                statusBadge.setBackgroundResource(R.drawable.bg_pending_badge);
                statusBadge.setTextColor(itemView.getContext().getColor(R.color.accent_neon));
            }
        }

        private void bindIndicators(CollectionNoteItem item, boolean isReminder) {
            List<String> flags = new ArrayList<>();
            if (!TextUtils.isEmpty(orBlank(item.getMediaUri()))) {
                flags.add("MEDIA");
            }
            if (item.getLatitude() != null && item.getLongitude() != null) {
                flags.add("LOCATION");
            }
            if (isReminder && !TextUtils.isEmpty(orBlank(item.getNextTriggerAt()))) {
                flags.add("SCHEDULED");
            }

            if (flags.isEmpty()) {
                indicators.setVisibility(View.GONE);
                return;
            }

            indicators.setVisibility(View.VISIBLE);
            indicators.setText(TextUtils.join("  |  ", flags));
        }

        private void bindMetadata(CollectionNoteItem item, boolean isReminder) {
            String created = orBlank(item.getCreatedAt());
            String completed = orBlank(item.getCompletedAt());
            String frequency = orBlank(item.getFrequency());

            StringBuilder builder = new StringBuilder();
            if (!TextUtils.isEmpty(created)) {
                builder.append("Created: ").append(created.trim());
            }
            if (isReminder && !TextUtils.isEmpty(frequency)) {
                appendSeparator(builder);
                builder.append("Every: ").append(frequency.trim());
            }
            if (isReminder && !TextUtils.isEmpty(completed)) {
                appendSeparator(builder);
                builder.append("Completed: ").append(completed.trim());
            }

            metadata.setText(builder.length() > 0 ? builder.toString() : "No metadata available");
        }

        private static void appendSeparator(StringBuilder builder) {
            if (builder.length() > 0) {
                builder.append("  |  ");
            }
        }

        private static String orBlank(String value) {
            return value == null ? "" : value;
        }

        private static String orFallback(String value, String fallback) {
            return TextUtils.isEmpty(orBlank(value).trim()) ? fallback : value.trim();
        }
    }
}
