package com.voicechanger.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ErrorManager {
    private static final String TAG = "ErrorManager";
    private static final String PREFS_NAME = "ErrorManagerPrefs";
    private static final String KEY_ERROR_COUNT = "error_count";
    private static final String KEY_LAST_ERROR = "last_error";
    private static final String KEY_ERROR_HISTORY = "error_history";
    
    private static ErrorManager instance;
    private Context context;
    private SharedPreferences preferences;
    private ConcurrentLinkedQueue<ErrorInfo> errorQueue;
    private List<ErrorListener> listeners;
    private int errorCount = 0;
    private String lastError = "";
    
    public interface ErrorListener {
        void onErrorOccurred(ErrorInfo error);
        void onErrorRecovered();
    }
    
    public static class ErrorInfo {
        public enum ErrorType {
            AUDIO_INITIALIZATION,
            PERMISSION_DENIED,
            SERVICE_UNAVAILABLE,
            NETWORK_ERROR,
            API_ERROR,
            PROCESSING_ERROR,
            UNKNOWN
        }
        
        public enum ErrorSeverity {
            LOW,
            MEDIUM,
            HIGH,
            CRITICAL
        }
        
        private final String message;
        private final ErrorType type;
        private final ErrorSeverity severity;
        private final long timestamp;
        private final String stackTrace;
        private final String context;
        
        public ErrorInfo(String message, ErrorType type, ErrorSeverity severity, String stackTrace, String context) {
            this.message = message;
            this.type = type;
            this.severity = severity;
            this.timestamp = System.currentTimeMillis();
            this.stackTrace = stackTrace;
            this.context = context;
        }
        
        // Getters
        public String getMessage() { return message; }
        public ErrorType getType() { return type; }
        public ErrorSeverity getSeverity() { return severity; }
        public long getTimestamp() { return timestamp; }
        public String getStackTrace() { return stackTrace; }
        public String getContext() { return context; }
        
        @Override
        public String toString() {
            return String.format("[%s] %s: %s (Severity: %s)", 
                new java.util.Date(timestamp), type.name(), message, severity.name());
        }
    }
    
    private ErrorManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.errorQueue = new ConcurrentLinkedQueue<>();
        this.listeners = new ArrayList<>();
        loadErrorData();
    }
    
    public static synchronized ErrorManager getInstance(Context context) {
        if (instance == null) {
            instance = new ErrorManager(context);
        }
        return instance;
    }
    
    public void addErrorListener(ErrorListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    public void removeErrorListener(ErrorListener listener) {
        listeners.remove(listener);
    }
    
    public void reportError(String message, ErrorInfo.ErrorType type, ErrorInfo.ErrorSeverity severity) {
        reportError(message, type, severity, null, null);
    }
    
    public void reportError(String message, ErrorInfo.ErrorType type, ErrorInfo.ErrorSeverity severity, String stackTrace, String context) {
        ErrorInfo error = new ErrorInfo(message, type, severity, stackTrace, context);
        
        // Add to queue
        errorQueue.offer(error);
        
        // Update counters
        errorCount++;
        lastError = message;
        
        // Save to preferences
        saveErrorData();
        
        // Log error
        Log.e(TAG, "Error reported: " + error.toString());
        
        // Notify listeners
        notifyErrorListeners(error);
        
        // Handle error based on severity
        handleErrorBySeverity(error);
    }
    
    public void reportException(Exception exception, ErrorInfo.ErrorType type, ErrorInfo.ErrorSeverity severity, String context) {
        String message = exception.getMessage() != null ? exception.getMessage() : "Unknown exception";
        String stackTrace = Log.getStackTraceString(exception);
        reportError(message, type, severity, stackTrace, context);
    }
    
    public void reportRecovery() {
        Log.i(TAG, "Error recovery reported");
        notifyRecoveryListeners();
    }
    
    public int getErrorCount() {
        return errorCount;
    }
    
    public String getLastError() {
        return lastError;
    }
    
    public List<ErrorInfo> getRecentErrors(int count) {
        List<ErrorInfo> recentErrors = new ArrayList<>();
        ErrorInfo[] errors = errorQueue.toArray(new ErrorInfo[0]);
        
        int startIndex = Math.max(0, errors.length - count);
        for (int i = startIndex; i < errors.length; i++) {
            recentErrors.add(errors[i]);
        }
        
        return recentErrors;
    }
    
    public List<ErrorInfo> getErrorsByType(ErrorInfo.ErrorType type) {
        List<ErrorInfo> filteredErrors = new ArrayList<>();
        for (ErrorInfo error : errorQueue) {
            if (error.getType() == type) {
                filteredErrors.add(error);
            }
        }
        return filteredErrors;
    }
    
    public List<ErrorInfo> getErrorsBySeverity(ErrorInfo.ErrorSeverity severity) {
        List<ErrorInfo> filteredErrors = new ArrayList<>();
        for (ErrorInfo error : errorQueue) {
            if (error.getSeverity() == severity) {
                filteredErrors.add(error);
            }
        }
        return filteredErrors;
    }
    
    public void clearErrorHistory() {
        errorQueue.clear();
        errorCount = 0;
        lastError = "";
        saveErrorData();
        Log.i(TAG, "Error history cleared");
    }
    
    public boolean hasRecentErrors(int timeWindowMs) {
        long currentTime = System.currentTimeMillis();
        for (ErrorInfo error : errorQueue) {
            if (currentTime - error.getTimestamp() < timeWindowMs) {
                return true;
            }
        }
        return false;
    }
    
    public boolean hasCriticalErrors() {
        return !getErrorsBySeverity(ErrorInfo.ErrorSeverity.CRITICAL).isEmpty();
    }
    
    private void notifyErrorListeners(ErrorInfo error) {
        for (ErrorListener listener : listeners) {
            try {
                listener.onErrorOccurred(error);
            } catch (Exception e) {
                Log.e(TAG, "Error in error listener: " + e.getMessage());
            }
        }
    }
    
    private void notifyRecoveryListeners() {
        for (ErrorListener listener : listeners) {
            try {
                listener.onErrorRecovered();
            } catch (Exception e) {
                Log.e(TAG, "Error in recovery listener: " + e.getMessage());
            }
        }
    }
    
    private void handleErrorBySeverity(ErrorInfo error) {
        switch (error.getSeverity()) {
            case LOW:
                Log.d(TAG, "Low severity error: " + error.getMessage());
                break;
            case MEDIUM:
                Log.w(TAG, "Medium severity error: " + error.getMessage());
                break;
            case HIGH:
                Log.e(TAG, "High severity error: " + error.getMessage());
                // Could trigger user notification
                break;
            case CRITICAL:
                Log.e(TAG, "Critical error: " + error.getMessage());
                // Could trigger app restart or emergency mode
                break;
        }
    }
    
    private void loadErrorData() {
        errorCount = preferences.getInt(KEY_ERROR_COUNT, 0);
        lastError = preferences.getString(KEY_LAST_ERROR, "");
        
        // Load error history from preferences (simplified)
        String errorHistory = preferences.getString(KEY_ERROR_HISTORY, "");
        if (!errorHistory.isEmpty()) {
            // Parse error history (simplified implementation)
            Log.d(TAG, "Loaded error history: " + errorHistory);
        }
    }
    
    private void saveErrorData() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(KEY_ERROR_COUNT, errorCount);
        editor.putString(KEY_LAST_ERROR, lastError);
        
        // Save error history (simplified)
        StringBuilder historyBuilder = new StringBuilder();
        for (ErrorInfo error : errorQueue) {
            historyBuilder.append(error.toString()).append(";");
        }
        editor.putString(KEY_ERROR_HISTORY, historyBuilder.toString());
        
        editor.apply();
    }
    
    // Utility methods for common error scenarios
    public static void reportAudioError(String message, Exception exception, Context context) {
        ErrorManager.getInstance(context).reportException(
            exception, 
            ErrorInfo.ErrorType.AUDIO_INITIALIZATION, 
            ErrorInfo.ErrorSeverity.HIGH,
            "Audio processing"
        );
    }
    
    public static void reportPermissionError(String permission, Context context) {
        ErrorManager.getInstance(context).reportError(
            "Permission denied: " + permission,
            ErrorInfo.ErrorType.PERMISSION_DENIED,
            ErrorInfo.ErrorSeverity.MEDIUM,
            null,
            "Permission request"
        );
    }
    
    public static void reportServiceError(String serviceName, Exception exception, Context context) {
        ErrorManager.getInstance(context).reportException(
            exception,
            ErrorInfo.ErrorType.SERVICE_UNAVAILABLE,
            ErrorInfo.ErrorSeverity.HIGH,
            "Service: " + serviceName
        );
    }
    
    public static void reportNetworkError(String operation, Exception exception, Context context) {
        ErrorManager.getInstance(context).reportException(
            exception,
            ErrorInfo.ErrorType.NETWORK_ERROR,
            ErrorInfo.ErrorSeverity.MEDIUM,
            "Network operation: " + operation
        );
    }
    
    public static void reportAPIError(String apiEndpoint, int statusCode, String response, Context context) {
        ErrorManager.getInstance(context).reportError(
            "API error: " + statusCode + " - " + response,
            ErrorInfo.ErrorType.API_ERROR,
            ErrorInfo.ErrorSeverity.MEDIUM,
            null,
            "API: " + apiEndpoint
        );
    }
}
