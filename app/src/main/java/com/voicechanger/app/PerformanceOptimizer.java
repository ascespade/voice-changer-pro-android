package com.voicechanger.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.atomic.AtomicLong;

public class PerformanceOptimizer {
    private static final String TAG = "PerformanceOptimizer";
    private static final String PREFS_NAME = "PerformanceOptimizerPrefs";
    private static final String KEY_OPTIMIZATION_LEVEL = "optimization_level";
    private static final String KEY_AUTO_OPTIMIZE = "auto_optimize";
    
    public enum OptimizationLevel {
        DISABLED(0, "Disabled"),
        LOW(1, "Low"),
        MEDIUM(2, "Medium"),
        HIGH(3, "High"),
        MAXIMUM(4, "Maximum");
        
        private final int value;
        private final String name;
        
        OptimizationLevel(int value, String name) {
            this.value = value;
            this.name = name;
        }
        
        public int getValue() { return value; }
        public String getName() { return name; }
    }
    
    public interface PerformanceListener {
        void onPerformanceUpdate(long latency, float cpuUsage, float memoryUsage);
        void onOptimizationApplied(OptimizationLevel level);
    }
    
    private static PerformanceOptimizer instance;
    private Context context;
    private SharedPreferences preferences;
    private Handler mainHandler;
    private PerformanceListener listener;
    
    // Performance metrics
    private final AtomicLong totalLatency = new AtomicLong(0);
    private final AtomicLong totalChunks = new AtomicLong(0);
    private final AtomicLong successfulChunks = new AtomicLong(0);
    
    // Optimization settings
    private OptimizationLevel currentLevel = OptimizationLevel.MEDIUM;
    private boolean autoOptimize = true;
    private boolean isOptimizing = false;
    
    // Performance monitoring
    private long lastOptimizationTime = 0;
    private static final long OPTIMIZATION_INTERVAL = 30000; // 30 seconds
    
    private PerformanceOptimizer(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.mainHandler = new Handler(Looper.getMainLooper());
        loadSettings();
    }
    
    public static synchronized PerformanceOptimizer getInstance(Context context) {
        if (instance == null) {
            instance = new PerformanceOptimizer(context);
        }
        return instance;
    }
    
    public void setListener(PerformanceListener listener) {
        this.listener = listener;
    }
    
    public void updateLatency(long latency) {
        totalLatency.addAndGet(latency);
        totalChunks.incrementAndGet();
        
        if (autoOptimize && shouldOptimize()) {
            optimizePerformance();
        }
        
        notifyPerformanceUpdate();
    }
    
    public void updateSuccess(boolean success) {
        if (success) {
            successfulChunks.incrementAndGet();
        }
    }
    
    public long getAverageLatency() {
        long chunks = totalChunks.get();
        return chunks > 0 ? totalLatency.get() / chunks : 0;
    }
    
    public float getSuccessRate() {
        long chunks = totalChunks.get();
        return chunks > 0 ? (float) successfulChunks.get() / chunks * 100 : 0;
    }
    
    public float getCpuUsage() {
        // Simplified CPU usage calculation
        // In a real implementation, you would use system APIs to get actual CPU usage
        return Math.min(100.0f, getAverageLatency() / 10.0f);
    }
    
    public float getMemoryUsage() {
        // Simplified memory usage calculation
        // In a real implementation, you would use system APIs to get actual memory usage
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return (float) usedMemory / maxMemory * 100;
    }
    
    public void setOptimizationLevel(OptimizationLevel level) {
        this.currentLevel = level;
        saveSettings();
        applyOptimization();
        Log.d(TAG, "Optimization level set to: " + level.getName());
    }
    
    private void applyOptimization() {
        // Apply optimization based on current level
        if (currentLevel == OptimizationLevel.HIGH || currentLevel == OptimizationLevel.MAXIMUM) {
            // High performance optimizations
            System.gc(); // Force garbage collection
        } else if (currentLevel == OptimizationLevel.MEDIUM) {
            // Balanced optimizations
        } else if (currentLevel == OptimizationLevel.LOW) {
            // Battery saving optimizations
        }
    }
    
    public OptimizationLevel getOptimizationLevel() {
        return currentLevel;
    }
    
    public void setAutoOptimize(boolean enabled) {
        this.autoOptimize = enabled;
        saveSettings();
        Log.d(TAG, "Auto-optimize set to: " + enabled);
    }
    
    public boolean isAutoOptimize() {
        return autoOptimize;
    }
    
    public void optimizePerformance() {
        if (isOptimizing) {
            return;
        }
        
        isOptimizing = true;
        lastOptimizationTime = System.currentTimeMillis();
        
        Log.d(TAG, "Starting performance optimization...");
        
        // Apply optimizations based on current level
        switch (currentLevel) {
            case DISABLED:
                // No optimizations
                break;
            case LOW:
                applyLowOptimizations();
                break;
            case MEDIUM:
                applyMediumOptimizations();
                break;
            case HIGH:
                applyHighOptimizations();
                break;
            case MAXIMUM:
                applyMaximumOptimizations();
                break;
        }
        
        // Notify listener
        if (listener != null) {
            mainHandler.post(() -> listener.onOptimizationApplied(currentLevel));
        }
        
        isOptimizing = false;
        Log.d(TAG, "Performance optimization completed");
    }
    
    private boolean shouldOptimize() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastOptimizationTime) > OPTIMIZATION_INTERVAL;
    }
    
    private void applyLowOptimizations() {
        Log.d(TAG, "Applying low-level optimizations");
        // Basic optimizations like reducing buffer sizes
        System.gc(); // Suggest garbage collection
    }
    
    private void applyMediumOptimizations() {
        Log.d(TAG, "Applying medium-level optimizations");
        applyLowOptimizations();
        // Medium optimizations like adjusting thread priorities
    }
    
    private void applyHighOptimizations() {
        Log.d(TAG, "Applying high-level optimizations");
        applyMediumOptimizations();
        // High optimizations like memory management
        System.gc();
    }
    
    private void applyMaximumOptimizations() {
        Log.d(TAG, "Applying maximum optimizations");
        applyHighOptimizations();
        // Maximum optimizations like aggressive memory management
        System.gc();
        System.runFinalization();
    }
    
    private void notifyPerformanceUpdate() {
        if (listener != null) {
            mainHandler.post(() -> {
                listener.onPerformanceUpdate(
                    getAverageLatency(),
                    getCpuUsage(),
                    getMemoryUsage()
                );
            });
        }
    }
    
    public void resetMetrics() {
        totalLatency.set(0);
        totalChunks.set(0);
        successfulChunks.set(0);
        Log.d(TAG, "Performance metrics reset");
    }
    
    public String getPerformanceReport() {
        StringBuilder report = new StringBuilder();
        report.append("Performance Report:\n");
        report.append("Average Latency: ").append(getAverageLatency()).append(" ms\n");
        report.append("Success Rate: ").append(String.format("%.1f", getSuccessRate())).append("%\n");
        report.append("CPU Usage: ").append(String.format("%.1f", getCpuUsage())).append("%\n");
        report.append("Memory Usage: ").append(String.format("%.1f", getMemoryUsage())).append("%\n");
        report.append("Optimization Level: ").append(currentLevel.getName()).append("\n");
        report.append("Auto-Optimize: ").append(autoOptimize ? "Enabled" : "Disabled");
        return report.toString();
    }
    
    private void loadSettings() {
        int levelValue = preferences.getInt(KEY_OPTIMIZATION_LEVEL, OptimizationLevel.MEDIUM.getValue());
        for (OptimizationLevel level : OptimizationLevel.values()) {
            if (level.getValue() == levelValue) {
                currentLevel = level;
                break;
            }
        }
        autoOptimize = preferences.getBoolean(KEY_AUTO_OPTIMIZE, true);
        Log.d(TAG, "Settings loaded - Level: " + currentLevel.getName() + ", Auto-optimize: " + autoOptimize);
    }
    
    private void saveSettings() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(KEY_OPTIMIZATION_LEVEL, currentLevel.getValue());
        editor.putBoolean(KEY_AUTO_OPTIMIZE, autoOptimize);
        editor.apply();
        Log.d(TAG, "Settings saved");
    }
    
    // Utility methods for specific optimizations
    public void optimizeForLowLatency() {
        Log.d(TAG, "Optimizing for low latency");
        setOptimizationLevel(OptimizationLevel.HIGH);
    }
    
    public void optimizeForQuality() {
        Log.d(TAG, "Optimizing for quality");
        setOptimizationLevel(OptimizationLevel.MEDIUM);
    }
    
    public void optimizeForBattery() {
        Log.d(TAG, "Optimizing for battery life");
        setOptimizationLevel(OptimizationLevel.LOW);
    }
    
    public void optimizeForMaximumPerformance() {
        Log.d(TAG, "Optimizing for maximum performance");
        setOptimizationLevel(OptimizationLevel.MAXIMUM);
    }
}
