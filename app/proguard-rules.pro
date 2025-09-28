# Voice Changer Pro - ProGuard Rules for Security and Optimization

# Basic Android rules
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-dontwarn android.support.**
-dontwarn androidx.**

# Security: Remove debug information
-keepattributes !LocalVariableTable,!LocalVariableTypeTable
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep essential voice processing classes
-keep class com.voicechanger.app.** { *; }

# Keep AI and API classes
-keep class com.voicechanger.app.GeminiAIService { *; }
-keep class com.voicechanger.app.AIVoiceAnalyzer { *; }
-keep class com.voicechanger.app.VoiceCloningEngine { *; }

# Keep audio processing classes
-keep class com.voicechanger.app.AudioProcessor { *; }
-keep class com.voicechanger.app.AdvancedVoiceProcessor { *; }
-keep class com.voicechanger.app.LiveCallOptimizer { *; }
-keep class com.voicechanger.app.SystemWideVoiceProcessor { *; }

# Keep service classes
-keep class com.voicechanger.app.SystemWideAudioService { *; }
-keep class com.voicechanger.app.MediaProjectionService { *; }

# Keep template management
-keep class com.voicechanger.app.VoiceTemplateManager { *; }

# Keep OkHttp and Gson
-keep class okhttp3.** { *; }
-keep class com.google.gson.** { *; }
-keep class retrofit2.** { *; }

# Security: Obfuscate package names
-repackageclasses 'a'
-flattenpackagehierarchy 'a'

# Remove unused code
-dontshrink
-dontoptimize
-dontpreverify

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Security: Remove logging
-assumenosideeffects class * {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}
