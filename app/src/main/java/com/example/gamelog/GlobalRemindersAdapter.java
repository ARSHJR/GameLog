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

public class GlobalRemindersAdapter extends RecyclerView.Adapter<GlobalRemindersAdapter.ReminderViewHolder> {

    private final List<CollectionNoteItem> items;
    private final Set<String> mutationInFlightIds = new HashSet<>();
    private OnReminderActionListener actionListener;
    private OnReminderItemClickListener itemClickListener;

    public interface OnReminderActionListener {
        void onPinToggleRequested(CollectionNoteItem item, boolean nextPinned);
        void onTaskStatusToggleRequested(CollectionNoteItem item, String nextStatus);
        void onDeleteRequested(CollectionNoteItem item);
    }

    public interface OnReminderItemClickListener {
        void onReminderClicked(CollectionNoteItem item);
    }

    public GlobalRemindersAdapter(List<CollectionNoteItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public void setOnReminderActionListener(OnReminderActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void setOnReminderItemClickListener(OnReminderItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public void updateItems(List<CollectionNoteItem> updatedItems) {
        items.clear();
        if (updatedItems != null) {
            items.addAll(updatedItems);
        }
        notifyDataSetChanged();
    }

    public void setMutationInFlightIds(Set<String> noteIds) {
        mutationInFlightIds.clear();
        if (noteIds != null) {
            mutationInFlightIds.addAll(noteIds);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_global_reminder, parent, false);
        return new ReminderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        CollectionNoteItem item = items.get(position);
        boolean inFlight = item.getNoteId() != null && mutationInFlightIds.contains(item.getNoteId());
        holder.bind(item, inFlight, actionListener, itemClickListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ReminderViewHolder extends RecyclerView.ViewHolder {

        private final MaterialButton pinButton;
        private final MaterialButton completeButton;
        private final MaterialButton deleteButton;
        private final TextView title;
        private final TextView body;
        private final TextView gameTitle;
        private final TextView pinnedBadge;
        private final TextView statusBadge;

        ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            pinButton = itemView.findViewById(R.id.global_reminder_pin_button);
            completeButton = itemView.findViewById(R.id.global_reminder_complete_button);
            deleteButton = itemView.findViewById(R.id.global_reminder_delete_button);
            title = itemView.findViewById(R.id.global_reminder_title);
            body = itemView.findViewById(R.id.global_reminder_body);
            gameTitle = itemView.findViewById(R.id.global_reminder_game_title);
            pinnedBadge = itemView.findViewById(R.id.global_reminder_pinned_badge);
            statusBadge = itemView.findViewById(R.id.global_reminder_status_badge);
        }

        void bind(CollectionNoteItem item,
              boolean inFlight,
              OnReminderActionListener actionListener,
              OnReminderItemClickListener itemClickListener) {
            String normalizedStatus = orBlank(item.getTaskStatus()).trim().toLowerCase();
            boolean isCompleted = "completed".equals(normalizedStatus);
            String nextStatus = isCompleted ? "pending" : "completed";

            title.setText(orFallback(item.getTitle(), "Untitled reminder"));
            body.setText(orFallback(item.getNoteText(), "No reminder text"));
            gameTitle.setText("Game: " + orFallback(item.getGameTitle(), "Unknown game"));

            boolean pinned = Boolean.TRUE.equals(item.getIsPinned());
            boolean nextPinned = !pinned;
            pinnedBadge.setVisibility(pinned ? View.VISIBLE : View.GONE);
            pinButton.setText(pinned ? "Unpin" : "Pin");

            statusBadge.setText(isCompleted ? "COMPLETED" : "PENDING");
            statusBadge.setBackgroundResource(isCompleted ? R.drawable.bg_completed_badge : R.drawable.bg_pending_badge);
            statusBadge.setTextColor(itemView.getContext().getColor(isCompleted ? R.color.status_success : R.color.accent_neon));
            completeButton.setText(isCompleted ? "Mark pending" : "Mark completed");

            boolean canMutate = !inFlight && !TextUtils.isEmpty(orBlank(item.getNoteId()));

            pinButton.setEnabled(canMutate);
            pinButton.setAlpha(canMutate ? 1f : 0.6f);
            pinButton.setOnClickListener(v -> {
                if (actionListener != null && !TextUtils.isEmpty(orBlank(item.getNoteId()))) {
                    actionListener.onPinToggleRequested(item, nextPinned);
                }
            });

            completeButton.setEnabled(canMutate);
            completeButton.setAlpha(canMutate ? 1f : 0.6f);
            completeButton.setOnClickListener(v -> {
                if (actionListener != null && !TextUtils.isEmpty(orBlank(item.getNoteId()))) {
                    actionListener.onTaskStatusToggleRequested(item, nextStatus);
                }
            });

            deleteButton.setEnabled(!inFlight);
            deleteButton.setAlpha(deleteButton.isEnabled() ? 1f : 0.6f);
            deleteButton.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onDeleteRequested(item);
                }
            });

            itemView.setOnClickListener(v -> {
                if (itemClickListener != null) {
                    itemClickListener.onReminderClicked(item);
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
