package com.voicechanger.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

/**
 * Background service that handles voice processing to ensure continuous operation
 * even when the app is not in the foreground.
 */
public class VoiceProcessingService extends Service implements VoiceProcessingEngine.VoiceProcessingListener {
    private static final String TAG = "VoiceProcessingService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "voice_processing_channel";
    
    private VoiceProcessingEngine voiceEngine;
    private final IBinder binder = new VoiceProcessingBinder();
    private ServiceListener serviceListener;
    
    public interface ServiceListener {
        void onProcessingStarted();
        void onProcessingStopped();
        void onError(String error);
        void onAudioLevelChanged(float level);
    }
    
    public class VoiceProcessingBinder extends Binder {
        public VoiceProcessingService getService() {
            return VoiceProcessingService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        
        voiceEngine = new VoiceProcessingEngine(this);
        voiceEngine.setListener(this);
        
        createNotificationChannel();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        
        // Start as foreground service to prevent being killed by the system
        startForeground(NOTIFICATION_ID, createNotification("Voice Changer Ready"));
        
        return START_STICKY; // Restart if killed by system
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        
        if (voiceEngine != null) {
            voiceEngine.stopRealTimeProcessing();
            voiceEngine.release();
        }
    }
    
    public void setServiceListener(ServiceListener listener) {
        this.serviceListener = listener;
    }
    
    public void setApiKey(String apiKey) {
        if (voiceEngine != null) {
            voiceEngine.setApiKey(apiKey);
        }
    }
    
    public void setSelectedVoiceId(String voiceId) {
        if (voiceEngine != null) {
            voiceEngine.setSelectedVoiceId(voiceId);
        }
    }
    
    public void startVoiceProcessing() {
        if (voiceEngine != null) {
            voiceEngine.startRealTimeProcessing();
        }
    }
    
    public void stopVoiceProcessing() {
        if (voiceEngine != null) {
            voiceEngine.stopRealTimeProcessing();
        }
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Voice Processing",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Voice changer processing notifications");
            channel.setSound(null, null);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createNotification(String contentText) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Voice Changer")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_media_play) // Using system icon for now
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .build();
    }
    
    private void updateNotification(String contentText) {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, createNotification(contentText));
        }
    }
    
    // VoiceProcessingEngine.VoiceProcessingListener implementation
    @Override
    public void onProcessingStarted() {
        Log.d(TAG, "Voice processing started");
        updateNotification("Voice processing active");
        
        if (serviceListener != null) {
            serviceListener.onProcessingStarted();
        }
    }
    
    @Override
    public void onProcessingStopped() {
        Log.d(TAG, "Voice processing stopped");
        updateNotification("Voice Changer Ready");
        
        if (serviceListener != null) {
            serviceListener.onProcessingStopped();
        }
    }
    
    @Override
    public void onError(String error) {
        Log.e(TAG, "Voice processing error: " + error);
        updateNotification("Error: " + error);
        
        if (serviceListener != null) {
            serviceListener.onError(error);
        }
    }
    
    @Override
    public void onAudioLevelChanged(float level) {
        // Forward to service listener for UI updates
        if (serviceListener != null) {
            serviceListener.onAudioLevelChanged(level);
        }
    }
}

