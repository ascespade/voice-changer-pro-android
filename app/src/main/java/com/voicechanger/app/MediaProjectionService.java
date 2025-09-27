package com.voicechanger.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * A foreground service to manage MediaProjection for system audio capture.
 * This service ensures that MediaProjection can run in the background
 * and provides a persistent notification to the user.
 */
public class MediaProjectionService extends Service {
    private static final String TAG = "MediaProjectionService";
    private static final int NOTIFICATION_ID = 12345;
    private static final String NOTIFICATION_CHANNEL_ID = "MediaProjectionChannel";

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private SystemWideAudioService systemWideAudioService;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MediaProjectionService onCreate");
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "MediaProjectionService onStartCommand");

        if (intent != null) {
            String action = intent.getAction();
            if (action != null && action.equals("START_PROJECTION")) {
                int resultCode = intent.getIntExtra("resultCode", 0);
                Intent data = intent.getParcelableExtra("data");

                if (resultCode != 0 && data != null) {
                    startForeground(NOTIFICATION_ID, createNotification());
                    mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
                    if (mediaProjection != null) {
                        Log.d(TAG, "MediaProjection started successfully.");
                        // Pass the MediaProjection to the SystemWideAudioService
                        if (systemWideAudioService != null) {
                            systemWideAudioService.setMediaProjection(mediaProjection);
                        }
                    } else {
                        Log.e(TAG, "Failed to get MediaProjection.");
                        stopSelf();
                    }
                } else {
                    Log.e(TAG, "Invalid resultCode or data for MediaProjection.");
                    stopSelf();
                }
            } else if (action != null && action.equals("STOP_PROJECTION")) {
                stopMediaProjection();
                stopSelf();
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MediaProjectionService onDestroy");
        stopMediaProjection();
    }

    private void stopMediaProjection() {
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
            Log.d(TAG, "MediaProjection stopped.");
        }
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Media Projection Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification createNotification() {
        return new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Voice Changer Pro")
                .setContentText("Capturing system audio for voice transformation.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
    }

    // Method to set the SystemWideAudioService instance
    public void setSystemWideAudioService(SystemWideAudioService service) {
        this.systemWideAudioService = service;
    }
}

