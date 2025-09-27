package com.voicechanger.app;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enhanced audio processor that handles real-time audio capture, buffering,
 * and playback with optimized latency for voice changing applications.
 */
public class AudioProcessor {
    private static final String TAG = "AudioProcessor";
    
    // Optimized audio configuration for low latency
    private static final int SAMPLE_RATE = 16000; // 16kHz for good quality and API compatibility
    private static final int CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO;
    private static final int CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    
    // Buffer sizes optimized for real-time processing
    private static final int CAPTURE_BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT) * 2;
    private static final int PLAYBACK_BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT) * 2;
    
    // Processing chunk size (smaller for lower latency)
    private static final int CHUNK_SIZE = SAMPLE_RATE / 4; // 250ms chunks for balance between latency and API efficiency
    
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    
    // Threading and synchronization
    private Thread captureThread;
    private Thread playbackThread;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    
    // Audio data queues for buffering
    private final BlockingQueue<byte[]> captureQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<byte[]> playbackQueue = new LinkedBlockingQueue<>();
    
    // Listeners
    private AudioProcessorListener listener;
    
    public interface AudioProcessorListener {
        void onAudioCaptured(byte[] audioData);
        void onAudioLevelChanged(float level);
        void onError(String error);
    }
    
    public AudioProcessor() {
        initializeAudioComponents();
    }
    
    public void setListener(AudioProcessorListener listener) {
        this.listener = listener;
    }
    
    private void initializeAudioComponents() {
        try {
            // Initialize AudioRecord with optimized settings
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG_IN,
                    AUDIO_FORMAT,
                    CAPTURE_BUFFER_SIZE
            );
            
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new RuntimeException("AudioRecord initialization failed");
            }
            
            // Initialize AudioTrack with optimized settings
            audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG_OUT,
                    AUDIO_FORMAT,
                    PLAYBACK_BUFFER_SIZE,
                    AudioTrack.MODE_STREAM
            );
            
            if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
                throw new RuntimeException("AudioTrack initialization failed");
            }
            
            Log.d(TAG, "Audio components initialized successfully");
            Log.d(TAG, "Capture buffer size: " + CAPTURE_BUFFER_SIZE + ", Playback buffer size: " + PLAYBACK_BUFFER_SIZE);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize audio components", e);
            if (listener != null) {
                listener.onError("Failed to initialize audio: " + e.getMessage());
            }
        }
    }
    
    public void startProcessing() {
        if (isRunning.get()) {
            Log.w(TAG, "Audio processing already running");
            return;
        }
        
        if (audioRecord == null || audioTrack == null) {
            Log.e(TAG, "Audio components not initialized");
            if (listener != null) {
                listener.onError("Audio components not initialized");
            }
            return;
        }
        
        isRunning.set(true);
        
        // Clear any existing data in queues
        captureQueue.clear();
        playbackQueue.clear();
        
        // Start audio capture and playback
        try {
            audioRecord.startRecording();
            audioTrack.play();
            
            // Start processing threads
            startCaptureThread();
            startPlaybackThread();
            
            Log.d(TAG, "Audio processing started");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start audio processing", e);
            if (listener != null) {
                listener.onError("Failed to start audio processing: " + e.getMessage());
            }
            stopProcessing();
        }
    }
    
    public void stopProcessing() {
        if (!isRunning.get()) {
            return;
        }
        
        isRunning.set(false);
        
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
                captureThread.join(1000);
            }
            
            if (playbackThread != null) {
                playbackThread.interrupt();
                playbackThread.join(1000);
            }
        } catch (InterruptedException e) {
            Log.w(TAG, "Thread interruption during shutdown", e);
        }
        
        // Clear queues
        captureQueue.clear();
        playbackQueue.clear();
        
        Log.d(TAG, "Audio processing stopped");
    }
    
    private void startCaptureThread() {
        captureThread = new Thread(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            
            byte[] buffer = new byte[CHUNK_SIZE * 2]; // 16-bit samples = 2 bytes per sample
            ByteArrayOutputStream chunkStream = new ByteArrayOutputStream();
            
            while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                    
                    if (bytesRead > 0) {
                        chunkStream.write(buffer, 0, bytesRead);
                        
                        // Calculate and report audio level
                        float audioLevel = calculateAudioLevel(buffer, bytesRead);
                        if (listener != null) {
                            listener.onAudioLevelChanged(audioLevel);
                        }
                        
                        // When we have enough data for a chunk, process it
                        if (chunkStream.size() >= CHUNK_SIZE * 2) {
                            byte[] chunk = chunkStream.toByteArray();
                            
                            // Add to capture queue for processing
                            if (!captureQueue.offer(chunk)) {
                                Log.w(TAG, "Capture queue full, dropping audio chunk");
                            }
                            
                            // Notify listener of captured audio
                            if (listener != null) {
                                listener.onAudioCaptured(chunk);
                            }
                            
                            // Reset for next chunk
                            chunkStream.reset();
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
        
        captureThread.setName("AudioCapture");
        captureThread.start();
    }
    
    private void startPlaybackThread() {
        playbackThread = new Thread(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            
            while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    // Wait for processed audio data
                    byte[] audioData = playbackQueue.take();
                    
                    if (audioData != null && audioData.length > 0) {
                        // Play the processed audio
                        int bytesWritten = audioTrack.write(audioData, 0, audioData.length);
                        
                        if (bytesWritten < 0) {
                            Log.e(TAG, "AudioTrack write error: " + bytesWritten);
                        }
                    }
                    
                } catch (InterruptedException e) {
                    Log.d(TAG, "Playback thread interrupted");
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
        
        playbackThread.setName("AudioPlayback");
        playbackThread.start();
    }
    
    /**
     * Queue processed audio data for playback
     */
    public void queueProcessedAudio(byte[] processedAudio) {
        if (isRunning.get() && processedAudio != null && processedAudio.length > 0) {
            if (!playbackQueue.offer(processedAudio)) {
                Log.w(TAG, "Playback queue full, dropping processed audio");
            }
        }
    }
    
    /**
     * Get the next captured audio chunk for processing
     */
    public byte[] getNextCapturedChunk() {
        return captureQueue.poll();
    }
    
    private float calculateAudioLevel(byte[] audioData, int length) {
        if (length < 2) return -60.0f; // Minimum level
        
        long sum = 0;
        int sampleCount = length / 2;
        
        // Convert bytes to 16-bit samples and calculate RMS
        for (int i = 0; i < length - 1; i += 2) {
            short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
            sum += sample * sample;
        }
        
        double rms = Math.sqrt((double) sum / sampleCount);
        
        // Convert to dB (with minimum floor to avoid log(0))
        if (rms < 1.0) rms = 1.0;
        return (float) (20 * Math.log10(rms / 32767.0));
    }
    
    /**
     * Convert PCM audio data to WAV format for API transmission
     */
    public static byte[] convertPcmToWav(byte[] pcmData) {
        ByteArrayOutputStream wavStream = new ByteArrayOutputStream();
        
        try {
            // WAV header for 16kHz, 16-bit, mono PCM
            wavStream.write("RIFF".getBytes());
            wavStream.write(intToLittleEndian(pcmData.length + 36));
            wavStream.write("WAVE".getBytes());
            wavStream.write("fmt ".getBytes());
            wavStream.write(intToLittleEndian(16)); // PCM format chunk size
            wavStream.write(shortToLittleEndian((short) 1)); // PCM format
            wavStream.write(shortToLittleEndian((short) 1)); // Mono
            wavStream.write(intToLittleEndian(SAMPLE_RATE)); // Sample rate
            wavStream.write(intToLittleEndian(SAMPLE_RATE * 2)); // Byte rate (sample rate * channels * bytes per sample)
            wavStream.write(shortToLittleEndian((short) 2)); // Block align (channels * bytes per sample)
            wavStream.write(shortToLittleEndian((short) 16)); // Bits per sample
            wavStream.write("data".getBytes());
            wavStream.write(intToLittleEndian(pcmData.length));
            wavStream.write(pcmData);
            
        } catch (IOException e) {
            Log.e(TAG, "Error creating WAV data", e);
            return pcmData; // Return original data if conversion fails
        }
        
        return wavStream.toByteArray();
    }
    
    private static byte[] intToLittleEndian(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }
    
    private static byte[] shortToLittleEndian(short value) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array();
    }
    
    public void release() {
        stopProcessing();
        
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
        
        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;
        }
        
        Log.d(TAG, "AudioProcessor released");
    }
    
    // Getters for audio configuration
    public static int getSampleRate() {
        return SAMPLE_RATE;
    }
    
    public static int getChunkSize() {
        return CHUNK_SIZE;
    }
}

