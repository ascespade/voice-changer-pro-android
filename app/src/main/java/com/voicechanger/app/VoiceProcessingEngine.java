package com.voicechanger.app;

import android.content.Context;
import android.util.Log;

/**
 * Core voice processing engine that handles audio capture, transformation via ElevenLabs API,
 * and playback of the transformed audio using the enhanced RealTimeVoiceChanger.
 */
public class VoiceProcessingEngine implements RealTimeVoiceChanger.RealTimeVoiceChangerListener {
    private static final String TAG = "VoiceProcessingEngine";
    
    private Context context;
    private RealTimeVoiceChanger realTimeVoiceChanger;
    
    // Voice configuration
    private String selectedVoiceId = "default_voice";
    private String apiKey;
    
    // Listeners
    private VoiceProcessingListener listener;
    
    public interface VoiceProcessingListener {
        void onProcessingStarted();
        void onProcessingStopped();
        void onError(String error);
        void onAudioLevelChanged(float level);
    }
    
    public VoiceProcessingEngine(Context context) {
        this.context = context;
        this.realTimeVoiceChanger = new RealTimeVoiceChanger(context);
        this.realTimeVoiceChanger.setListener(this);
        
        Log.d(TAG, "VoiceProcessingEngine initialized with enhanced real-time processing");
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        if (realTimeVoiceChanger != null) {
            realTimeVoiceChanger.setApiKey(apiKey);
        }
    }
    
    public void setSelectedVoiceId(String voiceId) {
        this.selectedVoiceId = voiceId;
        if (realTimeVoiceChanger != null) {
            realTimeVoiceChanger.setSelectedVoiceId(voiceId);
        }
    }
    
    public void setListener(VoiceProcessingListener listener) {
        this.listener = listener;
    }
    
    public void startRealTimeProcessing() {
        if (realTimeVoiceChanger != null) {
            realTimeVoiceChanger.startRealTimeVoiceChanging();
        }
    }
    
    public void stopRealTimeProcessing() {
        if (realTimeVoiceChanger != null) {
            realTimeVoiceChanger.stopRealTimeVoiceChanging();
        }
    }
    
    public boolean isProcessing() {
        return realTimeVoiceChanger != null && realTimeVoiceChanger.isProcessing();
    }
    
    public RealTimeVoiceChanger.PerformanceStats getPerformanceStats() {
        return realTimeVoiceChanger != null ? realTimeVoiceChanger.getPerformanceStats() : null;
    }
    
    // RealTimeVoiceChanger.RealTimeVoiceChangerListener implementation
    @Override
    public void onVoiceChangingStarted() {
        Log.d(TAG, "Voice changing started");
        if (listener != null) {
            listener.onProcessingStarted();
        }
    }
    
    @Override
    public void onVoiceChangingStopped() {
        Log.d(TAG, "Voice changing stopped");
        if (listener != null) {
            listener.onProcessingStopped();
        }
    }
    
    @Override
    public void onError(String error) {
        Log.e(TAG, "Voice changing error: " + error);
        if (listener != null) {
            listener.onError(error);
        }
    }
    
    @Override
    public void onAudioLevelChanged(float level) {
        if (listener != null) {
            listener.onAudioLevelChanged(level);
        }
    }
    
    @Override
    public void onPerformanceUpdate(long avgLatency, float successRate) {
        Log.d(TAG, String.format("Performance update - Latency: %dms, Success: %.1f%%", 
                avgLatency, successRate));
        // Could forward this to UI if needed
    }
    
    public void release() {
        if (realTimeVoiceChanger != null) {
            realTimeVoiceChanger.release();
            realTimeVoiceChanger = null;
        }
        
        Log.d(TAG, "VoiceProcessingEngine released");
    }
}

