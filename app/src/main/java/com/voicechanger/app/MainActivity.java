package com.voicechanger.app;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SystemWideAudioService.VoiceProcessorListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1001;
    private static final int REQUEST_CODE_AUDIO_PERMISSIONS = 1002;
    private static final String PREFS_NAME = "VoiceChangerPrefs";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_VOICE_MODEL = "voice_model";
    private static final String KEY_PROCESSING_MODE = "processing_mode";
    private static final String KEY_AI_ANALYSIS = "ai_analysis";
    private static final String KEY_VOICE_CLONING = "voice_cloning";
    private static final String KEY_SMART_PROCESSING = "smart_processing";

    // UI Components
    private Button startStopButton;
    private TextView statusTextView;
    private TextView latencyTextView;
    private TextView successRateTextView;
    private Spinner voiceModelSpinner;
    private Spinner processingModeSpinner;
    private EditText apiKeyEditText;
    private ImageView statusIcon;
    private Switch aiAnalysisSwitch;
    private Switch voiceCloningSwitch;
    private Switch smartProcessingSwitch;
    private Button settingsButton;
    private Button helpButton;

    // Service and Logic
    private SystemWideAudioService systemWideAudioService;
    private boolean isServiceBound = false;
    private SharedPreferences preferences;
    private Handler uiHandler;
    private PerformanceMonitor performanceMonitor;

    // Voice Models
    private List<VoiceModel> voiceModels;
    private List<ProcessingMode> processingModes;
    private VoiceTemplateManager voiceTemplateManager;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected: " + name.getClassName());
            isServiceBound = true;
            if (SystemWideAudioService.getInstance() != null) {
                systemWideAudioService = SystemWideAudioService.getInstance();
                systemWideAudioService.setVoiceProcessorListener(MainActivity.this);
                updateUiState();
                startPerformanceMonitoring();
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

        initializeComponents();
        setupVoiceModels();
        setupProcessingModes();
        initViews();
        loadPreferences();
        setupSpinners();
        checkAndRequestPermissions();
        startPerformanceMonitoring();
    }

    private void initializeComponents() {
        preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        uiHandler = new Handler(Looper.getMainLooper());
        performanceMonitor = new PerformanceMonitor();
    }

    private void setupVoiceModels() {
        // Initialize voice template manager
        voiceTemplateManager = new VoiceTemplateManager(this);
        
        // Initialize voice models from templates
        voiceModels = new ArrayList<>();
        if (voiceTemplateManager != null) {
            List<VoiceTemplateManager.VoiceTemplate> templates = voiceTemplateManager.getAllTemplates();
            for (VoiceTemplateManager.VoiceTemplate template : templates) {
                voiceModels.add(new VoiceModel(template.id, template.name, template.description));
            }
        } else {
            // Fallback to default voice models if template manager fails
            voiceModels.add(new VoiceModel("default", "Default Voice", "A balanced voice for general use"));
            voiceModels.add(new VoiceModel("male", "Male Voice", "Deep, masculine voice"));
            voiceModels.add(new VoiceModel("female", "Female Voice", "Clear, feminine voice"));
            voiceModels.add(new VoiceModel("child", "Child Voice", "Young, energetic voice"));
            voiceModels.add(new VoiceModel("elderly", "Elderly Voice", "Mature, wise voice"));
            voiceModels.add(new VoiceModel("robot", "Robot Voice", "Mechanical, futuristic voice"));
            voiceModels.add(new VoiceModel("whisper", "Whisper Voice", "Soft, intimate voice"));
        }
    }

    private void setupProcessingModes() {
        processingModes = new ArrayList<>();
        processingModes.add(new ProcessingMode("realtime", "Real-time", "Low latency processing"));
        processingModes.add(new ProcessingMode("batch", "Batch", "High quality processing"));
        processingModes.add(new ProcessingMode("hybrid", "Hybrid", "Balanced processing"));
        processingModes.add(new ProcessingMode("ai", "AI Enhanced", "AI-powered processing"));
    }

    private void initViews() {
        startStopButton = findViewById(R.id.startStopButton);
        statusTextView = findViewById(R.id.statusTextView);
        latencyTextView = findViewById(R.id.latencyTextView);
        successRateTextView = findViewById(R.id.successRateTextView);
        voiceModelSpinner = findViewById(R.id.voiceModelSpinner);
        processingModeSpinner = findViewById(R.id.processingModeSpinner);
        apiKeyEditText = findViewById(R.id.apiKeyEditText);
        statusIcon = findViewById(R.id.statusIcon);
        aiAnalysisSwitch = findViewById(R.id.aiAnalysisSwitch);
        voiceCloningSwitch = findViewById(R.id.voiceCloningSwitch);
        smartProcessingSwitch = findViewById(R.id.smartProcessingSwitch);
        settingsButton = findViewById(R.id.settingsButton);
        helpButton = findViewById(R.id.helpButton);

        // Set click listeners
        startStopButton.setOnClickListener(v -> toggleVoiceChanger());
        settingsButton.setOnClickListener(v -> showSettingsDialog());
        helpButton.setOnClickListener(v -> showHelpDialog());

        // Set switch listeners
        aiAnalysisSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference(KEY_AI_ANALYSIS, isChecked);
            if (systemWideAudioService != null) {
                systemWideAudioService.setAIAnalysisEnabled(isChecked);
            }
        });

        voiceCloningSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference(KEY_VOICE_CLONING, isChecked);
            if (systemWideAudioService != null) {
                systemWideAudioService.setVoiceCloningEnabled(isChecked);
            }
        });

        smartProcessingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference(KEY_SMART_PROCESSING, isChecked);
            if (systemWideAudioService != null) {
                systemWideAudioService.setSmartProcessingEnabled(isChecked);
            }
        });
    }

    private void setupSpinners() {
        // Voice Model Spinner
        List<String> voiceModelNames = new ArrayList<>();
        if (voiceModels != null && !voiceModels.isEmpty()) {
            for (VoiceModel model : voiceModels) {
                voiceModelNames.add(model.getName());
            }
        } else {
            // Fallback if voiceModels is null or empty
            voiceModelNames.add("Default Voice");
            voiceModelNames.add("Male Voice");
            voiceModelNames.add("Female Voice");
            voiceModelNames.add("Child Voice");
            voiceModelNames.add("Elderly Voice");
            voiceModelNames.add("Robot Voice");
            voiceModelNames.add("Whisper Voice");
        }
        ArrayAdapter<String> voiceModelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, voiceModelNames);
        voiceModelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        voiceModelSpinner.setAdapter(voiceModelAdapter);

        voiceModelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (voiceModels != null && position < voiceModels.size()) {
                    VoiceModel selectedModel = voiceModels.get(position);
                    savePreference(KEY_VOICE_MODEL, selectedModel.getId());
                    if (systemWideAudioService != null) {
                        systemWideAudioService.setVoiceModel(selectedModel.getId());
                    }
                    showToast("Voice model set to: " + selectedModel.getName());
                } else {
                    // Fallback for default voice models
                    String[] defaultModels = {"default", "male", "female", "child", "elderly", "robot", "whisper"};
                    if (position < defaultModels.length) {
                        savePreference(KEY_VOICE_MODEL, defaultModels[position]);
                        if (systemWideAudioService != null) {
                            systemWideAudioService.setVoiceModel(defaultModels[position]);
                        }
                        showToast("Voice model set to: " + defaultModels[position]);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Processing Mode Spinner
        List<String> processingModeNames = new ArrayList<>();
        for (ProcessingMode mode : processingModes) {
            processingModeNames.add(mode.getName());
        }
        ArrayAdapter<String> processingModeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, processingModeNames);
        processingModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        processingModeSpinner.setAdapter(processingModeAdapter);

        processingModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ProcessingMode selectedMode = processingModes.get(position);
                savePreference(KEY_PROCESSING_MODE, selectedMode.getId());
                if (systemWideAudioService != null) {
                    systemWideAudioService.setProcessingMode(selectedMode.getId());
                }
                showToast("Processing mode set to: " + selectedMode.getName());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadPreferences() {
        // Load API key
        String apiKey = preferences.getString(KEY_API_KEY, "");
        apiKeyEditText.setText(apiKey);

        // Load voice model
        String voiceModelId = preferences.getString(KEY_VOICE_MODEL, "default");
        if (voiceModels != null && !voiceModels.isEmpty()) {
            for (int i = 0; i < voiceModels.size(); i++) {
                if (voiceModels.get(i).getId().equals(voiceModelId)) {
                    voiceModelSpinner.setSelection(i);
                    break;
                }
            }
        }

        // Load processing mode
        String processingModeId = preferences.getString(KEY_PROCESSING_MODE, "realtime");
        for (int i = 0; i < processingModes.size(); i++) {
            if (processingModes.get(i).getId().equals(processingModeId)) {
                processingModeSpinner.setSelection(i);
                break;
            }
        }

        // Load AI settings
        aiAnalysisSwitch.setChecked(preferences.getBoolean(KEY_AI_ANALYSIS, true));
        voiceCloningSwitch.setChecked(preferences.getBoolean(KEY_VOICE_CLONING, false));
        smartProcessingSwitch.setChecked(preferences.getBoolean(KEY_SMART_PROCESSING, true));
    }

    private void savePreference(String key, String value) {
        preferences.edit().putString(key, value).apply();
    }

    private void savePreference(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
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
                .setTitle(getString(R.string.permission_accessibility_title))
                .setMessage(getString(R.string.permission_accessibility_message))
                .setPositiveButton(getString(R.string.go_to_settings), (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_AUDIO_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast(getString(R.string.audio_permission_granted));
            } else {
                showToast(getString(R.string.audio_permission_denied));
            }
        } else if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast(getString(R.string.notification_permission_granted));
            } else {
                showToast(getString(R.string.notification_permission_denied));
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
            showToast(getString(R.string.service_not_ready));
            return;
        }

        if (systemWideAudioService.isCapturing()) {
            systemWideAudioService.stopSystemWideCapture();
            showToast(getString(R.string.voice_changer_stopped));
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

            // Save API key
            String apiKey = apiKeyEditText.getText().toString().trim();
            if (!apiKey.isEmpty()) {
                savePreference(KEY_API_KEY, apiKey);
                systemWideAudioService.setApiKey(apiKey);
            }

            systemWideAudioService.startSystemWideCapture();
            showToast(getString(R.string.voice_changer_started));
        }
        updateUiState();
    }

    private void updateUiState() {
        uiHandler.post(() -> {
            if (systemWideAudioService != null && systemWideAudioService.isCapturing()) {
                startStopButton.setText(getString(R.string.stop_voice_changer));
                statusTextView.setText(getString(R.string.status_active));
                statusTextView.setTextColor(ContextCompat.getColor(this, R.color.status_active));
                statusIcon.setImageResource(R.drawable.ic_status_active);
            } else {
                startStopButton.setText(getString(R.string.start_voice_changer));
                statusTextView.setText(getString(R.string.status_inactive));
                statusTextView.setTextColor(ContextCompat.getColor(this, R.color.status_inactive));
                statusIcon.setImageResource(R.drawable.ic_status_inactive);
            }
        });
    }

    private void startPerformanceMonitoring() {
        uiHandler.postDelayed(new Runnable() {
    @Override
            public void run() {
                if (systemWideAudioService != null && systemWideAudioService.isCapturing()) {
                    performanceMonitor.updateMetrics();
                    updatePerformanceUI();
                }
                uiHandler.postDelayed(this, 1000); // Update every second
            }
        }, 1000);
    }

    private void updatePerformanceUI() {
        uiHandler.post(() -> {
            latencyTextView.setText(String.format("Latency: %d ms", performanceMonitor.getAverageLatency()));
            successRateTextView.setText(String.format("Success: %.1f%%", performanceMonitor.getSuccessRate()));
        });
    }

    private void showSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.settings_title))
                .setMessage("Advanced settings and configuration options will be available here.")
                .setPositiveButton(getString(R.string.ok), null)
                .show();
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.help_title))
                .setMessage("Help and support information will be available here.")
                .setPositiveButton(getString(R.string.ok), null)
                .show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // VoiceProcessorListener implementation
    @Override
    public void onPerformanceUpdate(long avgLatency, float successRate, long totalChunks) {
        uiHandler.post(() -> {
            performanceMonitor.setLatency(avgLatency);
            performanceMonitor.setSuccessRate(successRate);
            updatePerformanceUI();
        });
    }
    
    @Override
    public void onError(String message) {
        uiHandler.post(() -> {
            showToast("Error: " + message);
            Log.e(TAG, "Voice processing error: " + message);
            statusTextView.setText(getString(R.string.status_error));
            statusTextView.setTextColor(ContextCompat.getColor(this, R.color.status_inactive));
            statusIcon.setImageResource(R.drawable.ic_status_inactive);
        });
    }

    // Inner classes for data models
    private static class VoiceModel {
        private final String id;
        private final String name;
        private final String description;

        public VoiceModel(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
    }

    private static class ProcessingMode {
        private final String id;
        private final String name;
        private final String description;

        public ProcessingMode(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
    }

    private static class PerformanceMonitor {
        private long averageLatency = 0;
        private float successRate = 0.0f;

        public void setLatency(long latency) { this.averageLatency = latency; }
        public void setSuccessRate(float rate) { this.successRate = rate; }
        public long getAverageLatency() { return averageLatency; }
        public float getSuccessRate() { return successRate; }

        public void updateMetrics() {
            // Update performance metrics based on current system state
        }
    }
}