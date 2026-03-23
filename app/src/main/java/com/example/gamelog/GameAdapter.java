package com.example.gamelog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class GameAdapter extends RecyclerView.Adapter<GameAdapter.GameViewHolder> {

    private ArrayList<GameItem> gameList;
    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public GameAdapter(ArrayList<GameItem> gameList) {
        this.gameList = gameList;
    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_game, parent, false);
        return new GameViewHolder(view, listener, longClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        GameItem currentItem = gameList.get(position);
        holder.title.setText(currentItem.getTitle());
        holder.notes.setText(currentItem.getNotes());
        holder.status.setText(currentItem.getStatus());
    }

    @Override
    public int getItemCount() {
        return gameList.size();
    }

    static class GameViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView notes;
        TextView status;

        GameViewHolder(@NonNull View itemView, final OnItemClickListener listener, final OnItemLongClickListener longClickListener) {
            super(itemView);
            title = itemView.findViewById(R.id.game_title);
            notes = itemView.findViewById(R.id.game_notes);
            status = itemView.findViewById(R.id.game_status);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(position);
                    }
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        longClickListener.onItemLongClick(position);
                        return true;
                    }
                }
                return false;
            });
        }
    }
}
