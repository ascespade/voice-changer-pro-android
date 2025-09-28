package com.voicechanger.app;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced local voice processor with real-time AI-like effects
 * No external APIs needed - all processing happens locally
 */
public class AdvancedVoiceProcessor {
    private static final String TAG = "AdvancedVoiceProcessor";
    
    // Optimized for ultra-low latency
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    
    // Ultra-small chunks for minimal latency (62.5ms)
    private static final int CHUNK_SIZE = SAMPLE_RATE / 16; // 62.5ms chunks
    
    private Context context;
    private AudioTrack audioTrack;
    private ExecutorService executorService;
    
    // Optimized queues with smaller capacity
    private final BlockingQueue<AudioChunk> inputQueue = new LinkedBlockingQueue<>(20);
    private final BlockingQueue<byte[]> outputQueue = new LinkedBlockingQueue<>(20);
    
    // Processing state
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private Thread processingThread;
    private Thread playbackThread;
    
    // Voice effects configuration
    private VoiceProfile currentProfile = VoiceProfile.SAUDI_GIRL_WARM;
    private float pitchShift = 1.2f;
    private float formantShift = 1.1f;
    private float warmth = 0.3f;
    private float clarity = 0.8f;
    
    // Performance tracking
    private final AtomicLong totalProcessedChunks = new AtomicLong(0);
    private final AtomicLong totalLatency = new AtomicLong(0);
    
    // Voice profiles with pre-calculated parameters
    public enum VoiceProfile {
        SAUDI_GIRL_WARM(1.2f, 1.1f, 0.3f, 0.8f),
        DEEP_MALE(0.8f, 0.9f, 0.2f, 0.9f),
        ROBOT(1.0f, 1.0f, 0.0f, 1.0f),
        CHILD(1.4f, 1.2f, 0.4f, 0.7f),
        ELDERLY(0.9f, 0.8f, 0.5f, 0.6f);
        
        public final float pitchShift;
        public final float formantShift;
        public final float warmth;
        public final float clarity;
        
        VoiceProfile(float pitchShift, float formantShift, float warmth, float clarity) {
            this.pitchShift = pitchShift;
            this.formantShift = formantShift;
            this.warmth = warmth;
            this.clarity = clarity;
        }
    }
    
    private static class AudioChunk {
        byte[] data;
        int length;
        long timestamp;
        
        AudioChunk(byte[] data, int length) {
            this.data = new byte[length];
            System.arraycopy(data, 0, this.data, 0, length);
            this.length = length;
            this.timestamp = System.nanoTime(); // Use nanoTime for better precision
        }
    }
    
    public interface VoiceProcessorListener {
        void onPerformanceUpdate(long avgLatency, long totalChunks);
        void onError(String message);
    }
    
    private VoiceProcessorListener listener;
    
    public AdvancedVoiceProcessor(Context context) {
        this.context = context;
        this.executorService = Executors.newFixedThreadPool(2); // Limit threads for better performance
        
        initializeAudioOutput();
        Log.d(TAG, "AdvancedVoiceProcessor initialized with ultra-low latency");
    }
    
    public void setListener(VoiceProcessorListener listener) {
        this.listener = listener;
    }
    
    private void initializeAudioOutput() {
        try {
            audioTrack = new AudioTrack(
                    AudioManager.STREAM_VOICE_CALL,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    BUFFER_SIZE,
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
        totalProcessedChunks.set(0);
        totalLatency.set(0);
        
        // Start processing threads
        startProcessingThread();
        startPlaybackThread();
        
        // Start audio playback
        if (audioTrack != null && audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            audioTrack.play();
        }
        
        Log.d(TAG, "Advanced voice processing started");
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
        
        Log.d(TAG, "Advanced voice processing stopped");
    }
    
    public void processAudioChunk(byte[] audioData, int length) {
        if (!isProcessing.get() || audioData == null || length <= 0) {
            return;
        }
        
        // Add to input queue for processing
        AudioChunk chunk = new AudioChunk(audioData, length);
        if (!inputQueue.offer(chunk)) {
            Log.w(TAG, "Input queue full, dropping audio chunk");
            // Fallback: play original audio to maintain real-time flow
            if (outputQueue.remainingCapacity() > 0) {
                outputQueue.offer(audioData);
            }
        }
    }
    
    private void startProcessingThread() {
        processingThread = new Thread(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            
            while (isProcessing.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    AudioChunk chunk = inputQueue.take();
                    
                    // Process audio with advanced effects
                    byte[] processedAudio = applyAdvancedVoiceEffects(chunk.data, chunk.length);
                    
                    // Queue for playback
                    if (!outputQueue.offer(processedAudio)) {
                        Log.w(TAG, "Output queue full, dropping processed audio");
                    }
                    
                    // Update performance metrics
                    long latency = (System.nanoTime() - chunk.timestamp) / 1_000_000; // Convert to ms
                    updatePerformanceMetrics(latency);
                    
                } catch (InterruptedException e) {
                    Log.d(TAG, "Processing thread interrupted");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error in processing thread", e);
                    if (listener != null) listener.onError("Processing error: " + e.getMessage());
                }
            }
            
            Log.d(TAG, "Processing thread ended");
        });
        
        processingThread.setName("AdvancedVoiceProcessor");
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
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error in playback thread", e);
                    if (listener != null) listener.onError("Playback error: " + e.getMessage());
                }
            }
            
            Log.d(TAG, "Playback thread ended");
        });
        
        playbackThread.setName("AdvancedVoicePlayback");
        playbackThread.start();
    }
    
    private byte[] applyAdvancedVoiceEffects(byte[] audioData, int length) {
        // Convert bytes to 16-bit samples
        short[] samples = new short[length / 2];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = (short) ((audioData[i * 2 + 1] << 8) | (audioData[i * 2] & 0xFF));
        }
        
        // Apply advanced voice effects
        samples = applyPitchShift(samples);
        samples = applyFormantShift(samples);
        samples = applyWarmthEffect(samples);
        samples = applyClarityEnhancement(samples);
        samples = applyNoiseReduction(samples);
        
        // Convert back to bytes
        byte[] result = new byte[length];
        for (int i = 0; i < samples.length; i++) {
            result[i * 2] = (byte) (samples[i] & 0xFF);
            result[i * 2 + 1] = (byte) ((samples[i] >> 8) & 0xFF);
        }
        
        return result;
    }
    
    private short[] applyPitchShift(short[] samples) {
        // Simple pitch shifting using linear interpolation
        if (pitchShift == 1.0f) return samples;
        
        short[] result = new short[samples.length];
        for (int i = 0; i < result.length; i++) {
            float sourceIndex = i / pitchShift;
            int index1 = (int) sourceIndex;
            int index2 = Math.min(index1 + 1, samples.length - 1);
            float fraction = sourceIndex - index1;
            
            if (index1 < samples.length) {
                result[i] = (short) (samples[index1] * (1 - fraction) + samples[index2] * fraction);
            }
        }
        return result;
    }
    
    private short[] applyFormantShift(short[] samples) {
        // Simulate formant shifting with frequency modulation
        if (formantShift == 1.0f) return samples;
        
        short[] result = new short[samples.length];
        for (int i = 0; i < samples.length; i++) {
            float sample = samples[i];
            
            // Apply subtle frequency modulation to simulate formant shift
            float modulation = (float) Math.sin(2 * Math.PI * i / (SAMPLE_RATE / 100.0f)) * 0.1f;
            sample *= (1.0f + modulation * (formantShift - 1.0f));
            
            result[i] = (short) Math.max(-32767, Math.min(32767, sample));
        }
        return result;
    }
    
    private short[] applyWarmthEffect(short[] samples) {
        // Apply harmonic enhancement for warmth
        if (warmth == 0.0f) return samples;
        
        short[] result = new short[samples.length];
        for (int i = 0; i < samples.length; i++) {
            float sample = samples[i] / 32767.0f;
            
            // Apply soft saturation for warmth
            sample = (float) Math.tanh(sample * (1.0f + warmth));
            
            result[i] = (short) (sample * 32767.0f);
        }
        return result;
    }
    
    private short[] applyClarityEnhancement(short[] samples) {
        // Apply high-frequency emphasis for clarity
        if (clarity == 1.0f) return samples;
        
        short[] result = new short[samples.length];
        
        // Simple high-pass filter for clarity
        float alpha = 0.95f * clarity;
        float prevSample = 0;
        
        float filtered = 0;
        for (int i = 0; i < samples.length; i++) {
            float sample = samples[i];
            filtered = alpha * (filtered + sample - prevSample);
            prevSample = sample;
            
            result[i] = (short) Math.max(-32767, Math.min(32767, filtered));
        }
        return result;
    }
    
    private short[] applyNoiseReduction(short[] samples) {
        // Simple noise gate
        short[] result = new short[samples.length];
        float threshold = 0.01f; // Adjust based on noise level
        
        for (int i = 0; i < samples.length; i++) {
            float sample = Math.abs(samples[i] / 32767.0f);
            if (sample < threshold) {
                result[i] = 0; // Gate out low-level noise
            } else {
                result[i] = samples[i];
            }
        }
        return result;
    }
    
    private void updatePerformanceMetrics(long latency) {
        totalProcessedChunks.incrementAndGet();
        totalLatency.addAndGet(latency);
        
        if (totalProcessedChunks.get() % 10 == 0 && listener != null) {
            long avgLatency = totalLatency.get() / totalProcessedChunks.get();
            listener.onPerformanceUpdate(avgLatency, totalProcessedChunks.get());
        }
    }
    
    public void setVoiceProfile(VoiceProfile profile) {
        this.currentProfile = profile;
        this.pitchShift = profile.pitchShift;
        this.formantShift = profile.formantShift;
        this.warmth = profile.warmth;
        this.clarity = profile.clarity;
        
        Log.d(TAG, "Voice profile set to: " + profile.name());
    }
    
    public boolean isProcessing() {
        return isProcessing.get();
    }
    
    public long getAverageLatency() {
        long chunks = totalProcessedChunks.get();
        return chunks > 0 ? totalLatency.get() / chunks : 0;
    }
    
    public long getTotalProcessedChunks() {
        return totalProcessedChunks.get();
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
        
        Log.d(TAG, "AdvancedVoiceProcessor released");
    }
}
