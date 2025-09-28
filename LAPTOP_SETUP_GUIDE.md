# دليل إعداد اللابتوب للتطوير عن بُعد - Android Studio

## 🎯 نظرة عامة

هذا الدليل يوضح كيفية إعداد اللابتوب للتطوير عن بُعد مع السيرفر الذي يحتوي على:
- ✅ Android SDK
- ✅ Android Emulator
- ✅ Gradle
- ✅ مشروع Android

## 📋 المتطلبات

- **نظام التشغيل**: Windows, macOS, أو Linux
- **الذاكرة**: 8GB RAM على الأقل
- **المساحة**: 10GB مساحة فارغة
- **الاتصال**: إنترنت مستقر

## 🚀 الخطوة 1: تثبيت Android Studio

### Windows:
1. اذهب إلى [Android Studio](https://developer.android.com/studio)
2. حمل Android Studio
3. شغل المثبت واتبع التعليمات
4. اختر "Standard" installation

### macOS:
1. حمل Android Studio من الموقع الرسمي
2. اسحب Android Studio إلى Applications
3. شغل Android Studio من Applications

### Linux:
```bash
# Ubuntu/Debian
sudo snap install android-studio --classic

# أو
wget https://redirector.gvt1.com/edgedl/android/studio/ide-zips/2023.3.1.18/android-studio-2023.3.1.18-linux.tar.gz
tar -xzf android-studio-2023.3.1.18-linux.tar.gz
sudo mv android-studio /opt/
sudo ln -s /opt/android-studio/bin/studio.sh /usr/local/bin/studio
```

## 🔧 الخطوة 2: تكوين Android Studio

### 1. فتح Android Studio
- شغل Android Studio
- اختر "Do not import settings" إذا كان أول مرة

### 2. إعداد SDK
- اذهب إلى **File → Settings** (أو **Android Studio → Preferences** على macOS)
- اختر **Appearance & Behavior → System Settings → Android SDK**
- تأكد من تثبيت:
  - Android SDK Platform 34
  - Android SDK Build-Tools 34.0.0
  - Android SDK Platform-Tools
  - Android Emulator

### 3. تكوين Remote Development
- اذهب إلى **File → Settings → Build, Execution, Deployment → Gradle**
- اختر **Use Gradle from: 'gradle-wrapper.properties' file**
- اختر **Gradle JVM: Remote JVM**

## 🔗 الخطوة 3: إعداد الاتصال بالسيرفر

### 1. تثبيت SSH Client
- **Windows**: استخدم PuTTY أو Windows Subsystem for Linux (WSL)
- **macOS**: SSH مثبت مسبقاً
- **Linux**: SSH مثبت مسبقاً

### 2. تكوين SSH
```bash
# إنشاء SSH config
mkdir -p ~/.ssh
cat >> ~/.ssh/config << 'EOF'
Host android-dev-server
    HostName ec2-100-27-210-173.compute-1.amazonaws.com
    User ubuntu
    IdentityFile /path/to/your/my-dev-key.pem
    StrictHostKeyChecking no
    UserKnownHostsFile /dev/null
    ServerAliveInterval 60
    ServerAliveCountMax 3
    LocalForward 8080 localhost:8080
    LocalForward 8081 localhost:8081
    LocalForward 8082 localhost:8082
    LocalForward 8083 localhost:8083
    LocalForward 8084 localhost:8084
    LocalForward 5005 localhost:5005
    LocalForward 5006 localhost:5006
    LocalForward 5007 localhost:5007
    LocalForward 5008 localhost:5008
    LocalForward 5009 localhost:5009
    LocalForward 5010 localhost:5010
    ForwardX11 yes
    ForwardX11Trusted yes
    Compression yes
    ForwardAgent yes
EOF
```

### 3. اختبار الاتصال
```bash
ssh android-dev-server "echo 'Connection successful'"
```

## 📱 الخطوة 4: إعداد المحاكي

### 1. تثبيت X11 Server (للـ Windows)
- حمل [VcXsrv](https://sourceforge.net/projects/vcxsrv/)
- شغل XLaunch
- اختر "Multiple windows"
- اختر "Start no client"
- فعّل "Disable access control"

### 2. تثبيت XQuartz (للـ macOS)
```bash
brew install --cask xquartz
```

### 3. بدء المحاكي على السيرفر
```bash
# الاتصال بالسيرفر
ssh android-dev-server

# بدء المحاكي
~/emulator-control.sh start

# فحص حالة المحاكي
~/emulator-control.sh status
```

## 🎯 الخطوة 5: فتح المشروع

### الطريقة 1: Android Studio مع Remote Development
1. **فتح Android Studio**
2. **File → Open**
3. **اختيار مسار السيرفر**: `android-dev-server:~/projects/voice-changer-pro-android`
4. **انتظار تحميل المشروع**

### الطريقة 2: VS Code مع Remote-SSH
1. **تثبيت Remote-SSH extension**
2. **الاتصال**: `android-dev-server`
3. **فتح المشروع**: `~/projects/voice-changer-pro-android`

## 🔨 الخطوة 6: البناء والاختبار

### 1. بناء المشروع
```bash
# على السيرفر
ssh android-dev-server "cd ~/projects/voice-changer-pro-android && ./gradlew assembleDebug"

# أو من اللابتوب
./build-on-server.sh
```

### 2. تثبيت التطبيق
```bash
# على السيرفر
ssh android-dev-server "~/adb-control.sh install ~/projects/voice-changer-pro-android/app/build/outputs/apk/debug/app-debug.apk"

# أو من اللابتوب
scp android-dev-server:~/projects/voice-changer-pro-android/app/build/outputs/apk/debug/app-debug.apk ./
adb install app-debug.apk
```

### 3. تشغيل الاختبارات
```bash
# على السيرفر
ssh android-dev-server "cd ~/projects/voice-changer-pro-android && ./gradlew test"

# أو من اللابتوب
./test-on-server.sh
```

## 🎮 الخطوة 7: استخدام المحاكي

### 1. بدء المحاكي
```bash
ssh android-dev-server "~/emulator-control.sh start"
```

### 2. فحص الأجهزة المتصلة
```bash
ssh android-dev-server "~/adb-control.sh devices"
```

### 3. تثبيت التطبيق على المحاكي
```bash
ssh android-dev-server "~/build-and-test.sh run"
```

### 4. عرض سجلات التطبيق
```bash
ssh android-dev-server "~/adb-control.sh logcat"
```

## 🔍 الخطوة 8: استكشاف الأخطاء

### مشاكل الاتصال
```bash
# اختبار الاتصال
ssh android-dev-server "echo 'Connection OK'"

# فحص حالة السيرفر
ssh android-dev-server "~/android-status.sh"
```

### مشاكل المحاكي
```bash
# إعادة تشغيل المحاكي
ssh android-dev-server "~/emulator-control.sh stop"
ssh android-dev-server "~/emulator-control.sh start"

# فحص حالة المحاكي
ssh android-dev-server "~/emulator-control.sh status"
```

### مشاكل البناء
```bash
# تنظيف وإعادة البناء
ssh android-dev-server "cd ~/projects/voice-changer-pro-android && ./gradlew clean assembleDebug"
```

## 📊 الخطوة 9: مراقبة الأداء

### استخدام الذاكرة
```bash
ssh android-dev-server "free -h"
```

### استخدام القرص
```bash
ssh android-dev-server "df -h"
```

### العمليات النشطة
```bash
ssh android-dev-server "ps aux | grep -E '(java|gradle|android|emulator)'"
```

## 🚀 الخطوة 10: سكريبتات مفيدة

### إنشاء سكريبتات على اللابتوب
```bash
# مزامنة المشروع
cat > sync-project.sh << 'EOF'
#!/bin/bash
echo "📤 Syncing project to server..."
scp -r app/src android-dev-server:~/projects/voice-changer-pro-android/app/
scp app/build.gradle android-dev-server:~/projects/voice-changer-pro-android/app/
scp build.gradle android-dev-server:~/projects/voice-changer-pro-android/
scp settings.gradle android-dev-server:~/projects/voice-changer-pro-android/
echo "✅ Project synced!"
EOF

# بناء المشروع
cat > build-project.sh << 'EOF'
#!/bin/bash
echo "🔨 Building project on server..."
ssh android-dev-server "cd ~/projects/voice-changer-pro-android && ./gradlew assembleDebug"
echo "📥 Downloading APK..."
scp android-dev-server:~/projects/voice-changer-pro-android/app/build/outputs/apk/debug/app-debug.apk ./
echo "✅ Build completed!"
EOF

# تشغيل المحاكي
cat > start-emulator.sh << 'EOF'
#!/bin/bash
echo "🎮 Starting emulator on server..."
ssh android-dev-server "~/emulator-control.sh start"
echo "✅ Emulator started!"
EOF

# تثبيت التطبيق
cat > install-app.sh << 'EOF'
#!/bin/bash
echo "📱 Installing app on emulator..."
ssh android-dev-server "~/build-and-test.sh run"
echo "✅ App installed!"
EOF

chmod +x *.sh
```

## 📞 الدعم

### السجلات
```bash
# سجلات Android Studio
ssh android-dev-server "journalctl -u android-emulator"

# سجلات البناء
ssh android-dev-server "cd ~/projects/voice-changer-pro-android && ./gradlew --info"
```

### إعادة التشغيل
```bash
# إعادة تشغيل خدمة المحاكي
ssh android-dev-server "sudo systemctl restart android-emulator"

# إعادة تشغيل السيرفر
ssh android-dev-server "sudo reboot"
```

## 🎉 النتيجة النهائية

بعد اتباع هذا الدليل، ستحصل على:
- ✅ Android Studio على اللابتوب
- ✅ اتصال آمن بالسيرفر
- ✅ محاكي Android على السيرفر
- ✅ بناء وتطوير عن بُعد
- ✅ اختبار التطبيقات على المحاكي

---

**ملاحظة**: هذا الإعداد يسمح لك بالتطوير على اللابتوب مع البناء والاختبار على السيرفر، مما يوفر الأداء والموارد.
