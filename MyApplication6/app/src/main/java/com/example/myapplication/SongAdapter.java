package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    public interface OnSongClickListener {
        void onSongClick(Song song, int position);
    }

    private final List<Song> songList;
    private final OnSongClickListener clickListener;
    private int selectedPosition = -1;

    public SongAdapter(List<Song> songList, OnSongClickListener clickListener) {
        this.songList = songList;
        this.clickListener = clickListener;
    }

    public void setSelectedPosition(int position) {
        int previous = selectedPosition;
        selectedPosition = position;
        if (previous >= 0 && previous < songList.size()) {
            notifyItemChanged(previous);
        }
        if (selectedPosition >= 0 && selectedPosition < songList.size()) {
            notifyItemChanged(selectedPosition);
        }
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songList.get(position);
        holder.tvTitle.setText(song.getTitle());
        holder.tvArtist.setText(song.getArtist());

        boolean isSelected = (position == selectedPosition);
        holder.imgIndicator.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.cardSong.setStrokeWidth(isSelected ? 3 : 1);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onSongClick(song, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songList != null ? songList.size() : 0;
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardSong;
        TextView tvTitle;
        TextView tvArtist;
        ImageView imgIndicator;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            cardSong = itemView.findViewById(R.id.cardSong);
            tvTitle = itemView.findViewById(R.id.tvSongTitle);
            tvArtist = itemView.findViewById(R.id.tvSongArtist);
            imgIndicator = itemView.findViewById(R.id.imgPlayingIndicator);
        }
    }
}