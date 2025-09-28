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

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep serializable classes
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}