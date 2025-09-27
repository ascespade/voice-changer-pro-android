# Voice Changer Pro - Build Instructions

## Development Environment Setup

### Prerequisites
- **Android Studio**: Latest stable version (2023.1.1 or newer)
- **JDK**: OpenJDK 17 or newer
- **Android SDK**: API levels 24-34
- **NDK**: Latest version (for audio processing optimizations)
- **Git**: For version control

### SDK Components Required
```bash
# Install required SDK components
sdkmanager "platform-tools"
sdkmanager "build-tools;34.0.0"
sdkmanager "platforms;android-34"
sdkmanager "platforms;android-24"  # Minimum API level
sdkmanager "ndk;25.2.9519653"      # Latest NDK
```

## Project Structure

```
VoiceChangerApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/voicechanger/app/
│   │   │   ├── MainActivity.java
│   │   │   ├── VoiceProcessingEngine.java
│   │   │   ├── SystemWideAudioService.java
│   │   │   ├── SystemWideVoiceProcessor.java
│   │   │   ├── AudioProcessor.java
│   │   │   ├── RealTimeVoiceChanger.java
│   │   │   ├── ElevenLabsApiClient.java
│   │   │   └── VoiceProcessingService.java
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml
│   │   │   ├── values/strings.xml
│   │   │   ├── values/colors.xml
│   │   │   ├── values/themes.xml
│   │   │   ├── drawable/
│   │   │   ├── xml/accessibility_service_config.xml
│   │   │   └── xml/backup_rules.xml
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## Build Configuration

### App-level build.gradle
```gradle
plugins {
    id 'com.android.application'
}

android {
    namespace 'com.voicechanger.app'
    compileSdk 34

    defaultConfig {
        applicationId "com.voicechanger.app"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            signingConfig signingConfigs.release
        }
        debug {
            minifyEnabled false
            debuggable true
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    packagingOptions {
        pickFirst '**/libc++_shared.so'
        pickFirst '**/libjsc.so'
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.10.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.core:core:1.12.0'
    
    // HTTP client for API calls
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'
    
    // JSON parsing
    implementation 'com.google.code.gson:gson:2.10.1'
    
    // Audio processing
    implementation 'androidx.media:media:1.7.0'
    
    // Testing
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}
```

### Project-level build.gradle
```gradle
plugins {
    id 'com.android.application' version '8.1.4' apply false
    id 'com.android.library' version '8.1.4' apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}

task clean(type: Delete) {
    delete rootProject.buildDir
}
```

## Required XML Configurations

### accessibility_service_config.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged|typeViewFocused|typeNotificationStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagRetrieveInteractiveWindows"
    android:canRetrieveWindowContent="true"
    android:canRequestFilterKeyEvents="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100"
    android:packageNames="com.whatsapp,com.snapchat.android,com.facebook.orca,com.viber.voip,com.skype.raider,us.zoom.videomeetings,com.discord,org.telegram.messenger,com.google.android.apps.tachyon,com.microsoft.teams,com.android.server.telecom,com.android.dialer" />
```

### Updated AndroidManifest.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Audio and system permissions -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
    <uses-permission android:name="android.permission.CAPTURE_AUDIO_OUTPUT" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.VoiceChangerApp"
        tools:targetApi="31">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.VoiceChangerApp">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
        <service
            android:name=".VoiceProcessingService"
            android:enabled="true"
            android:exported="false" />
            
        <service
            android:name=".SystemWideAudioService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>
    </application>
</manifest>
```

## Building the APK

### Debug Build
```bash
# Navigate to project directory
cd VoiceChangerApp

# Build debug APK
./gradlew assembleDebug

# APK location: app/build/outputs/apk/debug/app-debug.apk
```

### Release Build
```bash
# Generate signing key (first time only)
keytool -genkey -v -keystore voice-changer-key.keystore -alias voice-changer -keyalg RSA -keysize 2048 -validity 10000

# Build release APK
./gradlew assembleRelease

# APK location: app/build/outputs/apk/release/app-release.apk
```

### Signing Configuration
Add to app/build.gradle:
```gradle
android {
    signingConfigs {
        release {
            storeFile file('voice-changer-key.keystore')
            storePassword 'your_store_password'
            keyAlias 'voice-changer'
            keyPassword 'your_key_password'
        }
    }
}
```

## Testing and Debugging

### Unit Testing
```bash
# Run unit tests
./gradlew test

# Run with coverage
./gradlew testDebugUnitTestCoverage
```

### Integration Testing
```bash
# Run instrumented tests
./gradlew connectedAndroidTest
```

### Audio Testing
1. **Test on real device** (emulator audio is limited)
2. **Test with different communication apps**
3. **Verify accessibility service functionality**
4. **Test system audio capture on Android 10+**

### Debugging Tips
```bash
# Enable audio debugging
adb shell setprop log.tag.AudioFlinger V
adb shell setprop log.tag.AudioPolicyService V

# Monitor accessibility service
adb shell dumpsys accessibility

# Check audio focus
adb shell dumpsys audio
```

## Optimization and Performance

### ProGuard Configuration (proguard-rules.pro)
```proguard
# Keep accessibility service
-keep class com.voicechanger.app.SystemWideAudioService { *; }

# Keep audio processing classes
-keep class com.voicechanger.app.AudioProcessor { *; }
-keep class com.voicechanger.app.SystemWideVoiceProcessor { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Keep Gson
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
```

### Performance Optimization
1. **Enable R8 optimization** in release builds
2. **Use vector drawables** to reduce APK size
3. **Optimize audio buffer sizes** for target devices
4. **Implement lazy loading** for AI models
5. **Use background threads** for heavy processing

## Deployment

### APK Distribution
1. **Direct APK**: Distribute via file sharing
2. **Internal testing**: Use Google Play Console internal testing
3. **Beta testing**: Closed testing with selected users
4. **Production**: Full Play Store release (requires policy compliance)

### Play Store Considerations
- **Accessibility service usage** requires detailed explanation
- **Audio recording permissions** need clear justification
- **Privacy policy** must explain data usage
- **Target audience** should be clearly defined

### Alternative Distribution
- **F-Droid**: Open source app store
- **APKPure**: Third-party app store
- **Direct download**: Host APK on website
- **Enterprise distribution**: For business use

## Troubleshooting Build Issues

### Common Build Errors
1. **SDK not found**: Update local.properties with correct SDK path
2. **NDK not found**: Install NDK through SDK Manager
3. **Dependency conflicts**: Use dependency resolution strategies
4. **Memory issues**: Increase Gradle heap size

### Gradle Configuration Issues
```gradle
# In gradle.properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
android.useAndroidX=true
android.enableJetifier=true
```

### Audio Processing Issues
1. **Test on multiple devices** with different Android versions
2. **Verify permissions** are properly requested and granted
3. **Check audio format compatibility** across devices
4. **Monitor memory usage** during audio processing

## Continuous Integration

### GitHub Actions Example
```yaml
name: Build APK

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Setup Android SDK
      uses: android-actions/setup-android@v2
      
    - name: Build debug APK
      run: ./gradlew assembleDebug
      
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
```

This completes the comprehensive build instructions for the Voice Changer Pro Android application.

