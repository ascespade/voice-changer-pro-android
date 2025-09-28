package com.voicechanger.app;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI Voice Analyzer - Records and analyzes voice samples using Gemini AI
 * Provides intelligent voice characteristics analysis for better cloning
 */
public class AIVoiceAnalyzer {
    private static final String TAG = "AIVoiceAnalyzer";
    
    // Audio configuration
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    
    private Context context;
    private AudioRecord audioRecord;
    private ExecutorService executorService;
    private GeminiAIService geminiAIService;
    
    // Recording state
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private Thread recordingThread;
    private ByteArrayOutputStream audioBuffer;
    
    // Analysis state
    private GeminiAIService.VoiceAnalysisResult lastAnalysisResult;
    private GeminiAIService.VoiceTemplate lastGeneratedTemplate;
    
    public interface VoiceAnalyzerListener {
        void onRecordingStarted();
        void onRecordingStopped(byte[] audioData);
        void onAnalysisComplete(GeminiAIService.VoiceAnalysisResult result);
        void onTemplateGenerated(GeminiAIService.VoiceTemplate template);
        void onError(String message);
    }
    
    private VoiceAnalyzerListener listener;
    
    public AIVoiceAnalyzer(Context context) {
        this.context = context;
        this.executorService = Executors.newFixedThreadPool(2);
        this.geminiAIService = new GeminiAIService(context);
        this.audioBuffer = new ByteArrayOutputStream();
        
        Log.d(TAG, "AI Voice Analyzer initialized");
    }
    
    public void setListener(VoiceAnalyzerListener listener) {
        this.listener = listener;
    }
    
    /**
     * Start recording voice sample for analysis
     */
    public void startRecording() {
        if (isRecording.get()) {
            Log.w(TAG, "Recording already in progress");
            return;
        }
        
        try {
            // Initialize AudioRecord
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    BUFFER_SIZE
            );
            
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new RuntimeException("AudioRecord initialization failed");
            }
            
            // Clear previous audio buffer
            audioBuffer.reset();
            
            // Start recording
            audioRecord.startRecording();
            isRecording.set(true);
            
            // Start recording thread
            startRecordingThread();
            
            if (listener != null) {
                listener.onRecordingStarted();
            }
            
            Log.d(TAG, "Voice recording started for AI analysis");
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting voice recording", e);
            if (listener != null) {
                listener.onError("Failed to start recording: " + e.getMessage());
            }
        }
    }
    
    /**
     * Stop recording and analyze the voice sample
     */
    public void stopRecordingAndAnalyze() {
        if (!isRecording.get()) {
            Log.w(TAG, "No recording in progress");
            return;
        }
        
        try {
            // Stop recording
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }
            
            isRecording.set(false);
            
            // Stop recording thread
            if (recordingThread != null) {
                recordingThread.interrupt();
                recordingThread = null;
            }
            
            // Get recorded audio data
            byte[] audioData = audioBuffer.toByteArray();
            
            if (listener != null) {
                listener.onRecordingStopped(audioData);
            }
            
            // Analyze the voice sample using Gemini AI
            analyzeVoiceSample(audioData);
            
            Log.d(TAG, "Voice recording stopped and analysis started");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping voice recording", e);
            if (listener != null) {
                listener.onError("Failed to stop recording: " + e.getMessage());
            }
        }
    }
    
    /**
     * Analyze voice sample using Gemini AI
     */
    private void analyzeVoiceSample(byte[] audioData) {
        if (audioData.length == 0) {
            if (listener != null) {
                listener.onError("No audio data to analyze");
            }
            return;
        }
        
        // Convert PCM to WAV format for better AI analysis
        byte[] wavData = convertPcmToWav(audioData);
        
        // Analyze using Gemini AI
        geminiAIService.analyzeVoiceCharacteristics(wavData, "wav", new GeminiAIService.VoiceAnalysisListener() {
            @Override
            public void onVoiceAnalysisComplete(GeminiAIService.VoiceAnalysisResult result) {
                lastAnalysisResult = result;
                
                if (listener != null) {
                    listener.onAnalysisComplete(result);
                }
                
                Log.d(TAG, "Voice analysis completed - Gender: " + result.gender + 
                          ", Age: " + result.estimatedAge + 
                          ", Confidence: " + result.confidence);
            }
            
            @Override
            public void onTemplateGenerated(GeminiAIService.VoiceTemplate template) {
                lastGeneratedTemplate = template;
                
                if (listener != null) {
                    listener.onTemplateGenerated(template);
                }
                
                Log.d(TAG, "Voice template generated - " + template.name);
            }
            
            @Override
            public void onError(String message) {
                if (listener != null) {
                    listener.onError("Voice analysis failed: " + message);
                }
            }
        });
    }
    
    /**
     * Generate intelligent voice template based on analysis
     */
    public void generateIntelligentTemplate(String targetVoice) {
        if (lastAnalysisResult == null) {
            if (listener != null) {
                listener.onError("No voice analysis available. Please analyze voice first.");
            }
            return;
        }
        
        geminiAIService.generateVoiceTemplate(lastAnalysisResult, targetVoice, new GeminiAIService.VoiceAnalysisListener() {
            @Override
            public void onVoiceAnalysisComplete(GeminiAIService.VoiceAnalysisResult result) {
                // Not used in template generation
            }
            
            @Override
            public void onTemplateGenerated(GeminiAIService.VoiceTemplate template) {
                lastGeneratedTemplate = template;
                
                if (listener != null) {
                    listener.onTemplateGenerated(template);
                }
                
                Log.d(TAG, "Intelligent template generated for target: " + targetVoice);
            }
            
            @Override
            public void onError(String message) {
                if (listener != null) {
                    listener.onError("Template generation failed: " + message);
                }
            }
        });
    }
    
    private void startRecordingThread() {
        recordingThread = new Thread(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            
            byte[] buffer = new byte[BUFFER_SIZE];
            
            while (isRecording.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                    
                    if (bytesRead > 0) {
                        audioBuffer.write(buffer, 0, bytesRead);
                    } else if (bytesRead < 0) {
                        Log.e(TAG, "AudioRecord read error: " + bytesRead);
                        break;
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error in recording thread", e);
                    break;
                }
            }
            
            Log.d(TAG, "Recording thread ended");
        });
        
        recordingThread.setName("AIVoiceRecording");
        recordingThread.start();
    }
    
    /**
     * Convert PCM audio data to WAV format
     */
    private byte[] convertPcmToWav(byte[] pcmData) {
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
            wavStream.write(intToLittleEndian(SAMPLE_RATE * 2)); // Byte rate
            wavStream.write(shortToLittleEndian((short) 2)); // Block align
            wavStream.write(shortToLittleEndian((short) 16)); // Bits per sample
            wavStream.write("data".getBytes());
            wavStream.write(intToLittleEndian(pcmData.length));
            wavStream.write(pcmData);
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating WAV data", e);
            return pcmData; // Return original data if conversion fails
        }
        
        return wavStream.toByteArray();
    }
    
    private byte[] intToLittleEndian(int value) {
        return java.nio.ByteBuffer.allocate(4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }
    
    private byte[] shortToLittleEndian(short value) {
        return java.nio.ByteBuffer.allocate(2).order(java.nio.ByteOrder.LITTLE_ENDIAN).putShort(value).array();
    }
    
    /**
     * Get the last analysis result
     */
    public GeminiAIService.VoiceAnalysisResult getLastAnalysisResult() {
        return lastAnalysisResult;
    }
    
    /**
     * Get the last generated template
     */
    public GeminiAIService.VoiceTemplate getLastGeneratedTemplate() {
        return lastGeneratedTemplate;
    }
    
    /**
     * Check if currently recording
     */
    public boolean isRecording() {
        return isRecording.get();
    }
    
    /**
     * Get recording duration in seconds
     */
    public long getRecordingDuration() {
        if (audioBuffer == null) return 0;
        int bytesPerSecond = SAMPLE_RATE * 2; // 16-bit = 2 bytes per sample
        return audioBuffer.size() / bytesPerSecond;
    }
    
    public void release() {
        if (isRecording.get()) {
            stopRecordingAndAnalyze();
        }
        
        if (executorService != null) {
            executorService.shutdown();
        }
        
        if (geminiAIService != null) {
            geminiAIService.release();
        }
        
        Log.d(TAG, "AI Voice Analyzer released");
    }
}
