package com.voicechanger.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.RequiresApi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * System-wide audio service that uses AccessibilityService to capture and process
 * audio from all applications on the device.
 * Enhanced with MediaProjection integration and robust error handling.
 */
@RequiresApi(api = Build.VERSION_CODES.Q)
public class SystemWideAudioService extends AccessibilityService {
    private static final String TAG = "SystemWideAudioService";
    
    // Audio configuration for system-wide capture
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    
    private AudioRecord audioRecord;
    private MediaProjection mediaProjection;
    private MediaProjectionManager mediaProjectionManager;
    public SystemWideVoiceProcessor voiceProcessor;
    private boolean isCapturing = false;
    
    // Set of communication app package names for efficient lookup
    private static final Set<String> COMMUNICATION_APPS = new HashSet<>(Arrays.asList(
            "com.whatsapp",
            "com.snapchat.android",
            "com.facebook.orca", // Messenger
            "com.viber.voip",
            "com.skype.raider",
            "us.zoom.videomeetings",
            "com.discord",
            "org.telegram.messenger",
            "com.google.android.apps.tachyon", // Google Duo/Meet
            "com.microsoft.teams",
            "com.tencent.mm", // WeChat
            "jp.naver.line.android", // LINE
            "com.kakao.talk", // KakaoTalk
            "com.android.server.telecom", // System calls
            "com.android.dialer"
    ));
    
    @Override
    public void onCreate() {
        super.onCreate();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Log.d(TAG, "SystemWideAudioService onCreate");
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "SystemWideAudioService connected");
        
        // Initialize voice processor with free AI models
        voiceProcessor = new SystemWideVoiceProcessor(this);
        
        // Request MediaProjection permission from user if not already granted
        // This part needs to be triggered from MainActivity or a user interaction
        // For now, we assume MediaProjection is already set if available.
        // startSystemWideCapture(); // This will be called when MediaProjection is ready or by user action
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Monitor accessibility events to detect when apps are using audio
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
            
            // Check if this is a communication app that we should process
            if (COMMUNICATION_APPS.contains(packageName)) {
                Log.d(TAG, "Communication app detected: " + packageName);
                ensureVoiceProcessingActive();
            }
        }
    }
    
    @Override
    public void onInterrupt() {
        Log.d(TAG, "SystemWideAudioService interrupted");
        stopSystemWideCapture();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "SystemWideAudioService destroyed");
        stopSystemWideCapture();
        
        if (voiceProcessor != null) {
            voiceProcessor.release();
        }
        
        // Stop MediaProjectionService if it was started by us
        Intent stopProjectionService = new Intent(this, MediaProjectionService.class);
        stopProjectionService.setAction("STOP_PROJECTION");
        startService(stopProjectionService);
    }
    
    /**
     * Starts system-wide audio capture. This method should be called after
     * MediaProjection permission is granted and MediaProjection object is set.
     */
    public void startSystemWideCapture() {
        if (isCapturing) {
            Log.w(TAG, "System-wide capture already active.");
            return;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaProjection == null) {
            Log.e(TAG, "MediaProjection not set. Cannot start system-wide capture on Android Q+.");
            // Fallback to microphone capture if MediaProjection is not available
            startMicrophoneCapture();
            return;
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaProjection != null) {
                startPlaybackCapture();
            } else {
                startMicrophoneCapture();
            }
            
            isCapturing = true;
            Log.d(TAG, "System-wide audio capture started.");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start system-wide capture", e);
            // Attempt fallback if initial capture method fails
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaProjection != null) {
                Log.w(TAG, "Playback capture failed, attempting microphone capture as fallback.");
                startMicrophoneCapture();
            } else {
                Log.e(TAG, "Failed to start any audio capture method.");
            }
        }
    }
    
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void startPlaybackCapture() {
        if (mediaProjection == null) {
            Log.e(TAG, "MediaProjection is null, cannot start playback capture.");
            return;
        }
        
        try {
            // Create AudioPlaybackCaptureConfiguration to capture system audio
            // Note: AudioPlaybackCaptureConfiguration requires API 29+, so we'll use regular AudioRecord for API 26
            AudioPlaybackCaptureConfiguration config = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    // For API 29+, we can use AudioPlaybackCaptureConfiguration
                    config = new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                            .addMatchingUsage(2) // USAGE_VOICE_COMMUNICATION
                            .addMatchingUsage(1) // USAGE_MEDIA
                            .build();
                } catch (Exception e) {
                    Log.w(TAG, "Could not create AudioPlaybackCaptureConfiguration: " + e.getMessage());
                    config = null;
                }
            }
            
            // Create AudioRecord with playback capture configuration
            AudioFormat audioFormat = new AudioFormat.Builder()
                    .setEncoding(AUDIO_FORMAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_CONFIG)
                    .build();
            
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            
            AudioRecord.Builder builder = new AudioRecord.Builder()
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(bufferSize * 2);
            
            if (config != null) {
                builder.setAudioPlaybackCaptureConfig(config);
            } else {
                // For API 26, use regular microphone recording
                builder.setAudioSource(MediaRecorder.AudioSource.MIC);
            }
            
            audioRecord = builder.build();
            
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.startRecording();
                startAudioProcessingThread();
                Log.d(TAG, "Playback capture started successfully.");
            } else {
                Log.e(TAG, "AudioRecord initialization failed for playback capture.");
                releaseAudioRecord();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start playback capture", e);
            releaseAudioRecord();
        }
    }
    
    private void startMicrophoneCapture() {
        try {
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize * 2
            );
            
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.startRecording();
                startAudioProcessingThread();
                Log.d(TAG, "Microphone capture started successfully.");
            } else {
                Log.e(TAG, "Microphone AudioRecord initialization failed.");
                releaseAudioRecord();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start microphone capture", e);
            releaseAudioRecord();
        }
    }
    
    private void startAudioProcessingThread() {
        Thread processingThread = new Thread(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            
            byte[] buffer = new byte[1024]; // Small buffer for low latency
            
            while (isCapturing && audioRecord != null && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                try {
                    int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                    
                    if (bytesRead > 0 && voiceProcessor != null) {
                        voiceProcessor.processAudioChunk(buffer, bytesRead);
                    } else if (bytesRead == AudioRecord.ERROR_BAD_VALUE || bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                        Log.e(TAG, "AudioRecord read error: " + bytesRead);
                        break; // Exit loop on critical error
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error in audio processing thread", e);
                    break;
                }
            }
            
            Log.d(TAG, "Audio processing thread ended.");
            // If thread ends unexpectedly, try to restart capture or notify user
            if (isCapturing) {
                Log.w(TAG, "Audio processing thread stopped unexpectedly. Attempting to restart capture.");
                stopSystemWideCapture(); // Clean up current state
                // Consider a delayed restart or user notification here
            }
        });
        
        processingThread.setName("SystemAudioProcessor");
        processingThread.start();
    }
    
    public void stopSystemWideCapture() {
        isCapturing = false;
        releaseAudioRecord();
        
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
            Log.d(TAG, "MediaProjection stopped.");
        }
        
        if (voiceProcessor != null) {
            voiceProcessor.stopProcessing();
        }
        
        Log.d(TAG, "System-wide audio capture stopped.");
    }
    
    private void releaseAudioRecord() {
        if (audioRecord != null) {
            try {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
                audioRecord.release();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error stopping or releasing AudioRecord: " + e.getMessage());
            }
            audioRecord = null;
        }
    }
    
    private void ensureVoiceProcessingActive() {
        if (voiceProcessor != null && !voiceProcessor.isProcessing()) {
            voiceProcessor.startProcessing();
        }
    }
    
    public void setMediaProjection(MediaProjection projection) {
        this.mediaProjection = projection;
        if (isCapturing) {
            // If already capturing, restart with new MediaProjection
            stopSystemWideCapture();
            startSystemWideCapture();
        }
    }
    
    public boolean isCapturing() {
        return isCapturing;
    }
    
    // Method to request MediaProjection permission from the user
    public Intent createMediaProjectionIntent() {
        if (mediaProjectionManager != null) {
            return mediaProjectionManager.createScreenCaptureIntent();
        }
        return null;
    }
    
    // Static method to get the SystemWideAudioService instance
    private static SystemWideAudioService instance;
    
    public static SystemWideAudioService getInstance() {
        return instance;
    }
    

    @Override
    public boolean onUnbind(Intent intent) {
        instance = null;
        Log.d(TAG, "SystemWideAudioService unbound, instance cleared.");
        return super.onUnbind(intent);
    }
}

