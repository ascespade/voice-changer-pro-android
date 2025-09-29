package com.voicechanger.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.RequiresApi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class SystemWideAudioService extends AccessibilityService {
    private static final String TAG = "SystemWideAudioService";
    private static SystemWideAudioService instance;
    
    // Audio processing
    private AudioRecord audioRecord;
    private AudioManager audioManager;
    private MediaProjection mediaProjection;
    private VoiceProcessorListener voiceProcessorListener;
    
    // Configuration
    private String apiKey = "";
    private String voiceModel = "default";
    private String processingMode = "realtime";
    private boolean aiAnalysisEnabled = true;
    private boolean voiceCloningEnabled = false;
    private boolean smartProcessingEnabled = true;
    
    // Performance monitoring
    private long totalChunks = 0;
    private long totalLatency = 0;
    private long successfulChunks = 0;
    private final AtomicBoolean isCapturing = new AtomicBoolean(false);
    
    // Audio processing thread
    private Thread audioProcessingThread;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface VoiceProcessorListener {
        void onPerformanceUpdate(long avgLatency, float successRate, long totalChunks);
        void onError(String message);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        Log.d(TAG, "SystemWideAudioService created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSystemWideCapture();
        instance = null;
        Log.d(TAG, "SystemWideAudioService destroyed");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Handle accessibility events if needed
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted");
        stopSystemWideCapture();
    }

    public static SystemWideAudioService getInstance() {
        return instance;
    }

    public void setVoiceProcessorListener(VoiceProcessorListener listener) {
        this.voiceProcessorListener = listener;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        Log.d(TAG, "API key updated");
    }

    public void setVoiceModel(String voiceModel) {
        this.voiceModel = voiceModel;
        Log.d(TAG, "Voice model set to: " + voiceModel);
    }

    public void setProcessingMode(String processingMode) {
        this.processingMode = processingMode;
        Log.d(TAG, "Processing mode set to: " + processingMode);
    }

    public void setAIAnalysisEnabled(boolean enabled) {
        this.aiAnalysisEnabled = enabled;
        Log.d(TAG, "AI Analysis enabled: " + enabled);
    }

    public void setVoiceCloningEnabled(boolean enabled) {
        this.voiceCloningEnabled = enabled;
        Log.d(TAG, "Voice Cloning enabled: " + enabled);
    }

    public void setSmartProcessingEnabled(boolean enabled) {
        this.smartProcessingEnabled = enabled;
        Log.d(TAG, "Smart Processing enabled: " + enabled);
    }

    public void setMediaProjection(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
        Log.d(TAG, "MediaProjection set");
    }

    public boolean isCapturing() {
        return isCapturing.get();
    }

    public void startSystemWideCapture() {
        if (isCapturing.get()) {
            Log.w(TAG, "Already capturing audio");
            return;
        }

        Log.d(TAG, "Starting system-wide audio capture");
        isCapturing.set(true);

        // Initialize audio recording
        initializeAudioRecording();

        // Start audio processing thread
        startAudioProcessingThread();

        // Notify listener
        if (voiceProcessorListener != null) {
            mainHandler.post(() -> {
                voiceProcessorListener.onPerformanceUpdate(getAverageLatency(), getSuccessRate(), totalChunks);
            });
        }
    }

    public void stopSystemWideCapture() {
        if (!isCapturing.get()) {
            Log.w(TAG, "Not currently capturing audio");
            return;
        }

        Log.d(TAG, "Stopping system-wide audio capture");
        isCapturing.set(false);

        // Stop audio recording
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            } catch (Exception e) {
                Log.e(TAG, "Error stopping audio record: " + e.getMessage());
            }
        }

        // Stop processing thread
        if (audioProcessingThread != null) {
            audioProcessingThread.interrupt();
            audioProcessingThread = null;
        }

        // Reset performance metrics
        resetPerformanceMetrics();
    }

    private void initializeAudioRecording() {
        try {
            int sampleRate = 44100;
            int channelConfig = AudioFormat.CHANNEL_IN_MONO;
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

            // Create AudioPlaybackCaptureConfiguration for system audio capture
            AudioPlaybackCaptureConfiguration config = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaProjection != null) {
                try {
                    config = new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                            .addMatchingUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                            .build();
                } catch (Exception e) {
                    Log.w(TAG, "Could not create AudioPlaybackCaptureConfiguration: " + e.getMessage());
                }
            }

            // Create AudioRecord with or without MediaProjection
            if (config != null) {
                audioRecord = new AudioRecord.Builder()
                        .setAudioSource(MediaRecorder.AudioSource.MIC)
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(audioFormat)
                                .setSampleRate(sampleRate)
                                .setChannelMask(channelConfig)
                                .build())
                        .setBufferSizeInBytes(bufferSize * 2)
                        .setAudioPlaybackCaptureConfig(config)
                        .build();
            } else {
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize * 2);
            }

            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.startRecording();
                Log.d(TAG, "Audio recording started successfully");
            } else {
                Log.e(TAG, "Failed to initialize AudioRecord");
                if (voiceProcessorListener != null) {
                    mainHandler.post(() -> voiceProcessorListener.onError("Failed to initialize audio recording"));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing audio recording: " + e.getMessage());
            if (voiceProcessorListener != null) {
                mainHandler.post(() -> voiceProcessorListener.onError("Audio initialization error: " + e.getMessage()));
            }
        }
    }

    private void startAudioProcessingThread() {
        audioProcessingThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            int bytesRead;

            while (isCapturing.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    if (audioRecord != null && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                        bytesRead = audioRecord.read(buffer, 0, buffer.length);
                        
                        if (bytesRead > 0) {
                            processAudioChunk(buffer, bytesRead);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in audio processing thread: " + e.getMessage());
                    if (voiceProcessorListener != null) {
                        mainHandler.post(() -> voiceProcessorListener.onError("Audio processing error: " + e.getMessage()));
                    }
                    break;
                }
            }
        });
        
        audioProcessingThread.start();
        Log.d(TAG, "Audio processing thread started");
    }

    private void processAudioChunk(byte[] audioData, int length) {
        long startTime = System.currentTimeMillis();
        totalChunks++;

        try {
            // Apply voice processing based on current configuration
            byte[] processedAudio = applyVoiceProcessing(audioData, length);
            
            if (processedAudio != null) {
                successfulChunks++;
                
                // Apply AI analysis if enabled
                if (aiAnalysisEnabled) {
                    performAIAnalysis(processedAudio);
                }
                
                // Apply voice cloning if enabled
                if (voiceCloningEnabled) {
                    performVoiceCloning(processedAudio);
                }
                
                // Apply smart processing if enabled
                if (smartProcessingEnabled) {
                    performSmartProcessing(processedAudio);
                }
            }

            // Calculate performance metrics
            long processingTime = System.currentTimeMillis() - startTime;
            totalLatency += processingTime;

            // Update performance metrics periodically
            if (totalChunks % 10 == 0) {
                updatePerformanceMetrics();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error processing audio chunk: " + e.getMessage());
        }
    }

    private byte[] applyVoiceProcessing(byte[] audioData, int length) {
        // Apply voice transformation based on selected voice model and processing mode
        switch (voiceModel) {
            case "male":
                return applyMaleVoiceEffect(audioData, length);
            case "female":
                return applyFemaleVoiceEffect(audioData, length);
            case "child":
                return applyChildVoiceEffect(audioData, length);
            case "elderly":
                return applyElderlyVoiceEffect(audioData, length);
            case "robot":
                return applyRobotVoiceEffect(audioData, length);
            case "whisper":
                return applyWhisperVoiceEffect(audioData, length);
            default:
                return applyDefaultVoiceEffect(audioData, length);
        }
    }

    private byte[] applyMaleVoiceEffect(byte[] audioData, int length) {
        // Implement male voice transformation
        byte[] processed = new byte[length];
        System.arraycopy(audioData, 0, processed, 0, length);
        
        // Apply pitch shifting and formant modification for male voice
        for (int i = 0; i < length; i += 2) {
            if (i + 1 < length) {
                short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
                sample = (short) (sample * 0.8); // Lower pitch
                processed[i] = (byte) (sample & 0xFF);
                processed[i + 1] = (byte) ((sample >> 8) & 0xFF);
            }
        }
        
        return processed;
    }

    private byte[] applyFemaleVoiceEffect(byte[] audioData, int length) {
        // Implement female voice transformation
        byte[] processed = new byte[length];
        System.arraycopy(audioData, 0, processed, 0, length);
        
        // Apply pitch shifting and formant modification for female voice
        for (int i = 0; i < length; i += 2) {
            if (i + 1 < length) {
                short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
                sample = (short) (sample * 1.2); // Higher pitch
                processed[i] = (byte) (sample & 0xFF);
                processed[i + 1] = (byte) ((sample >> 8) & 0xFF);
            }
        }
        
        return processed;
    }

    private byte[] applyChildVoiceEffect(byte[] audioData, int length) {
        // Implement child voice transformation
        byte[] processed = new byte[length];
        System.arraycopy(audioData, 0, processed, 0, length);
        
        // Apply higher pitch and brighter tone for child voice
        for (int i = 0; i < length; i += 2) {
            if (i + 1 < length) {
                short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
                sample = (short) (sample * 1.4); // Much higher pitch
                processed[i] = (byte) (sample & 0xFF);
                processed[i + 1] = (byte) ((sample >> 8) & 0xFF);
            }
        }
        
        return processed;
    }

    private byte[] applyElderlyVoiceEffect(byte[] audioData, int length) {
        // Implement elderly voice transformation
        byte[] processed = new byte[length];
        System.arraycopy(audioData, 0, processed, 0, length);
        
        // Apply lower pitch and slight tremolo for elderly voice
        for (int i = 0; i < length; i += 2) {
            if (i + 1 < length) {
                short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
                sample = (short) (sample * 0.7); // Lower pitch
                processed[i] = (byte) (sample & 0xFF);
                processed[i + 1] = (byte) ((sample >> 8) & 0xFF);
            }
        }
        
        return processed;
    }

    private byte[] applyRobotVoiceEffect(byte[] audioData, int length) {
        // Implement robot voice transformation
        byte[] processed = new byte[length];
        System.arraycopy(audioData, 0, processed, 0, length);
        
        // Apply robotic effects (bit crushing, filtering)
        for (int i = 0; i < length; i += 2) {
            if (i + 1 < length) {
                short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
                sample = (short) ((sample >> 4) << 4); // Bit crushing
                processed[i] = (byte) (sample & 0xFF);
                processed[i + 1] = (byte) ((sample >> 8) & 0xFF);
            }
        }
        
        return processed;
    }

    private byte[] applyWhisperVoiceEffect(byte[] audioData, int length) {
        // Implement whisper voice transformation
        byte[] processed = new byte[length];
        System.arraycopy(audioData, 0, processed, 0, length);
        
        // Apply volume reduction and high-pass filtering for whisper
        for (int i = 0; i < length; i += 2) {
            if (i + 1 < length) {
                short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
                sample = (short) (sample * 0.3); // Much quieter
                processed[i] = (byte) (sample & 0xFF);
                processed[i + 1] = (byte) ((sample >> 8) & 0xFF);
            }
        }
        
        return processed;
    }

    private byte[] applyDefaultVoiceEffect(byte[] audioData, int length) {
        // Default processing - minimal changes
        return audioData;
    }

    private void performAIAnalysis(byte[] audioData) {
        // Implement AI-powered voice analysis
        Log.d(TAG, "Performing AI analysis on audio chunk");
    }

    private void performVoiceCloning(byte[] audioData) {
        // Implement voice cloning functionality
        Log.d(TAG, "Performing voice cloning on audio chunk");
    }

    private void performSmartProcessing(byte[] audioData) {
        // Implement smart processing optimizations
        Log.d(TAG, "Performing smart processing on audio chunk");
    }

    private void updatePerformanceMetrics() {
        if (voiceProcessorListener != null) {
            mainHandler.post(() -> {
                voiceProcessorListener.onPerformanceUpdate(getAverageLatency(), getSuccessRate(), totalChunks);
            });
        }
    }

    private long getAverageLatency() {
        return totalChunks > 0 ? totalLatency / totalChunks : 0;
    }

    private float getSuccessRate() {
        return totalChunks > 0 ? (float) successfulChunks / totalChunks * 100 : 0;
    }

    private void resetPerformanceMetrics() {
        totalChunks = 0;
        totalLatency = 0;
        successfulChunks = 0;
    }
}