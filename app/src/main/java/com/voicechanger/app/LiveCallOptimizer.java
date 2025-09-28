package com.voicechanger.app;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Live Call Optimizer - Ultra-low latency voice processing for live calls
 * Optimized specifically for real-time communication with minimal delay
 */
public class LiveCallOptimizer {
    private static final String TAG = "LiveCallOptimizer";
    
    // Ultra-optimized configuration for live calls
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO;
    private static final int CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    
    // Minimal buffer sizes for lowest latency
    private static final int CAPTURE_BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT);
    private static final int PLAYBACK_BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT);
    
    // Ultra-small chunks for minimal latency (15.625ms)
    private static final int CHUNK_SIZE = SAMPLE_RATE / 64; // 15.625ms chunks
    
    private Context context;
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private ExecutorService executorService;
    
    // Optimized queues with minimal capacity
    private final BlockingQueue<byte[]> inputQueue = new LinkedBlockingQueue<>(5);
    private final BlockingQueue<byte[]> outputQueue = new LinkedBlockingQueue<>(5);
    
    // Processing state
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private Thread captureThread;
    private Thread processingThread;
    private Thread playbackThread;
    
    // Performance tracking
    private final AtomicLong totalProcessedChunks = new AtomicLong(0);
    private final AtomicLong totalLatency = new AtomicLong(0);
    private final AtomicLong maxLatency = new AtomicLong(0);
    
    // Voice cloning engine
    private VoiceCloningEngine voiceCloningEngine;
    
    // Optimization settings
    private boolean enableAdaptiveBuffering = true;
    private boolean enableNoiseReduction = true;
    private boolean enableEchoCancellation = true;
    private boolean enableAutomaticGainControl = true;
    
    public interface LiveCallListener {
        void onLatencyUpdate(long currentLatency, long maxLatency);
        void onAudioLevelChanged(float inputLevel, float outputLevel);
        void onError(String message);
    }
    
    private LiveCallListener listener;
    
    public LiveCallOptimizer(Context context) {
        this.context = context;
        this.executorService = Executors.newFixedThreadPool(4);
        this.voiceCloningEngine = new VoiceCloningEngine(context);
        
        initializeAudioComponents();
        Log.d(TAG, "LiveCallOptimizer initialized for ultra-low latency");
    }
    
    public void setListener(LiveCallListener listener) {
        this.listener = listener;
    }
    
    private void initializeAudioComponents() {
        try {
            // Initialize AudioRecord with minimal latency settings
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION, // Optimized for calls
                    SAMPLE_RATE,
                    CHANNEL_CONFIG_IN,
                    AUDIO_FORMAT,
                    CAPTURE_BUFFER_SIZE
            );
            
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new RuntimeException("AudioRecord initialization failed");
            }
            
            // Initialize AudioTrack with minimal latency settings
            audioTrack = new AudioTrack(
                    AudioManager.STREAM_VOICE_CALL, // Use voice call stream
                    SAMPLE_RATE,
                    CHANNEL_CONFIG_OUT,
                    AUDIO_FORMAT,
                    PLAYBACK_BUFFER_SIZE,
                    AudioTrack.MODE_STREAM
            );
            
            if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
                throw new RuntimeException("AudioTrack initialization failed");
            }
            
            Log.d(TAG, "Audio components initialized for live calls");
            Log.d(TAG, "Capture buffer: " + CAPTURE_BUFFER_SIZE + ", Playback buffer: " + PLAYBACK_BUFFER_SIZE);
            Log.d(TAG, "Chunk size: " + CHUNK_SIZE + " samples (" + (CHUNK_SIZE * 1000.0 / SAMPLE_RATE) + "ms)");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize audio components", e);
            if (listener != null) {
                listener.onError("Failed to initialize audio: " + e.getMessage());
            }
        }
    }
    
    public void startLiveCallProcessing() {
        if (isProcessing.get()) {
            Log.w(TAG, "Live call processing already started");
            return;
        }
        
        if (audioRecord == null || audioTrack == null) {
            Log.e(TAG, "Audio components not initialized");
            if (listener != null) {
                listener.onError("Audio components not initialized");
            }
            return;
        }
        
        isProcessing.set(true);
        
        // Clear queues and reset stats
        inputQueue.clear();
        outputQueue.clear();
        totalProcessedChunks.set(0);
        totalLatency.set(0);
        maxLatency.set(0);
        
        // Start audio capture and playback
        try {
            audioRecord.startRecording();
            audioTrack.play();
            
            // Start voice cloning engine
            voiceCloningEngine.startProcessing();
            
            // Start processing threads
            startCaptureThread();
            startProcessingThread();
            startPlaybackThread();
            
            Log.d(TAG, "Live call processing started with ultra-low latency");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start live call processing", e);
            if (listener != null) {
                listener.onError("Failed to start live call processing: " + e.getMessage());
            }
            stopLiveCallProcessing();
        }
    }
    
    public void stopLiveCallProcessing() {
        if (!isProcessing.get()) {
            return;
        }
        
        isProcessing.set(false);
        
        // Stop voice cloning engine
        voiceCloningEngine.stopProcessing();
        
        // Stop audio recording and playback
        try {
            if (audioRecord != null && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.stop();
            }
            
            if (audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack.stop();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping audio components", e);
        }
        
        // Wait for threads to finish
        try {
            if (captureThread != null) {
                captureThread.interrupt();
                captureThread.join(500);
            }
            
            if (processingThread != null) {
                processingThread.interrupt();
                processingThread.join(500);
            }
            
            if (playbackThread != null) {
                playbackThread.interrupt();
                playbackThread.join(500);
            }
        } catch (InterruptedException e) {
            Log.w(TAG, "Thread interruption during shutdown", e);
        }
        
        // Clear queues
        inputQueue.clear();
        outputQueue.clear();
        
        Log.d(TAG, "Live call processing stopped");
    }
    
    private void startCaptureThread() {
        captureThread = new Thread(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            
            byte[] buffer = new byte[CHUNK_SIZE * 2]; // 16-bit samples = 2 bytes per sample
            
            while (isProcessing.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                    
                    if (bytesRead > 0) {
                        // Apply real-time optimizations
                        byte[] optimizedAudio = applyRealTimeOptimizations(buffer, bytesRead);
                        
                        // Add to input queue
                        if (!inputQueue.offer(optimizedAudio)) {
                            Log.w(TAG, "Input queue full, dropping audio chunk");
                        }
                        
                        // Calculate and report audio level
                        float audioLevel = calculateAudioLevel(buffer, bytesRead);
                        if (listener != null) {
                            listener.onAudioLevelChanged(audioLevel, 0.0f); // Output level will be updated in playback thread
                        }
                        
                    } else if (bytesRead < 0) {
                        Log.e(TAG, "AudioRecord read error: " + bytesRead);
                        break;
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error in capture thread", e);
                    if (listener != null) {
                        listener.onError("Audio capture error: " + e.getMessage());
                    }
                    break;
                }
            }
            
            Log.d(TAG, "Capture thread ended");
        });
        
        captureThread.setName("LiveCallCapture");
        captureThread.start();
    }
    
    private void startProcessingThread() {
        processingThread = new Thread(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            
            while (isProcessing.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    byte[] audioData = inputQueue.take();
                    long startTime = System.nanoTime();
                    
                    // Process audio through voice cloning engine
                    voiceCloningEngine.processAudioChunk(audioData, audioData.length);
                    
                    // For now, we'll use the processed audio from voice cloning engine
                    // In a real implementation, we'd get the processed audio back
                    byte[] processedAudio = audioData; // Placeholder
                    
                    // Add to output queue
                    if (!outputQueue.offer(processedAudio)) {
                        Log.w(TAG, "Output queue full, dropping processed audio");
                    }
                    
                    // Update latency metrics
                    long latency = (System.nanoTime() - startTime) / 1_000_000; // Convert to ms
                    updateLatencyMetrics(latency);
                    
                } catch (InterruptedException e) {
                    Log.d(TAG, "Processing thread interrupted");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error in processing thread", e);
                    if (listener != null) {
                        listener.onError("Audio processing error: " + e.getMessage());
                    }
                    break;
                }
            }
            
            Log.d(TAG, "Processing thread ended");
        });
        
        processingThread.setName("LiveCallProcessing");
        processingThread.start();
    }
    
    private void startPlaybackThread() {
        playbackThread = new Thread(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            
            while (isProcessing.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    byte[] audioData = outputQueue.take();
                    
                    if (audioData != null && audioData.length > 0) {
                        // Play the processed audio
                        int bytesWritten = audioTrack.write(audioData, 0, audioData.length);
                        
                        if (bytesWritten < 0) {
                            Log.e(TAG, "AudioTrack write error: " + bytesWritten);
                        }
                        
                        // Calculate and report output audio level
                        float outputLevel = calculateAudioLevel(audioData, audioData.length);
                        if (listener != null) {
                            listener.onAudioLevelChanged(0.0f, outputLevel); // Input level will be updated in capture thread
                        }
                    }
                    
                } catch (InterruptedException e) {
                    Log.d(TAG, "Playback thread interrupted");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error in playback thread", e);
                    if (listener != null) {
                        listener.onError("Audio playback error: " + e.getMessage());
                    }
                    break;
                }
            }
            
            Log.d(TAG, "Playback thread ended");
        });
        
        playbackThread.setName("LiveCallPlayback");
        playbackThread.start();
    }
    
    private byte[] applyRealTimeOptimizations(byte[] audioData, int length) {
        // Apply real-time optimizations for live calls
        byte[] optimized = new byte[length];
        System.arraycopy(audioData, 0, optimized, 0, length);
        
        if (enableNoiseReduction) {
            optimized = applyNoiseReduction(optimized);
        }
        
        if (enableEchoCancellation) {
            optimized = applyEchoCancellation(optimized);
        }
        
        if (enableAutomaticGainControl) {
            optimized = applyAutomaticGainControl(optimized);
        }
        
        return optimized;
    }
    
    private byte[] applyNoiseReduction(byte[] audioData) {
        // Simple noise gate for real-time processing
        short[] samples = new short[audioData.length / 2];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = (short) ((audioData[i * 2 + 1] << 8) | (audioData[i * 2] & 0xFF));
        }
        
        // Apply noise gate
        float threshold = 0.01f;
        for (int i = 0; i < samples.length; i++) {
            float sample = Math.abs(samples[i] / 32767.0f);
            if (sample < threshold) {
                samples[i] = 0;
            }
        }
        
        // Convert back to bytes
        byte[] result = new byte[audioData.length];
        for (int i = 0; i < samples.length; i++) {
            result[i * 2] = (byte) (samples[i] & 0xFF);
            result[i * 2 + 1] = (byte) ((samples[i] >> 8) & 0xFF);
        }
        
        return result;
    }
    
    private byte[] applyEchoCancellation(byte[] audioData) {
        // Simple echo cancellation (placeholder)
        // Real implementation would use adaptive filtering
        return audioData;
    }
    
    private byte[] applyAutomaticGainControl(byte[] audioData) {
        // Simple AGC for consistent volume
        short[] samples = new short[audioData.length / 2];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = (short) ((audioData[i * 2 + 1] << 8) | (audioData[i * 2] & 0xFF));
        }
        
        // Calculate RMS
        long sum = 0;
        for (short sample : samples) {
            sum += sample * sample;
        }
        double rms = Math.sqrt((double) sum / samples.length);
        
        // Apply gain if needed
        if (rms > 0) {
            float targetRms = 10000.0f; // Target RMS level
            float gain = (float) (targetRms / rms);
            gain = Math.max(0.1f, Math.min(10.0f, gain)); // Limit gain range
            
            for (int i = 0; i < samples.length; i++) {
                samples[i] = (short) Math.max(-32767, Math.min(32767, samples[i] * gain));
            }
        }
        
        // Convert back to bytes
        byte[] result = new byte[audioData.length];
        for (int i = 0; i < samples.length; i++) {
            result[i * 2] = (byte) (samples[i] & 0xFF);
            result[i * 2 + 1] = (byte) ((samples[i] >> 8) & 0xFF);
        }
        
        return result;
    }
    
    private float calculateAudioLevel(byte[] audioData, int length) {
        if (length < 2) return -60.0f;
        
        long sum = 0;
        int sampleCount = length / 2;
        
        for (int i = 0; i < length - 1; i += 2) {
            short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
            sum += sample * sample;
        }
        
        double rms = Math.sqrt((double) sum / sampleCount);
        
        if (rms < 1.0) rms = 1.0;
        return (float) (20 * Math.log10(rms / 32767.0));
    }
    
    private void updateLatencyMetrics(long latency) {
        totalProcessedChunks.incrementAndGet();
        totalLatency.addAndGet(latency);
        
        long currentMax = maxLatency.get();
        while (latency > currentMax && !maxLatency.compareAndSet(currentMax, latency)) {
            currentMax = maxLatency.get();
        }
        
        if (listener != null) {
            long avgLatency = totalLatency.get() / totalProcessedChunks.get();
            listener.onLatencyUpdate(latency, maxLatency.get());
        }
    }
    
    // Public methods for voice management
    public void setVoiceProfile(String voiceId) {
        voiceCloningEngine.setCurrentVoice(voiceId);
    }
    
    public void cloneVoiceFromAudio(byte[] audioData, String voiceId, String name) {
        voiceCloningEngine.cloneVoiceFromAudio(audioData, voiceId, name);
    }
    
    public void setOptimizationSettings(boolean noiseReduction, boolean echoCancellation, boolean agc) {
        this.enableNoiseReduction = noiseReduction;
        this.enableEchoCancellation = echoCancellation;
        this.enableAutomaticGainControl = agc;
    }
    
    public boolean isProcessing() {
        return isProcessing.get();
    }
    
    public long getAverageLatency() {
        long chunks = totalProcessedChunks.get();
        return chunks > 0 ? totalLatency.get() / chunks : 0;
    }
    
    public long getMaxLatency() {
        return maxLatency.get();
    }
    
    public long getTotalProcessedChunks() {
        return totalProcessedChunks.get();
    }
    
    public void release() {
        stopLiveCallProcessing();
        
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
        
        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;
        }
        
        if (voiceCloningEngine != null) {
            voiceCloningEngine.release();
            voiceCloningEngine = null;
        }
        
        if (executorService != null) {
            executorService.shutdown();
        }
        
        Log.d(TAG, "LiveCallOptimizer released");
    }
}
