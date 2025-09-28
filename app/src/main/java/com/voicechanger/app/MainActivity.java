package com.voicechanger.app;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.voicechanger.app.SystemWideVoiceProcessor.VoiceProcessingMode;
import com.voicechanger.app.VoiceCloningEngine.VoiceCloningMode;
import com.voicechanger.app.VoiceTemplateManager.VoiceTemplate;
import com.voicechanger.app.VoiceTemplateManager.VoiceProfile;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SystemWideVoiceProcessor.VoiceProcessorListener, VoiceCloningEngine.VoiceCloningListener, LiveCallOptimizer.LiveCallListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1001;
    private static final int REQUEST_CODE_AUDIO_PERMISSIONS = 1002;
    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 1003;
    private static final int REQUEST_CODE_MEDIA_PROJECTION = 1004;

    private Button startStopButton;
    private TextView statusTextView;
    private TextView latencyTextView;
    private TextView successRateTextView;
    private Spinner voiceModelSpinner;
    private Spinner processingModeSpinner;
    private EditText apiKeyEditText;

    private SystemWideAudioService systemWideAudioService;
    private LiveCallOptimizer liveCallOptimizer;
    private VoiceTemplateManager voiceTemplateManager;
    private AIVoiceAnalyzer aiVoiceAnalyzer;
    private boolean isServiceBound = false;
    private ActivityResultLauncher<Intent> mediaProjectionLauncher;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected: " + name.getClassName());
            isServiceBound = true;
            if (SystemWideAudioService.getInstance() != null) {
                systemWideAudioService = SystemWideAudioService.getInstance();
                if (systemWideAudioService.voiceProcessor != null) {
                    systemWideAudioService.voiceProcessor.setListener(MainActivity.this);
                }
                updateUiState();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service disconnected: " + name.getClassName());
            isServiceBound = false;
            systemWideAudioService = null;
            updateUiState();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupSpinners();
        initializeVoiceSystems();
        checkAndRequestPermissions();
        registerMediaProjectionLauncher();
    }

    private void initViews() {
        startStopButton = findViewById(R.id.startStopButton);
        statusTextView = findViewById(R.id.statusTextView);
        latencyTextView = findViewById(R.id.latencyTextView);
        successRateTextView = findViewById(R.id.successRateTextView);
        voiceModelSpinner = findViewById(R.id.voiceModelSpinner);
        processingModeSpinner = findViewById(R.id.processingModeSpinner);
        apiKeyEditText = findViewById(R.id.apiKeyEditText);

        startStopButton.setOnClickListener(v -> toggleVoiceChanger());
    }
    
    private void initializeVoiceSystems() {
        // Initialize voice template manager
        voiceTemplateManager = new VoiceTemplateManager(this);
        
        // Initialize live call optimizer
        liveCallOptimizer = new LiveCallOptimizer(this);
        liveCallOptimizer.setListener(this);
        
        // Initialize AI voice analyzer
        aiVoiceAnalyzer = new AIVoiceAnalyzer(this);
        aiVoiceAnalyzer.setListener(new AIVoiceAnalyzer.VoiceAnalyzerListener() {
            @Override
            public void onRecordingStarted() {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "AI Voice Analysis Started", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onRecordingStopped(byte[] audioData) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Recording stopped, analyzing with AI...", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onAnalysisComplete(GeminiAIService.VoiceAnalysisResult result) {
                runOnUiThread(() -> {
                    String message = String.format("AI Analysis Complete:\nGender: %s\nAge: %d\nConfidence: %.1f%%", 
                            result.gender, result.estimatedAge, result.confidence * 100);
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                });
            }
            
            @Override
            public void onTemplateGenerated(GeminiAIService.VoiceTemplate template) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "AI Template Generated: " + template.name, Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "AI Error: " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
        
        Log.d(TAG, "Voice systems with AI initialized");
    }

    private void setupSpinners() {
        // Voice Models - Load from template manager
        List<VoiceTemplate> templates = voiceTemplateManager.getAllTemplates();
        List<String> voiceModelNames = new ArrayList<>();
        for (VoiceTemplate template : templates) {
            voiceModelNames.add(template.name);
        }
        
        ArrayAdapter<String> voiceModelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, voiceModelNames);
        voiceModelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        voiceModelSpinner.setAdapter(voiceModelAdapter);
        voiceModelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedModelName = (String) parent.getItemAtPosition(position);
                
                // Find template by name
                VoiceTemplate selectedTemplate = null;
                for (VoiceTemplate template : templates) {
                    if (template.name.equals(selectedModelName)) {
                        selectedTemplate = template;
                        break;
                    }
                }
                
                if (selectedTemplate != null) {
                    // Set voice in live call optimizer
                    if (liveCallOptimizer != null) {
                        liveCallOptimizer.setVoiceProfile(selectedTemplate.id);
                    }
                    
                    // Set voice in system wide audio service
                    if (systemWideAudioService != null && systemWideAudioService.voiceProcessor != null) {
                        systemWideAudioService.voiceProcessor.setVoiceModel(selectedTemplate.id);
                    }
                    
                    Toast.makeText(MainActivity.this, "Voice set to: " + selectedTemplate.name, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Processing Modes
        List<String> processingModes = new ArrayList<>();
        for (VoiceProcessingMode mode : VoiceProcessingMode.values()) {
            processingModes.add(mode.name());
        }
        ArrayAdapter<String> processingModeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, processingModes);
        processingModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        processingModeSpinner.setAdapter(processingModeAdapter);
        processingModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                VoiceProcessingMode selectedMode = VoiceProcessingMode.valueOf((String) parent.getItemAtPosition(position));
                if (systemWideAudioService != null && systemWideAudioService.voiceProcessor != null) {
                    systemWideAudioService.voiceProcessor.setProcessingMode(selectedMode);
                    Toast.makeText(MainActivity.this, "Processing mode set to: " + selectedMode.name(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void checkAndRequestPermissions() {
        // Request RECORD_AUDIO permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_AUDIO_PERMISSIONS);
        }

        // Request POST_NOTIFICATIONS permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_POST_NOTIFICATIONS);
            }
        }

        // Check for Accessibility Service
        if (!isAccessibilityServiceEnabled()) {
            showAccessibilityServiceDialog();
        }

        // Check for Overlay Permission (needed for MediaProjection on some devices)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            showOverlayPermissionDialog();
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        String service = getPackageName() + "/" + SystemWideAudioService.class.getCanonicalName();
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(getApplicationContext().getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Error finding setting, default accessibility to not enabled: " + e.getMessage());
        }
        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                return settingValue.contains(service);
            }
        }
        return false;
    }

    private void showAccessibilityServiceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Enable Accessibility Service")
                .setMessage("This app requires the Accessibility Service to be enabled for system-wide voice changing. Please enable it in settings.")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showOverlayPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Grant Overlay Permission")
                .setMessage("This app requires 'Display over other apps' permission for Media Projection to work correctly on some devices. Please grant it in settings.")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void registerMediaProjectionLauncher() {
        mediaProjectionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        int resultCode = result.getResultCode();
                        
                        // Start MediaProjectionService to manage the MediaProjection
                        Intent serviceIntent = new Intent(this, MediaProjectionService.class);
                        serviceIntent.setAction("START_PROJECTION");
                        serviceIntent.putExtra("resultCode", resultCode);
                        serviceIntent.putExtra("data", data);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(serviceIntent);
                        } else {
                            startService(serviceIntent);
                        }

                        // Pass MediaProjection to SystemWideAudioService
                        if (systemWideAudioService != null) {
                            MediaProjectionManager mpManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                            systemWideAudioService.setMediaProjection(mpManager.getMediaProjection(resultCode, data));
                            systemWideAudioService.startSystemWideCapture();
                            updateUiState();
                        } else {
                            Log.e(TAG, "SystemWideAudioService not available to set MediaProjection.");
                            Toast.makeText(this, "Service not ready. Try again.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Media Projection permission denied.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Media Projection permission denied.");
                        // Fallback to microphone capture if MediaProjection is denied
                        if (systemWideAudioService != null) {
                            systemWideAudioService.startSystemWideCapture(); // This will use microphone
                            updateUiState();
                        }
                    }
                }
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_AUDIO_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Audio permission granted.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Audio permission denied. Cannot record audio.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied. App may not show foreground service notifications.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Bind to the service to get its instance
        Intent serviceIntent = new Intent(this, SystemWideAudioService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        updateUiState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    private void toggleVoiceChanger() {
        if (systemWideAudioService == null) {
            Toast.makeText(this, "Service not ready. Please wait.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (systemWideAudioService.isCapturing()) {
            systemWideAudioService.stopSystemWideCapture();
            Toast.makeText(this, "Voice Changer Stopped", Toast.LENGTH_SHORT).show();
        } else {
            // Check permissions again before starting
            if (!isAccessibilityServiceEnabled()) {
                showAccessibilityServiceDialog();
                return;
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_AUDIO_PERMISSIONS);
                return;
            }

            // For Android Q (10) and above, request MediaProjection
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                if (mediaProjectionManager != null) {
                    mediaProjectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent());
                } else {
                    Log.e(TAG, "MediaProjectionManager is null.");
                    Toast.makeText(this, "Media Projection not available.", Toast.LENGTH_SHORT).show();
                    // Fallback to microphone capture if MediaProjection is not available
                    systemWideAudioService.startSystemWideCapture();
                }
            } else {
                // For older Android versions, directly start microphone capture
                systemWideAudioService.startSystemWideCapture();
            }
            Toast.makeText(this, "Voice Changer Started", Toast.LENGTH_SHORT).show();
        }
        updateUiState();
    }

    private void updateUiState() {
        runOnUiThread(() -> {
            if (systemWideAudioService != null && systemWideAudioService.isCapturing()) {
                startStopButton.setText("Stop Voice Changer");
                statusTextView.setText("Status: Active");
                statusTextView.setTextColor(ContextCompat.getColor(this, R.color.green_500));

                // Update performance metrics
                if (systemWideAudioService.voiceProcessor != null) {
                    latencyTextView.setText(String.format("Avg Latency: %d ms", systemWideAudioService.voiceProcessor.getAverageLatency()));
                    successRateTextView.setText(String.format("Success Rate: %.1f %%", systemWideAudioService.voiceProcessor.getSuccessRate()));
                }
            } else {
                startStopButton.setText("Start Voice Changer");
                statusTextView.setText("Status: Inactive");
                statusTextView.setTextColor(ContextCompat.getColor(this, R.color.red_500));
                latencyTextView.setText("Avg Latency: N/A");
                successRateTextView.setText("Success Rate: N/A");
            }
        });
    }

    @Override
    public void onPerformanceUpdate(long avgLatency, float successRate, long totalChunks) {
        runOnUiThread(() -> {
            latencyTextView.setText(String.format("Avg Latency: %d ms", avgLatency));
            successRateTextView.setText(String.format("Success Rate: %.1f %%", successRate));
        });
    }

    @Override
    public void onError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(MainActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
            Log.e(TAG, "App Error: " + message);
            statusTextView.setText("Status: Error");
            statusTextView.setTextColor(ContextCompat.getColor(this, R.color.red_500));
            updateUiState(); // Revert UI to inactive state
        });
    }
    
    // VoiceCloningEngine.VoiceCloningListener methods
    @Override
    public void onVoiceCloned(String voiceId, float similarity) {
        runOnUiThread(() -> {
            Toast.makeText(MainActivity.this, "Voice cloned successfully! Similarity: " + (similarity * 100) + "%", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Voice cloned: " + voiceId + " with similarity: " + similarity);
        });
    }
    
    @Override
    public void onPerformanceUpdate(long avgLatency, long totalChunks) {
        runOnUiThread(() -> {
            Log.d(TAG, "Voice cloning performance - Avg latency: " + avgLatency + " ms, Total chunks: " + totalChunks);
        });
    }
    
    // LiveCallOptimizer.LiveCallListener methods
    @Override
    public void onLatencyUpdate(long currentLatency, long maxLatency) {
        runOnUiThread(() -> {
            latencyTextView.setText(String.format("Latency: %d ms (Max: %d ms)", currentLatency, maxLatency));
        });
    }
    
    @Override
    public void onAudioLevelChanged(float inputLevel, float outputLevel) {
        // Update audio level indicators if needed
        Log.d(TAG, "Audio levels - Input: " + inputLevel + " dB, Output: " + outputLevel + " dB");
    }
    
    // AI Voice Analysis Methods
    private void startAIVoiceAnalysis() {
        if (aiVoiceAnalyzer == null) {
            Toast.makeText(this, "AI Voice Analyzer not initialized", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (aiVoiceAnalyzer.isRecording()) {
            // Stop recording and analyze
            aiVoiceAnalyzer.stopRecordingAndAnalyze();
            Toast.makeText(this, "AI Analysis Complete", Toast.LENGTH_SHORT).show();
        } else {
            // Start recording
            aiVoiceAnalyzer.startRecording();
            Toast.makeText(this, "AI Analysis Started", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void generateAITemplate() {
        if (aiVoiceAnalyzer == null) {
            Toast.makeText(this, "AI Voice Analyzer not initialized", Toast.LENGTH_SHORT).show();
            return;
        }
        
        GeminiAIService.VoiceAnalysisResult lastAnalysis = aiVoiceAnalyzer.getLastAnalysisResult();
        if (lastAnalysis == null) {
            Toast.makeText(this, "Please analyze a voice first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show dialog to select target voice
        showTargetVoiceDialog();
    }
    
    private void showTargetVoiceDialog() {
        String[] targetVoices = {
            "فتاة سعودية شابة",
            "امرأة سعودية ناضجة", 
            "رجل سعودي عميق",
            "طفل سعودي",
            "مراهقة سعودية",
            "عجوز سعودية"
        };
        
        new AlertDialog.Builder(this)
                .setTitle("Select Target Voice")
                .setItems(targetVoices, (dialog, which) -> {
                    String targetVoice = targetVoices[which];
                    generateTemplateForTarget(targetVoice);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void generateTemplateForTarget(String targetVoice) {
        if (aiVoiceAnalyzer != null) {
            aiVoiceAnalyzer.generateIntelligentTemplate(targetVoice);
            Toast.makeText(this, "Generating AI template for: " + targetVoice, Toast.LENGTH_SHORT).show();
        }
    }
}