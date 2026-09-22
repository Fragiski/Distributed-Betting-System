package com.example.casinoapp;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

public class MusicService extends Service {
    private MediaPlayer mediaPlayer;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Use applicationContext to avoid memory leaks associated with an Activity's lifecycle
        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.background_music);

        if (mediaPlayer != null) {
            mediaPlayer.setLooping(false);
            mediaPlayer.setVolume(0.5f, 0.5f);

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    // Manual audio loop/restart
                    mp.seekTo(0);
                    mp.start();
                }
            });
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
        // Tells the OS to recreate the service if it gets terminated due to low memory resources
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Properly stop and release player resources to prevent background memory leaks
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();// Clear reference for safety
        }
        super.onDestroy();
    }
}