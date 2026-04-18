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
import java.util.Locale;

public class UserActivityAdapter extends RecyclerView.Adapter<UserActivityAdapter.ActivityViewHolder> {

    private final List<UserActivityItem> items;

    public UserActivityAdapter(List<UserActivityItem> initialItems) {
        items = initialItems != null ? initialItems : new ArrayList<>();
    }

    public void updateItems(List<UserActivityItem> updatedItems) {
        items.clear();
        if (updatedItems != null) {
            items.addAll(updatedItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_activity, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {

        private final TextView title;
        private final TextView subtitle;
        private final TextView timestamp;

        ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.activity_item_title);
            subtitle = itemView.findViewById(R.id.activity_item_subtitle);
            timestamp = itemView.findViewById(R.id.activity_item_timestamp);
        }

        void bind(UserActivityItem item) {
            String action = orBlank(item.getActionType());
            if (TextUtils.isEmpty(action)) {
                action = "Activity event";
            }

            title.setText(formatAction(action));

            String entityType = orBlank(item.getEntityType());
            String entityId = orBlank(item.getEntityId());
            String durationPart = item.getDurationSeconds() != null && item.getDurationSeconds() > 0
                    ? "Duration: " + item.getDurationSeconds() + "s"
                    : "Duration: n/a";

            String entityPart;
            if (!TextUtils.isEmpty(entityType) && !TextUtils.isEmpty(entityId)) {
                entityPart = entityType + " #" + entityId;
            } else if (!TextUtils.isEmpty(entityType)) {
                entityPart = entityType;
            } else {
                entityPart = "General event";
            }

            subtitle.setText(entityPart + " • " + durationPart);
            timestamp.setText(orFallback(item.getOccurredAt(), "Time unavailable"));
        }

        private String formatAction(String rawAction) {
            String normalized = rawAction.trim().replace('_', ' ');
            if (TextUtils.isEmpty(normalized)) {
                return "Activity event";
            }
            String lower = normalized.toLowerCase(Locale.US);
            return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
        }

        private static String orBlank(String value) {
            return value == null ? "" : value;
        }

        private static String orFallback(String value, String fallback) {
            String trimmed = value == null ? "" : value.trim();
            return trimmed.isEmpty() ? fallback : trimmed;
        }
    }
}
