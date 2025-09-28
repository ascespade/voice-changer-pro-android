package com.voicechanger.app;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Gemini AI Service for intelligent voice analysis and template generation
 * Uses Gemini Pro 2.5 for advanced voice characteristics analysis
 */
public class GeminiAIService {
    private static final String TAG = "GeminiAIService";
    
    // Gemini API configuration
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent";
    private static final String API_KEY = "AIzaSyBqDQxAQJd5P03nrD2oGcorbuY8cyRwyjI";
    
    private Context context;
    private OkHttpClient httpClient;
    private ExecutorService executorService;
    private Gson gson;
    
    public interface VoiceAnalysisListener {
        void onVoiceAnalysisComplete(VoiceAnalysisResult result);
        void onTemplateGenerated(VoiceTemplate template);
        void onError(String message);
    }
    
    public static class VoiceAnalysisResult {
        public String gender;
        public int estimatedAge;
        public String accent;
        public String emotionalTone;
        public float pitchLevel; // 0.0 to 2.0
        public float speakingRate; // 0.5 to 2.0
        public float clarity; // 0.0 to 1.0
        public float warmth; // 0.0 to 1.0
        public float breathiness; // 0.0 to 1.0
        public String voiceType; // "child", "young_adult", "middle_aged", "elderly"
        public String personality; // "energetic", "calm", "authoritative", "friendly"
        public float confidence; // 0.0 to 1.0
        public String recommendations;
        
        public VoiceAnalysisResult() {
            this.confidence = 0.0f;
        }
    }
    
    public static class VoiceTemplate {
        public String templateId;
        public String name;
        public String description;
        public String category;
        public Map<String, Float> parameters;
        public String aiGeneratedPrompt;
        public float similarityScore;
        
        public VoiceTemplate() {
            this.parameters = new HashMap<>();
        }
    }
    
    public GeminiAIService(Context context) {
        this.context = context;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.executorService = Executors.newFixedThreadPool(2);
        this.gson = new Gson();
        
        Log.d(TAG, "Gemini AI Service initialized");
    }
    
    /**
     * Analyze voice characteristics using Gemini AI
     */
    public void analyzeVoiceCharacteristics(byte[] audioData, String audioFormat, VoiceAnalysisListener listener) {
        executorService.execute(() -> {
            try {
                // Convert audio to base64 for Gemini
                String audioBase64 = android.util.Base64.encodeToString(audioData, android.util.Base64.DEFAULT);
                
                // Create analysis prompt
                String prompt = createVoiceAnalysisPrompt(audioFormat);
                
                // Prepare request body
                JsonObject requestBody = new JsonObject();
                JsonObject contents = new JsonObject();
                JsonObject parts = new JsonObject();
                
                // Add text prompt
                JsonObject textPart = new JsonObject();
                textPart.addProperty("text", prompt);
                
                // Add audio data
                JsonObject audioPart = new JsonObject();
                JsonObject inlineData = new JsonObject();
                inlineData.addProperty("mime_type", "audio/" + audioFormat);
                inlineData.addProperty("data", audioBase64);
                audioPart.add("inline_data", inlineData);
                
                JsonObject[] partsArray = {textPart, audioPart};
                contents.add("parts", gson.toJsonTree(partsArray));
                
                JsonObject[] contentsArray = {contents};
                requestBody.add("contents", gson.toJsonTree(contentsArray));
                
                // Add generation config for structured output
                JsonObject generationConfig = new JsonObject();
                generationConfig.addProperty("temperature", 0.1);
                generationConfig.addProperty("maxOutputTokens", 2048);
                requestBody.add("generationConfig", generationConfig);
                
                // Make API request
                RequestBody body = RequestBody.create(
                    requestBody.toString(),
                    MediaType.parse("application/json")
                );
                
                Request request = new Request.Builder()
                        .url(GEMINI_API_URL + "?key=" + API_KEY)
                        .post(body)
                        .build();
                
                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Gemini API call failed", e);
                        if (listener != null) {
                            listener.onError("Voice analysis failed: " + e.getMessage());
                        }
                    }
                    
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try {
                            if (response.isSuccessful() && response.body() != null) {
                                String responseBody = response.body().string();
                                VoiceAnalysisResult result = parseVoiceAnalysisResponse(responseBody);
                                
                                if (listener != null) {
                                    listener.onVoiceAnalysisComplete(result);
                                }
                                
                            } else {
                                Log.e(TAG, "Gemini API response error: " + response.code() + " " + response.message());
                                if (listener != null) {
                                    listener.onError("API response error: " + response.message());
                                }
                            }
                        } finally {
                            response.close();
                        }
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error analyzing voice characteristics", e);
                if (listener != null) {
                    listener.onError("Voice analysis error: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Generate intelligent voice template based on analysis
     */
    public void generateVoiceTemplate(VoiceAnalysisResult analysis, String targetVoice, VoiceAnalysisListener listener) {
        executorService.execute(() -> {
            try {
                String prompt = createTemplateGenerationPrompt(analysis, targetVoice);
                
                JsonObject requestBody = new JsonObject();
                JsonObject contents = new JsonObject();
                JsonObject parts = new JsonObject();
                parts.addProperty("text", prompt);
                
                JsonObject[] partsArray = {parts};
                contents.add("parts", gson.toJsonTree(partsArray));
                
                JsonObject[] contentsArray = {contents};
                requestBody.add("contents", gson.toJsonTree(contentsArray));
                
                // Add generation config
                JsonObject generationConfig = new JsonObject();
                generationConfig.addProperty("temperature", 0.3);
                generationConfig.addProperty("maxOutputTokens", 1024);
                requestBody.add("generationConfig", generationConfig);
                
                RequestBody body = RequestBody.create(
                    requestBody.toString(),
                    MediaType.parse("application/json")
                );
                
                Request request = new Request.Builder()
                        .url(GEMINI_API_URL + "?key=" + API_KEY)
                        .post(body)
                        .build();
                
                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Template generation failed", e);
                        if (listener != null) {
                            listener.onError("Template generation failed: " + e.getMessage());
                        }
                    }
                    
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try {
                            if (response.isSuccessful() && response.body() != null) {
                                String responseBody = response.body().string();
                                VoiceTemplate template = parseTemplateGenerationResponse(responseBody, analysis);
                                
                                if (listener != null) {
                                    listener.onTemplateGenerated(template);
                                }
                                
                            } else {
                                Log.e(TAG, "Template generation response error: " + response.code());
                                if (listener != null) {
                                    listener.onError("Template generation failed");
                                }
                            }
                        } finally {
                            response.close();
                        }
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error generating voice template", e);
                if (listener != null) {
                    listener.onError("Template generation error: " + e.getMessage());
                }
            }
        });
    }
    
    private String createVoiceAnalysisPrompt(String audioFormat) {
        return "You are an expert voice analysis AI. Analyze the provided audio sample and return a detailed JSON response with the following structure:\n\n" +
                "{\n" +
                "  \"gender\": \"male/female/neutral\",\n" +
                "  \"estimatedAge\": 25,\n" +
                "  \"accent\": \"description of accent\",\n" +
                "  \"emotionalTone\": \"calm/excited/serious/friendly/etc\",\n" +
                "  \"pitchLevel\": 1.2,\n" +
                "  \"speakingRate\": 1.0,\n" +
                "  \"clarity\": 0.9,\n" +
                "  \"warmth\": 0.7,\n" +
                "  \"breathiness\": 0.2,\n" +
                "  \"voiceType\": \"child/young_adult/middle_aged/elderly\",\n" +
                "  \"personality\": \"energetic/calm/authoritative/friendly\",\n" +
                "  \"confidence\": 0.95,\n" +
                "  \"recommendations\": \"specific recommendations for voice cloning\"\n" +
                "}\n\n" +
                "Please analyze the voice characteristics carefully and provide accurate values. " +
                "The pitchLevel should be between 0.5 (very low) and 2.0 (very high), " +
                "speakingRate between 0.5 (slow) and 2.0 (fast), " +
                "and other parameters between 0.0 and 1.0. " +
                "Be specific about the accent and provide detailed recommendations for voice cloning.";
    }
    
    private String createTemplateGenerationPrompt(VoiceAnalysisResult analysis, String targetVoice) {
        return "Based on the voice analysis results, generate an intelligent voice template for transforming the voice to: " + targetVoice + "\n\n" +
                "Analysis Results:\n" +
                "- Gender: " + analysis.gender + "\n" +
                "- Age: " + analysis.estimatedAge + "\n" +
                "- Accent: " + analysis.accent + "\n" +
                "- Emotional Tone: " + analysis.emotionalTone + "\n" +
                "- Current Pitch: " + analysis.pitchLevel + "\n" +
                "- Current Speaking Rate: " + analysis.speakingRate + "\n" +
                "- Current Clarity: " + analysis.clarity + "\n" +
                "- Current Warmth: " + analysis.warmth + "\n" +
                "- Current Breathiness: " + analysis.breathiness + "\n\n" +
                "Generate a JSON template with optimized parameters for the target voice:\n\n" +
                "{\n" +
                "  \"templateId\": \"ai_generated_template\",\n" +
                "  \"name\": \"AI Generated Template\",\n" +
                "  \"description\": \"AI-optimized template for voice transformation\",\n" +
                "  \"category\": \"AI Generated\",\n" +
                "  \"parameters\": {\n" +
                "    \"pitch_shift\": 1.0,\n" +
                "    \"formant_shift\": 1.0,\n" +
                "    \"warmth\": 0.5,\n" +
                "    \"clarity\": 0.8,\n" +
                "    \"breathiness\": 0.3,\n" +
                "    \"speaking_rate\": 1.0,\n" +
                "    \"emotional_tone\": 0.5\n" +
                "  },\n" +
                "  \"aiGeneratedPrompt\": \"AI-generated description\",\n" +
                "  \"similarityScore\": 0.95\n" +
                "}\n\n" +
                "Calculate the optimal parameters to transform the current voice to the target voice. " +
                "Consider the current voice characteristics and provide realistic transformation parameters.";
    }
    
    private VoiceAnalysisResult parseVoiceAnalysisResponse(String responseBody) {
        try {
            JsonObject response = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonObject candidates = response.getAsJsonArray("candidates").get(0).getAsJsonObject();
            JsonObject content = candidates.getAsJsonObject("content");
            JsonObject parts = content.getAsJsonArray("parts").get(0).getAsJsonObject();
            String text = parts.get("text").getAsString();
            
            // Extract JSON from the response text
            String jsonText = extractJsonFromText(text);
            return gson.fromJson(jsonText, VoiceAnalysisResult.class);
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing voice analysis response", e);
            return createDefaultAnalysisResult();
        }
    }
    
    private VoiceTemplate parseTemplateGenerationResponse(String responseBody, VoiceAnalysisResult analysis) {
        try {
            JsonObject response = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonObject candidates = response.getAsJsonArray("candidates").get(0).getAsJsonObject();
            JsonObject content = candidates.getAsJsonObject("content");
            JsonObject parts = content.getAsJsonArray("parts").get(0).getAsJsonObject();
            String text = parts.get("text").getAsString();
            
            // Extract JSON from the response text
            String jsonText = extractJsonFromText(text);
            return gson.fromJson(jsonText, VoiceTemplate.class);
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing template generation response", e);
            return createDefaultTemplate(analysis);
        }
    }
    
    private String extractJsonFromText(String text) {
        // Find JSON object in the response text
        int startIndex = text.indexOf("{");
        int endIndex = text.lastIndexOf("}");
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return text.substring(startIndex, endIndex + 1);
        }
        
        return text;
    }
    
    private VoiceAnalysisResult createDefaultAnalysisResult() {
        VoiceAnalysisResult result = new VoiceAnalysisResult();
        result.gender = "neutral";
        result.estimatedAge = 25;
        result.accent = "neutral";
        result.emotionalTone = "neutral";
        result.pitchLevel = 1.0f;
        result.speakingRate = 1.0f;
        result.clarity = 0.8f;
        result.warmth = 0.5f;
        result.breathiness = 0.2f;
        result.voiceType = "young_adult";
        result.personality = "neutral";
        result.confidence = 0.5f;
        result.recommendations = "Default analysis - manual adjustment recommended";
        return result;
    }
    
    private VoiceTemplate createDefaultTemplate(VoiceAnalysisResult analysis) {
        VoiceTemplate template = new VoiceTemplate();
        template.templateId = "ai_default_" + System.currentTimeMillis();
        template.name = "AI Generated Template";
        template.description = "AI-generated template based on voice analysis";
        template.category = "AI Generated";
        template.aiGeneratedPrompt = "Generated based on voice analysis";
        template.similarityScore = 0.8f;
        
        // Set default parameters based on analysis
        template.parameters.put("pitch_shift", analysis.pitchLevel);
        template.parameters.put("formant_shift", 1.0f);
        template.parameters.put("warmth", analysis.warmth);
        template.parameters.put("clarity", analysis.clarity);
        template.parameters.put("breathiness", analysis.breathiness);
        template.parameters.put("speaking_rate", analysis.speakingRate);
        template.parameters.put("emotional_tone", 0.5f);
        
        return template;
    }
    
    public void release() {
        if (executorService != null) {
            executorService.shutdown();
        }
        Log.d(TAG, "Gemini AI Service released");
    }
}
