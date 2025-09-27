package com.voicechanger.app;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Client for interacting with the ElevenLabs API to manage voices and perform voice conversion.
 */
public class ElevenLabsApiClient {
    private static final String TAG = "ElevenLabsApiClient";
    private static final String API_BASE_URL = "https://api.elevenlabs.io/v1";
    
    private OkHttpClient httpClient;
    private Gson gson;
    private String apiKey;
    
    public ElevenLabsApiClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    /**
     * Fetches available voices from ElevenLabs API
     */
    public void getVoices(VoicesCallback callback) {
        if (apiKey == null || apiKey.isEmpty()) {
            callback.onError("API key not set");
            return;
        }
        
        Request request = new Request.Builder()
                .url(API_BASE_URL + "/voices")
                .addHeader("Accept", "application/json")
                .addHeader("xi-api-key", apiKey)
                .get()
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch voices", e);
                callback.onError("Network error: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        VoicesResponse voicesResponse = gson.fromJson(responseBody, VoicesResponse.class);
                        callback.onSuccess(voicesResponse.voices);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse voices response", e);
                        callback.onError("Failed to parse response: " + e.getMessage());
                    }
                } else {
                    Log.e(TAG, "API error: " + response.code() + " " + response.message());
                    callback.onError("API error: " + response.code() + " " + response.message());
                }
                response.close();
            }
        });
    }
    
    /**
     * Finds a voice that matches the description "20-year-old Saudi girl with warm voice"
     */
    public void findSaudiGirlVoice(VoiceSelectionCallback callback) {
        getVoices(new VoicesCallback() {
            @Override
            public void onSuccess(List<Voice> voices) {
                Voice selectedVoice = null;
                
                // Look for voices that might match our criteria
                for (Voice voice : voices) {
                    String name = voice.name.toLowerCase();
                    String description = voice.description != null ? voice.description.toLowerCase() : "";
                    
                    // Look for female voices with warm characteristics
                    if ((name.contains("female") || name.contains("woman") || name.contains("girl")) &&
                        (description.contains("warm") || description.contains("soft") || 
                         description.contains("gentle") || description.contains("young"))) {
                        selectedVoice = voice;
                        break;
                    }
                }
                
                // If no perfect match, look for any female voice
                if (selectedVoice == null) {
                    for (Voice voice : voices) {
                        String name = voice.name.toLowerCase();
                        if (name.contains("female") || name.contains("woman") || name.contains("girl")) {
                            selectedVoice = voice;
                            break;
                        }
                    }
                }
                
                // If still no match, use the first available voice
                if (selectedVoice == null && !voices.isEmpty()) {
                    selectedVoice = voices.get(0);
                    Log.w(TAG, "No suitable female voice found, using first available: " + selectedVoice.name);
                }
                
                if (selectedVoice != null) {
                    callback.onVoiceSelected(selectedVoice);
                } else {
                    callback.onError("No voices available");
                }
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    // Data classes for API responses
    public static class VoicesResponse {
        @SerializedName("voices")
        public List<Voice> voices;
    }
    
    public static class Voice {
        @SerializedName("voice_id")
        public String voiceId;
        
        @SerializedName("name")
        public String name;
        
        @SerializedName("description")
        public String description;
        
        @SerializedName("category")
        public String category;
        
        @SerializedName("labels")
        public VoiceLabels labels;
        
        @SerializedName("preview_url")
        public String previewUrl;
        
        @Override
        public String toString() {
            return name + " (" + (description != null ? description : "No description") + ")";
        }
    }
    
    public static class VoiceLabels {
        @SerializedName("gender")
        public String gender;
        
        @SerializedName("age")
        public String age;
        
        @SerializedName("accent")
        public String accent;
        
        @SerializedName("description")
        public String description;
    }
    
    // Callback interfaces
    public interface VoicesCallback {
        void onSuccess(List<Voice> voices);
        void onError(String error);
    }
    
    public interface VoiceSelectionCallback {
        void onVoiceSelected(Voice voice);
        void onError(String error);
    }
}

