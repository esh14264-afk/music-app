package com.example.myapplication;

import android.Manifest;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private final List<Song> playlist = new ArrayList<>();
    private SongAdapter songAdapter;

    private TextView tvNowPlayingTitle;
    private TextView tvNowPlayingArtist;
    private TextView tvCurrentTime;
    private TextView tvTotalDuration;
    private SeekBar seekBar;
    private FloatingActionButton fabPlayPause;
    private ImageButton btnPrevious;
    private ImageButton btnNext;

    private ListenableFuture<MediaController> controllerFuture;
    private MediaController mediaController;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private boolean isUserSeeking = false;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "Notification permission required for media controls", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupPlaylistData();
        setupRecyclerView();
        setupSeekBarListener();
        checkNotificationPermission();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializeMediaController();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopProgressUpdates();
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
            mediaController = null;
        }
    }

    private void initViews() {
        tvNowPlayingTitle = findViewById(R.id.tvNowPlayingTitle);
        tvNowPlayingArtist = findViewById(R.id.tvNowPlayingArtist);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalDuration = findViewById(R.id.tvTotalDuration);
        seekBar = findViewById(R.id.seekBar);
        fabPlayPause = findViewById(R.id.fabPlayPause);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);

        fabPlayPause.setOnClickListener(v -> togglePlayPause());
        btnPrevious.setOnClickListener(v -> {
            if (mediaController != null && mediaController.hasPreviousMediaItem()) {
                mediaController.seekToPreviousMediaItem();
            }
        });
        btnNext.setOnClickListener(v -> {
            if (mediaController != null && mediaController.hasNextMediaItem()) {
                mediaController.seekToNextMediaItem();
            }
        });
    }

    private void setupPlaylistData() {
        playlist.add(new Song("1", "SoundHelix Song 1", "SoundHelix Artist", "Album 1",
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", null));
        playlist.add(new Song("2", "SoundHelix Song 2", "SoundHelix Artist", "Album 1",
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3", null));
        playlist.add(new Song("3", "SoundHelix Song 3", "SoundHelix Artist", "Album 1",
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3", null));
        playlist.add(new Song("4", "SoundHelix Song 4", "SoundHelix Artist", "Album 1",
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3", null));
        playlist.add(new Song("5", "SoundHelix Song 5", "SoundHelix Artist", "Album 1",
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3", null));
    }

    private void setupRecyclerView() {
        RecyclerView rvPlaylist = findViewById(R.id.rvPlaylist);
        rvPlaylist.setLayoutManager(new LinearLayoutManager(this));
        songAdapter = new SongAdapter(playlist, (song, position) -> playSongAtPosition(position));
        rvPlaylist.setAdapter(songAdapter);
    }

    private void setupSeekBarListener() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
                if (mediaController != null) {
                    mediaController.seekTo(seekBar.getProgress());
                }
            }
        });
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void initializeMediaController() {
        ComponentName sessionToken = new ComponentName(this, PlaybackService.class);
        SessionToken token = new SessionToken(this, sessionToken);

        controllerFuture = new MediaController.Builder(this, token).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
                setupControllerListener();
                ensurePlaylistLoaded();
                syncUIWithController();
            } catch (ExecutionException | InterruptedException e) {
                Log.e("MainActivity", "Error initializing MediaController", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void setupControllerListener() {
        if (mediaController == null) return;

        mediaController.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updatePlayPauseButton(isPlaying);
                if (isPlaying) {
                    startProgressUpdates();
                } else {
                    stopProgressUpdates();
                }
            }

            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                updateNowPlayingInfo();
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                updateNowPlayingInfo();
            }
        });
    }

    private void ensurePlaylistLoaded() {
        if (mediaController == null) return;

        if (mediaController.getMediaItemCount() == 0) {
            List<MediaItem> mediaItems = new ArrayList<>();
            for (Song song : playlist) {
                mediaItems.add(song.toMediaItem());
            }
            mediaController.setMediaItems(mediaItems);
            mediaController.prepare();
        }
    }

    private void syncUIWithController() {
        if (mediaController == null) return;

        updatePlayPauseButton(mediaController.isPlaying());
        updateNowPlayingInfo();
        if (mediaController.isPlaying()) {
            startProgressUpdates();
        }
    }

    private void playSongAtPosition(int position) {
        if (mediaController == null) return;

        ensurePlaylistLoaded();
        mediaController.seekTo(position, 0);
        mediaController.prepare();
        mediaController.play();
        songAdapter.setSelectedPosition(position);
    }

    private void togglePlayPause() {
        if (mediaController == null) return;

        if (mediaController.getMediaItemCount() == 0) {
            playSongAtPosition(0);
            return;
        }

        if (mediaController.isPlaying()) {
            mediaController.pause();
        } else {
            mediaController.play();
        }
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        fabPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    private void updateNowPlayingInfo() {
        if (mediaController == null) return;

        int currentMediaItemIndex = mediaController.getCurrentMediaItemIndex();
        if (currentMediaItemIndex >= 0 && currentMediaItemIndex < playlist.size()) {
            Song currentSong = playlist.get(currentMediaItemIndex);
            tvNowPlayingTitle.setText(currentSong.getTitle());
            tvNowPlayingArtist.setText(currentSong.getArtist());
            songAdapter.setSelectedPosition(currentMediaItemIndex);
        } else {
            tvNowPlayingTitle.setText("No Song Selected");
            tvNowPlayingArtist.setText("Select a song below to start playing");
            songAdapter.setSelectedPosition(-1);
        }

        long duration = mediaController.getDuration();
        if (duration > 0) {
            seekBar.setMax((int) duration);
            tvTotalDuration.setText(formatTime(duration));
        } else {
            seekBar.setMax(100);
            tvTotalDuration.setText("00:00");
        }

        updateProgress();
    }

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            progressHandler.postDelayed(this, 500);
        }
    };

    private void startProgressUpdates() {
        stopProgressUpdates();
        progressHandler.post(progressRunnable);
    }

    private void stopProgressUpdates() {
        progressHandler.removeCallbacks(progressRunnable);
    }

    private void updateProgress() {
        if (mediaController == null || isUserSeeking) return;

        long currentPosition = mediaController.getCurrentPosition();
        seekBar.setProgress((int) currentPosition);
        tvCurrentTime.setText(formatTime(currentPosition));
    }

    private String formatTime(long millis) {
        long validMillis = Math.max(0, millis);
        long seconds = (validMillis / 1000) % 60;
        long minutes = (validMillis / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}