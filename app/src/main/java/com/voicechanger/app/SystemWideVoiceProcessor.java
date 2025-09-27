package com.voicechanger.app;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * System-wide voice processor that uses free AI models for voice conversion
 * and routes the processed audio back to the system.
 * Enhanced with advanced AI integration, robust error handling, and dynamic processing modes.
 */
public class SystemWideVoiceProcessor {
    private static final String TAG = "SystemWideVoiceProcessor";
    
    // Audio configuration
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    
    // Free AI service endpoints (placeholders - actual URLs would be dynamic or configured)
    private static final String FAKEYOU_API_BASE = "https://api.fakeyou.com";
    private static final String UBERDUCK_API_BASE = "https://api.uberduck.ai";
    private static final String KITS_AI_API_BASE = "https://api.kits.ai";
    
    private Context context;
    private AudioTrack audioTrack;
    private OkHttpClient httpClient;
    private ExecutorService executorService;
    
    // Processing queues
    private final BlockingQueue<AudioChunk> inputQueue = new LinkedBlockingQueue<>(100); // Limit queue size
    private final BlockingQueue<byte[]> outputQueue = new LinkedBlockingQueue<>(100);
    
    // Processing state
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private Thread processingThread;
    private Thread playbackThread;
    
    // Voice configuration
    private String selectedVoiceModel = "saudi_girl_warm"; // Default voice profile
    private VoiceProcessingMode processingMode = VoiceProcessingMode.REAL_TIME;
    private float pitchShiftFactor = 1.2f; // Default for 'saudi_girl_warm'
    private float formantShiftFactor = 1.1f;
    
    // Performance tracking
    private long totalProcessedChunks = 0;
    private long totalLatency = 0;
    private long failedApiRequests = 0;
    private long successfulApiRequests = 0;
    
    // Listener for UI updates
    private VoiceProcessorListener listener;
    
    public interface VoiceProcessorListener {
        void onPerformanceUpdate(long avgLatency, float successRate, long totalChunks);
        void onError(String message);
    }
    
    public enum VoiceProcessingMode {
        REAL_TIME,      // Low latency, local DSP + lightweight AI
        HIGH_QUALITY,   // Higher latency, external AI APIs
        HYBRID,         // Auto-switch based on conditions
        OFFLINE         // Process and cache (not implemented in real-time path)
    }
    
    private static class AudioChunk {
        byte[] data;
        int length;
        long timestamp;
        
        AudioChunk(byte[] data, int length) {
            this.data = new byte[length];
            System.arraycopy(data, 0, this.data, 0, length);
            this.length = length;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public SystemWideVoiceProcessor(Context context) {
        this.context = context;
        this.executorService = Executors.newCachedThreadPool();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        
        initializeAudioOutput();
        Log.d(TAG, "SystemWideVoiceProcessor initialized");
    }
    
    public void setListener(VoiceProcessorListener listener) {
        this.listener = listener;
    }
    
    private void initializeAudioOutput() {
        try {
            audioTrack = new AudioTrack(
                    AudioManager.STREAM_VOICE_CALL, // Use voice call stream for system-wide effect
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    BUFFER_SIZE * 2,
                    AudioTrack.MODE_STREAM
            );
            
            if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                Log.d(TAG, "Audio output initialized successfully");
            } else {
                Log.e(TAG, "Failed to initialize audio output");
                if (listener != null) listener.onError("Failed to initialize audio output.");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing audio output", e);
            if (listener != null) listener.onError("Error initializing audio output: " + e.getMessage());
        }
    }
    
    public void startProcessing() {
        if (isProcessing.get()) {
            Log.w(TAG, "Processing already started");
            return;
        }
        
        isProcessing.set(true);
        
        // Clear queues and reset stats
        inputQueue.clear();
        outputQueue.clear();
        totalProcessedChunks = 0;
        totalLatency = 0;
        failedApiRequests = 0;
        successfulApiRequests = 0;
        
        // Start processing threads
        startProcessingThread();
        startPlaybackThread();
        
        // Start audio playback
        if (audioTrack != null && audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            audioTrack.play();
        }
        
        Log.d(TAG, "Voice processing started");
    }
    
    public void stopProcessing() {
        if (!isProcessing.get()) {
            return;
        }
        
        isProcessing.set(false);
        
        // Stop audio playback
        if (audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack.stop();
        }
        
        // Interrupt processing threads
        if (processingThread != null) {
            processingThread.interrupt();
            processingThread = null;
        }
        
        if (playbackThread != null) {
            playbackThread.interrupt();
            playbackThread = null;
        }
        
        // Clear queues
        inputQueue.clear();
        outputQueue.clear();
        
        Log.d(TAG, "Voice processing stopped");
    }
    
    public void processAudioChunk(byte[] audioData, int length) {
        if (!isProcessing.get() || audioData == null || length <= 0) {
            return;
        }
        
        // Add to input queue for processing
        AudioChunk chunk = new AudioChunk(audioData, length);
        if (!inputQueue.offer(chunk)) {
            Log.w(TAG, "Input queue full, dropping audio chunk. Latency too high?");
            // Consider playing original audio or silence here to maintain real-time flow
            if (outputQueue.remainingCapacity() > 0) {
                outputQueue.offer(audioData); // Fallback to original audio
            }
        }
    }
    
    private void startProcessingThread() {
        processingThread = new Thread(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            
            ByteArrayOutputStream chunkAccumulator = new ByteArrayOutputStream();
            final int targetChunkSize = SAMPLE_RATE / 2; // 500ms chunks for balance
            
            while (isProcessing.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    AudioChunk chunk = inputQueue.take();
                    chunkAccumulator.write(chunk.data, 0, chunk.length);
                    
                    // When we have enough data, process it
                    if (chunkAccumulator.size() >= targetChunkSize * 2) { // 16-bit = 2 bytes per sample
                        byte[] dataToProcess = chunkAccumulator.toByteArray();
                        chunkAccumulator.reset();
                        
                        // Process the audio chunk based on mode
                        processAudioBasedOnMode(dataToProcess, chunk.timestamp);
                    }
                    
                } catch (InterruptedException e) {
                    Log.d(TAG, "Processing thread interrupted");
                    Thread.currentThread().interrupt(); // Restore interrupt status
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error in processing thread", e);
                    if (listener != null) listener.onError("Processing error: " + e.getMessage());
                }
            }
            
            Log.d(TAG, "Processing thread ended");
        });
        
        processingThread.setName("VoiceProcessor");
        processingThread.start();
    }
    
    private void startPlaybackThread() {
        playbackThread = new Thread(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            
            while (isProcessing.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    byte[] processedAudio = outputQueue.take();
                    
                    if (audioTrack != null && processedAudio.length > 0) {
                        int bytesWritten = audioTrack.write(processedAudio, 0, processedAudio.length);
                        
                        if (bytesWritten < 0) {
                            Log.e(TAG, "AudioTrack write error: " + bytesWritten);
                            if (listener != null) listener.onError("Audio playback error: " + bytesWritten);
                        }
                    }
                    
                } catch (InterruptedException e) {
                    Log.d(TAG, "Playback thread interrupted");
                    Thread.currentThread().interrupt(); // Restore interrupt status
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error in playback thread", e);
                    if (listener != null) listener.onError("Playback error: " + e.getMessage());
                }
            }
            
            Log.d(TAG, "Playback thread ended");
        });
        
        playbackThread.setName("VoicePlayback");
        playbackThread.start();
    }
    
    private void processAudioBasedOnMode(byte[] audioData, long timestamp) {
        switch (processingMode) {
            case REAL_TIME:
                processWithLocalModel(audioData, timestamp);
                break;
            case HIGH_QUALITY:
                processWithFreeAPI(audioData, timestamp);
                break;
            case HYBRID:
                // Implement logic to decide between local and API based on network/latency
                if (shouldUseApiForHybrid()) {
                    processWithFreeAPI(audioData, timestamp);
                } else {
                    processWithLocalModel(audioData, timestamp);
                }
                break;
            case OFFLINE:
                // Offline processing would typically involve saving and then processing
                processWithLocalModel(audioData, timestamp); // Fallback for real-time path
                break;
        }
    }
    
    private boolean shouldUseApiForHybrid() {
        // Example logic: use API if network is good and average latency is acceptable
        // This would require more sophisticated network monitoring and latency tracking
        return true; // Placeholder for now
    }
    
    private void processWithLocalModel(byte[] audioData, long timestamp) {
        executorService.execute(() -> {
            try {
                // Apply advanced local voice transformation (pitch, formant, warmth)
                byte[] processedAudio = applyAdvancedVoiceTransformation(audioData);
                
                // Queue for playback
                if (!outputQueue.offer(processedAudio)) {
                    Log.w(TAG, "Output queue full, dropping processed audio from local model.");
                }
                
                // Update performance metrics
                long latency = System.currentTimeMillis() - timestamp;
                updatePerformanceMetrics(latency, true);
                
            } catch (Exception e) {
                Log.e(TAG, "Error in local processing", e);
                if (listener != null) listener.onError("Local processing failed: " + e.getMessage());
                // Fallback: play original audio
                if (!outputQueue.offer(audioData)) {
                    Log.w(TAG, "Failed to queue fallback original audio after local processing error.");
                }
                updatePerformanceMetrics(0, false); // Mark as failed processing
            }
        });
    }
    
    private void processWithFreeAPI(byte[] audioData, long timestamp) {
        executorService.execute(() -> {
            try {
                // Convert to WAV format for API
                byte[] wavData = AudioProcessor.convertPcmToWav(audioData);
                
                // Select API dynamically (e.g., round-robin, based on last success)
                String apiBaseUrl = selectApiEndpoint();
                if (apiBaseUrl == null) {
                    Log.e(TAG, "No suitable API endpoint available. Falling back to local processing.");
                    processWithLocalModel(audioData, timestamp);
                    return;
                }
                
                RequestBody requestBody = RequestBody.create(wavData, MediaType.parse("audio/wav"));
                
                Request request = new Request.Builder()
                        .url(apiBaseUrl + "/v1/voice-conversion") // Placeholder URL
                        .addHeader("Content-Type", "audio/wav")
                        .post(requestBody)
                        .build();
                
                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "API call failed for " + apiBaseUrl + ": " + e.getMessage());
                        if (listener != null) listener.onError("API processing failed: " + e.getMessage());
                        // Fallback to local processing
                        processWithLocalModel(audioData, timestamp);
                        updatePerformanceMetrics(0, false); // Mark as failed API request
                    }
                    
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try {
                            if (response.isSuccessful() && response.body() != null) {
                                byte[] transformedAudio = response.body().bytes();
                                
                                // TODO: Implement MP3/other format decoding if API returns non-PCM
                                // For now, assume API returns PCM or compatible format
                                
                                if (!outputQueue.offer(transformedAudio)) {
                                    Log.w(TAG, "Output queue full, dropping API processed audio.");
                                }
                                
                                long latency = System.currentTimeMillis() - timestamp;
                                updatePerformanceMetrics(latency, true);
                                
                            } else {
                                Log.e(TAG, "API response error from " + apiBaseUrl + ": " + response.code() + " " + response.message());
                                if (listener != null) listener.onError("API response error: " + response.message());
                                // Fallback to local processing
                                processWithLocalModel(audioData, timestamp);
                                updatePerformanceMetrics(0, false); // Mark as failed API request
                            }
                        } finally {
                            response.close();
                        }
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error preparing API request", e);
                if (listener != null) listener.onError("API request preparation failed: " + e.getMessage());
                // Fallback to local processing
                processWithLocalModel(audioData, timestamp);
                updatePerformanceMetrics(0, false); // Mark as failed API request
            }
        });
    }
    
    private String selectApiEndpoint() {
        // Implement a more sophisticated API selection logic (e.g., round-robin, latency-based)
        List<String> availableApis = new ArrayList<>();
        availableApis.add(FAKEYOU_API_BASE);
        // availableApis.add(UBERDUCK_API_BASE); // Add other APIs as they are integrated
        // availableApis.add(KITS_AI_API_BASE);
        
        if (availableApis.isEmpty()) {
            return null;
        }
        
        // Simple round-robin for now
        int index = (int) (totalProcessedChunks % availableApis.size());
        return availableApis.get(index);
    }
    
    private byte[] applyAdvancedVoiceTransformation(byte[] audioData) {
        // This is where advanced local DSP and lightweight AI models would be integrated.
        // For a '20-year-old Saudi girl with warm voice', we need:
        // 1. Pitch shifting (upwards for female, youthful)
        // 2. Formant shifting (adjust vocal tract for female characteristics)
        // 3. Harmonic enhancement/saturation (for 'warmth')
        // 4. Slight speed adjustment (youthful speech can be faster)
        
        // Placeholder for a more complex DSP chain or on-device AI model.
        // For now, enhancing the previous simple pitch shift.
        
        short[] samples = new short[audioData.length / 2];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = (short) ((audioData[i * 2 + 1] << 8) | (audioData[i * 2] & 0xFF));
        }
        
        // Apply transformations:
        for (int i = 0; i < samples.length; i++) {
            float sample = samples[i];
            
            // 1. Pitch shift up (e.g., 1.2x for a younger female voice)
            // This simple multiplication is a crude pitch shift. A real implementation
            // would use PSOLA (Phase Vocoder) or similar for better quality.
            sample *= pitchShiftFactor; 
            
            // 2. Formant shift (adjust vocal tract resonance - simulated here crudely)
            // This is a very basic simulation. Proper formant shifting requires spectral analysis.
            // For 'warmth', we might slightly boost lower frequencies or apply a gentle saturation.
            sample = (float) (sample * (1.0 + 0.05 * Math.sin(2 * Math.PI * i / (SAMPLE_RATE / 100.0)))); // Subtle formant-like modulation
            
            // 3. Harmonic enhancement / Saturation for 'warmth'
            // Apply a soft clipping or tanh-like function for gentle saturation
            sample = (float) (32767.0 * Math.tanh(sample / 32767.0 * 0.8)); // Gentle saturation
            
            // Clamp to prevent clipping
            samples[i] = (short) Math.max(-32767, Math.min(32767, sample));
        }
        
        // Convert back to bytes
        byte[] result = new byte[audioData.length];
        for (int i = 0; i < samples.length; i++) {
            result[i * 2] = (byte) (samples[i] & 0xFF);
            result[i * 2 + 1] = (byte) ((samples[i] >> 8) & 0xFF);
        }
        
        return result;
    }
    
    private void updatePerformanceMetrics(long latency, boolean success) {
        totalProcessedChunks++;
        if (success) {
            totalLatency += latency;
            successfulApiRequests++;
        } else {
            failedApiRequests++;
        }
        
        if (totalProcessedChunks % 10 == 0 && listener != null) {
            long avgLatency = successfulApiRequests > 0 ? totalLatency / successfulApiRequests : 0;
            float successRate = (float) successfulApiRequests / totalProcessedChunks * 100;
            listener.onPerformanceUpdate(avgLatency, successRate, totalProcessedChunks);
        }
    }
    
    public void setVoiceModel(String voiceModel) {
        this.selectedVoiceModel = voiceModel;
        // Adjust pitchShiftFactor and formantShiftFactor based on the selected voice model
        // For 'saudi_girl_warm':
        if ("saudi_girl_warm".equals(voiceModel)) {
            this.pitchShiftFactor = 1.2f; // Example value
            this.formantShiftFactor = 1.1f; // Example value
        } else if ("deep_male".equals(voiceModel)) {
            this.pitchShiftFactor = 0.8f;
            this.formantShiftFactor = 0.9f;
        } // Add more voice models here
    }
    
    public void setProcessingMode(VoiceProcessingMode mode) {
        this.processingMode = mode;
    }
    
    public boolean isProcessing() {
        return isProcessing.get();
    }
    
    public long getAverageLatency() {
        return successfulApiRequests > 0 ? totalLatency / successfulApiRequests : 0;
    }
    
    public float getSuccessRate() {
        return totalProcessedChunks > 0 ? (float) successfulApiRequests / totalProcessedChunks * 100 : 0;
    }
    
    public void release() {
        stopProcessing();
        
        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;
        }
        
        if (executorService != null) {
            executorService.shutdown();
        }
        
        Log.d(TAG, "SystemWideVoiceProcessor released");
    }
}

