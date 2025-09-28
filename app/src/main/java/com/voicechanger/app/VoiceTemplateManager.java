package com.voicechanger.app;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Voice Template Manager - Manages pre-built voice templates and custom voice cloning
 */
public class VoiceTemplateManager {
    private static final String TAG = "VoiceTemplateManager";
    
    private Context context;
    private Map<String, VoiceTemplate> voiceTemplates;
    private Map<String, VoiceProfile> customVoices;
    
    public static class VoiceTemplate {
        public String id;
        public String name;
        public String description;
        public String category;
        public String language;
        public int iconResource;
        public Map<String, Float> parameters;
        public boolean isPremium;
        
        public VoiceTemplate(String id, String name, String description, String category, String language) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.category = category;
            this.language = language;
            this.parameters = new HashMap<>();
            this.isPremium = false;
        }
    }
    
    public static class VoiceProfile {
        public String id;
        public String name;
        public String description;
        public String category;
        public long createdAt;
        public float similarity;
        public boolean isCustom;
        
        public VoiceProfile(String id, String name, String description, String category) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.category = category;
            this.createdAt = System.currentTimeMillis();
            this.isCustom = true;
        }
    }
    
    public VoiceTemplateManager(Context context) {
        this.context = context;
        this.voiceTemplates = new HashMap<>();
        this.customVoices = new HashMap<>();
        
        initializeVoiceTemplates();
        Log.d(TAG, "VoiceTemplateManager initialized");
    }
    
    private void initializeVoiceTemplates() {
        // Saudi Female Voices
        createSaudiFemaleTemplates();
        
        // Saudi Male Voices
        createSaudiMaleTemplates();
        
        // Age-based Templates
        createAgeBasedTemplates();
        
        // Special Effects
        createSpecialEffectTemplates();
        
        Log.d(TAG, "Initialized " + voiceTemplates.size() + " voice templates");
    }
    
    private void createSaudiFemaleTemplates() {
        // فتاة سعودية شابة
        VoiceTemplate saudiGirl20 = new VoiceTemplate(
            "saudi_girl_20", 
            "فتاة سعودية 20 سنة", 
            "صوت فتاة سعودية شابة دافئ ومميز", 
            "Female", 
            "Arabic"
        );
        saudiGirl20.parameters.put("pitch_shift", 1.3f);
        saudiGirl20.parameters.put("formant_shift", 1.2f);
        saudiGirl20.parameters.put("warmth", 0.4f);
        saudiGirl20.parameters.put("clarity", 0.9f);
        saudiGirl20.parameters.put("breathiness", 0.2f);
        saudiGirl20.parameters.put("speaking_rate", 1.1f);
        saudiGirl20.parameters.put("emotional_tone", 0.7f); // Happy
        voiceTemplates.put(saudiGirl20.id, saudiGirl20);
        
        // امرأة سعودية ناضجة
        VoiceTemplate saudiWoman30 = new VoiceTemplate(
            "saudi_woman_30", 
            "امرأة سعودية 30 سنة", 
            "صوت امرأة سعودية ناضجة وواثقة", 
            "Female", 
            "Arabic"
        );
        saudiWoman30.parameters.put("pitch_shift", 1.1f);
        saudiWoman30.parameters.put("formant_shift", 1.0f);
        saudiWoman30.parameters.put("warmth", 0.6f);
        saudiWoman30.parameters.put("clarity", 0.95f);
        saudiWoman30.parameters.put("breathiness", 0.1f);
        saudiWoman30.parameters.put("speaking_rate", 0.95f);
        saudiWoman30.parameters.put("emotional_tone", 0.5f); // Neutral
        voiceTemplates.put(saudiWoman30.id, saudiWoman30);
        
        // عجوز سعودية
        VoiceTemplate saudiElderly60 = new VoiceTemplate(
            "saudi_elderly_60", 
            "عجوز سعودية 60 سنة", 
            "صوت عجوز سعودية حكيمة ومريح", 
            "Female", 
            "Arabic"
        );
        saudiElderly60.parameters.put("pitch_shift", 0.9f);
        saudiElderly60.parameters.put("formant_shift", 0.8f);
        saudiElderly60.parameters.put("warmth", 0.8f);
        saudiElderly60.parameters.put("clarity", 0.7f);
        saudiElderly60.parameters.put("breathiness", 0.3f);
        saudiElderly60.parameters.put("speaking_rate", 0.8f);
        saudiElderly60.parameters.put("emotional_tone", 0.3f); // Calm
        voiceTemplates.put(saudiElderly60.id, saudiElderly60);
        
        // مراهقة سعودية
        VoiceTemplate saudiTeenGirl = new VoiceTemplate(
            "saudi_teen_girl", 
            "مراهقة سعودية", 
            "صوت مراهقة سعودية حديثة ومتحمسة", 
            "Female", 
            "Arabic"
        );
        saudiTeenGirl.parameters.put("pitch_shift", 1.4f);
        saudiTeenGirl.parameters.put("formant_shift", 1.3f);
        saudiTeenGirl.parameters.put("warmth", 0.2f);
        saudiTeenGirl.parameters.put("clarity", 0.85f);
        saudiTeenGirl.parameters.put("breathiness", 0.3f);
        saudiTeenGirl.parameters.put("speaking_rate", 1.2f);
        saudiTeenGirl.parameters.put("emotional_tone", 0.8f); // Excited
        voiceTemplates.put(saudiTeenGirl.id, saudiTeenGirl);
    }
    
    private void createSaudiMaleTemplates() {
        // رجل سعودي عميق
        VoiceTemplate saudiManDeep = new VoiceTemplate(
            "saudi_man_deep", 
            "رجل سعودي عميق", 
            "صوت رجل سعودي عميق وذو سلطة", 
            "Male", 
            "Arabic"
        );
        saudiManDeep.parameters.put("pitch_shift", 0.7f);
        saudiManDeep.parameters.put("formant_shift", 0.7f);
        saudiManDeep.parameters.put("warmth", 0.5f);
        saudiManDeep.parameters.put("clarity", 0.9f);
        saudiManDeep.parameters.put("breathiness", 0.1f);
        saudiManDeep.parameters.put("speaking_rate", 0.9f);
        saudiManDeep.parameters.put("emotional_tone", 0.4f); // Authoritative
        voiceTemplates.put(saudiManDeep.id, saudiManDeep);
        
        // رجل سعودي متوسط
        VoiceTemplate saudiManMedium = new VoiceTemplate(
            "saudi_man_medium", 
            "رجل سعودي متوسط", 
            "صوت رجل سعودي متوسط ومتوازن", 
            "Male", 
            "Arabic"
        );
        saudiManMedium.parameters.put("pitch_shift", 0.9f);
        saudiManMedium.parameters.put("formant_shift", 0.9f);
        saudiManMedium.parameters.put("warmth", 0.6f);
        saudiManMedium.parameters.put("clarity", 0.9f);
        saudiManMedium.parameters.put("breathiness", 0.15f);
        saudiManMedium.parameters.put("speaking_rate", 1.0f);
        saudiManMedium.parameters.put("emotional_tone", 0.5f); // Neutral
        voiceTemplates.put(saudiManMedium.id, saudiManMedium);
    }
    
    private void createAgeBasedTemplates() {
        // طفل سعودي
        VoiceTemplate saudiChild8 = new VoiceTemplate(
            "saudi_child_8", 
            "طفل سعودي 8 سنوات", 
            "صوت طفل سعودي بريء ومليء بالحيوية", 
            "Child", 
            "Arabic"
        );
        saudiChild8.parameters.put("pitch_shift", 1.5f);
        saudiChild8.parameters.put("formant_shift", 1.4f);
        saudiChild8.parameters.put("warmth", 0.3f);
        saudiChild8.parameters.put("clarity", 0.8f);
        saudiChild8.parameters.put("breathiness", 0.4f);
        saudiChild8.parameters.put("speaking_rate", 1.3f);
        saudiChild8.parameters.put("emotional_tone", 0.9f); // Very happy
        voiceTemplates.put(saudiChild8.id, saudiChild8);
        
        // شاب سعودي
        VoiceTemplate saudiYoungMan = new VoiceTemplate(
            "saudi_young_man", 
            "شاب سعودي 25 سنة", 
            "صوت شاب سعودي حديث وحيوي", 
            "Young Adult", 
            "Arabic"
        );
        saudiYoungMan.parameters.put("pitch_shift", 1.1f);
        saudiYoungMan.parameters.put("formant_shift", 1.0f);
        saudiYoungMan.parameters.put("warmth", 0.4f);
        saudiYoungMan.parameters.put("clarity", 0.9f);
        saudiYoungMan.parameters.put("breathiness", 0.2f);
        saudiYoungMan.parameters.put("speaking_rate", 1.1f);
        saudiYoungMan.parameters.put("emotional_tone", 0.6f); // Energetic
        voiceTemplates.put(saudiYoungMan.id, saudiYoungMan);
    }
    
    private void createSpecialEffectTemplates() {
        // روبوت
        VoiceTemplate robot = new VoiceTemplate(
            "robot", 
            "روبوت", 
            "صوت روبوت تقني وميكانيكي", 
            "Special Effects", 
            "Arabic"
        );
        robot.parameters.put("pitch_shift", 1.0f);
        robot.parameters.put("formant_shift", 1.0f);
        robot.parameters.put("warmth", 0.0f);
        robot.parameters.put("clarity", 1.0f);
        robot.parameters.put("breathiness", 0.0f);
        robot.parameters.put("speaking_rate", 0.8f);
        robot.parameters.put("emotional_tone", 0.0f); // Robotic
        robot.parameters.put("robot_effect", 1.0f);
        voiceTemplates.put(robot.id, robot);
        
        // صوت خفي
        VoiceTemplate whisper = new VoiceTemplate(
            "whisper", 
            "صوت خفي", 
            "صوت خفي ومثير للاهتمام", 
            "Special Effects", 
            "Arabic"
        );
        whisper.parameters.put("pitch_shift", 1.2f);
        whisper.parameters.put("formant_shift", 1.1f);
        whisper.parameters.put("warmth", 0.3f);
        whisper.parameters.put("clarity", 0.6f);
        whisper.parameters.put("breathiness", 0.8f);
        whisper.parameters.put("speaking_rate", 0.7f);
        whisper.parameters.put("emotional_tone", 0.2f); // Mysterious
        whisper.parameters.put("whisper_effect", 1.0f);
        voiceTemplates.put(whisper.id, whisper);
        
        // صوت عميق ومخيف
        VoiceTemplate deepScary = new VoiceTemplate(
            "deep_scary", 
            "صوت عميق ومخيف", 
            "صوت عميق ومخيف للقصص المرعبة", 
            "Special Effects", 
            "Arabic"
        );
        deepScary.parameters.put("pitch_shift", 0.6f);
        deepScary.parameters.put("formant_shift", 0.6f);
        deepScary.parameters.put("warmth", 0.2f);
        deepScary.parameters.put("clarity", 0.8f);
        deepScary.parameters.put("breathiness", 0.3f);
        deepScary.parameters.put("speaking_rate", 0.7f);
        deepScary.parameters.put("emotional_tone", 0.1f); // Scary
        deepScary.parameters.put("reverb_effect", 0.5f);
        voiceTemplates.put(deepScary.id, deepScary);
    }
    
    // Public methods
    public List<VoiceTemplate> getTemplatesByCategory(String category) {
        List<VoiceTemplate> templates = new ArrayList<>();
        for (VoiceTemplate template : voiceTemplates.values()) {
            if (template.category.equals(category)) {
                templates.add(template);
            }
        }
        return templates;
    }
    
    public List<VoiceTemplate> getAllTemplates() {
        return new ArrayList<>(voiceTemplates.values());
    }
    
    public VoiceTemplate getTemplate(String templateId) {
        return voiceTemplates.get(templateId);
    }
    
    public List<VoiceProfile> getCustomVoices() {
        return new ArrayList<>(customVoices.values());
    }
    
    public VoiceProfile getCustomVoice(String voiceId) {
        return customVoices.get(voiceId);
    }
    
    public void addCustomVoice(VoiceProfile voiceProfile) {
        customVoices.put(voiceProfile.id, voiceProfile);
        Log.d(TAG, "Custom voice added: " + voiceProfile.name);
    }
    
    public void removeCustomVoice(String voiceId) {
        customVoices.remove(voiceId);
        Log.d(TAG, "Custom voice removed: " + voiceId);
    }
    
    public List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        for (VoiceTemplate template : voiceTemplates.values()) {
            if (!categories.contains(template.category)) {
                categories.add(template.category);
            }
        }
        return categories;
    }
    
    public List<VoiceTemplate> searchTemplates(String query) {
        List<VoiceTemplate> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        
        for (VoiceTemplate template : voiceTemplates.values()) {
            if (template.name.toLowerCase().contains(lowerQuery) ||
                template.description.toLowerCase().contains(lowerQuery) ||
                template.category.toLowerCase().contains(lowerQuery)) {
                results.add(template);
            }
        }
        
        return results;
    }
    
    public int getTemplateCount() {
        return voiceTemplates.size();
    }
    
    public int getCustomVoiceCount() {
        return customVoices.size();
    }
}
