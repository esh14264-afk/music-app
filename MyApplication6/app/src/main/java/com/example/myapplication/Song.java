package com.example.myapplication;

import android.net.Uri;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;

public class Song {
    private final String id;
    private final String title;
    private final String artist;
    private final String album;
    private final String mediaUrl;
    private final String artworkUrl;

    public Song(String id, String title, String artist, String album, String mediaUrl, String artworkUrl) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.mediaUrl = mediaUrl;
        this.artworkUrl = artworkUrl;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public String getArtworkUrl() {
        return artworkUrl;
    }

    public MediaItem toMediaItem() {
        MediaMetadata metadata = new MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(artworkUrl != null ? Uri.parse(artworkUrl) : null)
                .build();

        return new MediaItem.Builder()
                .setMediaId(id)
                .setUri(mediaUrl)
                .setMediaMetadata(metadata)
                .build();
    }
}