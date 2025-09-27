package com.voicechanger.app;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Real-time voice changer that integrates AudioProcessor with ElevenLabs API
 * for continuous voice transformation with optimized latency.
 */
public class RealTimeVoiceChanger implements AudioProcessor.AudioProcessorListener {
    private static final String TAG = "RealTimeVoiceChanger";
    
    // ElevenLabs API configuration
    private static final String ELEVENLABS_API_BASE = "https://api.elevenlabs.io/v1";
    private static final MediaType AUDIO_MEDIA_TYPE = MediaType.parse("audio/wav");
    
    private Context context;
    private AudioProcessor audioProcessor;
    private OkHttpClient httpClient;
    private ExecutorService executorService;
    
    // Configuration
    private String apiKey;
    private String selectedVoiceId;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    
    // Listeners
    private RealTimeVoiceChangerListener listener;
    
    // Performance tracking
    private long lastProcessingTime = 0;
    private int processedChunks = 0;
    private int failedChunks = 0;
    
    public interface RealTimeVoiceChangerListener {
        void onVoiceChangingStarted();
        void onVoiceChangingStopped();
        void onError(String error);
        void onAudioLevelChanged(float level);
        void onPerformanceUpdate(long avgLatency, float successRate);
    }
    
    public RealTimeVoiceChanger(Context context) {
        this.context = context;
        this.audioProcessor = new AudioProcessor();
        this.executorService = Executors.newCachedThreadPool();
        
        // Configure HTTP client for optimal performance
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        
        audioProcessor.setListener(this);
        
        Log.d(TAG, "RealTimeVoiceChanger initialized");
    }
    
    public void setListener(RealTimeVoiceChangerListener listener) {
        this.listener = listener;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public void setSelectedVoiceId(String voiceId) {
        this.selectedVoiceId = voiceId;
    }
    
    public void startRealTimeVoiceChanging() {
        if (isProcessing.get()) {
            Log.w(TAG, "Voice changing already active");
            return;
        }
        
        if (apiKey == null || apiKey.isEmpty()) {
            if (listener != null) {
                listener.onError("API key not set");
            }
            return;
        }
        
        if (selectedVoiceId == null || selectedVoiceId.isEmpty()) {
            if (listener != null) {
                listener.onError("Voice not selected");
            }
            return;
        }
        
        isProcessing.set(true);
        processedChunks = 0;
        failedChunks = 0;
        
        // Start audio processing
        audioProcessor.startProcessing();
        
        if (listener != null) {
            listener.onVoiceChangingStarted();
        }
        
        Log.d(TAG, "Real-time voice changing started");
    }
    
    public void stopRealTimeVoiceChanging() {
        if (!isProcessing.get()) {
            return;
        }
        
        isProcessing.set(false);
        
        // Stop audio processing
        audioProcessor.stopProcessing();
        
        if (listener != null) {
            listener.onVoiceChangingStopped();
        }
        
        // Report final performance statistics
        if (listener != null && processedChunks > 0) {
            float successRate = (float) (processedChunks - failedChunks) / processedChunks * 100;
            listener.onPerformanceUpdate(lastProcessingTime, successRate);
        }
        
        Log.d(TAG, "Real-time voice changing stopped");
        Log.d(TAG, String.format("Performance: %d processed, %d failed", processedChunks, failedChunks));
    }
    
    // AudioProcessor.AudioProcessorListener implementation
    @Override
    public void onAudioCaptured(byte[] audioData) {
        if (!isProcessing.get() || audioData == null || audioData.length == 0) {
            return;
        }
        
        // Process audio chunk asynchronously to maintain real-time performance
        executorService.execute(() -> processAudioChunk(audioData));
    }
    
    @Override
    public void onAudioLevelChanged(float level) {
        if (listener != null) {
            listener.onAudioLevelChanged(level);
        }
    }
    
    @Override
    public void onError(String error) {
        Log.e(TAG, "Audio processor error: " + error);
        if (listener != null) {
            listener.onError("Audio processing error: " + error);
        }
    }
    
    private void processAudioChunk(byte[] audioData) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Convert PCM to WAV format for ElevenLabs API
            byte[] wavData = AudioProcessor.convertPcmToWav(audioData);
            
            // Create request to ElevenLabs Speech-to-Speech API
            RequestBody requestBody = RequestBody.create(wavData, AUDIO_MEDIA_TYPE);
            
            Request request = new Request.Builder()
                    .url(ELEVENLABS_API_BASE + "/speech-to-speech/" + selectedVoiceId)
                    .addHeader("Accept", "audio/mpeg")
                    .addHeader("xi-api-key", apiKey)
                    .addHeader("Content-Type", "audio/wav")
                    .post(requestBody)
                    .build();
            
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    failedChunks++;
                    Log.e(TAG, "API call failed for audio chunk", e);
                    
                    // For real-time processing, we might want to play original audio
                    // instead of silence when API fails
                    audioProcessor.queueProcessedAudio(audioData);
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    long processingTime = System.currentTimeMillis() - startTime;
                    lastProcessingTime = processingTime;
                    processedChunks++;
                    
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            byte[] transformedAudio = response.body().bytes();
                            
                            // Convert MP3 to PCM for playback (simplified - in practice would need proper decoding)
                            // For now, we'll log the success and queue a placeholder
                            Log.d(TAG, String.format("Received transformed audio: %d bytes, latency: %dms", 
                                    transformedAudio.length, processingTime));
                            
                            // TODO: Implement proper MP3 to PCM conversion
                            // For demonstration, we'll play the original audio
                            audioProcessor.queueProcessedAudio(audioData);
                            
                        } else {
                            failedChunks++;
                            Log.e(TAG, "API response error: " + response.code() + " " + response.message());
                            
                            // Play original audio on API error
                            audioProcessor.queueProcessedAudio(audioData);
                        }
                    } finally {
                        response.close();
                    }
                    
                    // Report performance periodically
                    if (processedChunks % 10 == 0 && listener != null) {
                        float successRate = (float) (processedChunks - failedChunks) / processedChunks * 100;
                        listener.onPerformanceUpdate(processingTime, successRate);
                    }
                }
            });
            
        } catch (Exception e) {
            failedChunks++;
            Log.e(TAG, "Error processing audio chunk", e);
            
            // Play original audio on processing error
            audioProcessor.queueProcessedAudio(audioData);
        }
    }
    
    /**
     * Get current performance statistics
     */
    public PerformanceStats getPerformanceStats() {
        float successRate = processedChunks > 0 ? 
                (float) (processedChunks - failedChunks) / processedChunks * 100 : 0;
        
        return new PerformanceStats(
                processedChunks,
                failedChunks,
                lastProcessingTime,
                successRate
        );
    }
    
    public static class PerformanceStats {
        public final int processedChunks;
        public final int failedChunks;
        public final long lastLatency;
        public final float successRate;
        
        public PerformanceStats(int processed, int failed, long latency, float success) {
            this.processedChunks = processed;
            this.failedChunks = failed;
            this.lastLatency = latency;
            this.successRate = success;
        }
        
        @Override
        public String toString() {
            return String.format("Processed: %d, Failed: %d, Latency: %dms, Success: %.1f%%",
                    processedChunks, failedChunks, lastLatency, successRate);
        }
    }
    
    public boolean isProcessing() {
        return isProcessing.get();
    }
    
    public void release() {
        stopRealTimeVoiceChanging();
        
        if (audioProcessor != null) {
            audioProcessor.release();
        }
        
        if (executorService != null) {
            executorService.shutdown();
        }
        
        Log.d(TAG, "RealTimeVoiceChanger released");
    }
}

