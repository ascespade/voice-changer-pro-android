# Voice Changer Pro - دليل التطوير عن بُعد لـ Android Studio

## 🎯 نظرة عامة

تم إعداد بيئة التطوير عن بُعد بحيث:
- **السيرفر**: يحتوي على Android Studio وجميع أدوات التطوير
- **اللابتوب**: يستخدم Android Studio كواجهة فقط
- **البناء والاختبار**: يتم على السيرفر
- **الملفات**: متزامنة بين اللابتوب والسيرفر

## 🚀 البدء السريع

### 1. الاتصال بالسيرفر
```bash
ssh android-dev-server
```

### 2. مزامنة المشروع
```bash
./simple-sync.sh
```

### 3. بناء المشروع
```bash
./build-on-server.sh
```

### 4. اختبار المشروع
```bash
./test-on-server.sh
```

## 📋 السكريبتات المتاحة

| السكريبت | الوصف |
|---------|-------|
| `simple-sync.sh` | مزامنة الملفات الأساسية إلى السيرفر |
| `build-on-server.sh` | بناء المشروع على السيرفر وتنزيل APK |
| `test-on-server.sh` | تشغيل الاختبارات على السيرفر |
| `connect-to-server.sh` | الاتصال بالسيرفر للتطوير |
| `complete-server-setup.sh` | إكمال إعداد السيرفر |

## 🔧 إعداد Android Studio على اللابتوب

### الطريقة 1: Android Studio مع Remote Development

1. **فتح Android Studio**
2. **File → Open**
3. **اختيار مسار السيرفر**: `android-dev-server:~/projects/voice-changer-pro-android`
4. **تكوين Remote Development**:
   - File → Settings → Build → Gradle
   - Use Gradle from: 'gradle-wrapper.properties' file
   - Gradle JVM: Remote JVM

### الطريقة 2: VS Code مع Remote-SSH

1. **تثبيت Remote-SSH extension**
2. **الاتصال**: `android-dev-server`
3. **فتح المشروع**: `~/projects/voice-changer-pro-android`

## 🏗️ البناء والتطوير

### بناء المشروع
```bash
# على السيرفر
cd ~/projects/voice-changer-pro-android
./gradlew assembleDebug

# من اللابتوب
./build-on-server.sh
```

### تشغيل الاختبارات
```bash
# على السيرفر
cd ~/projects/voice-changer-pro-android
./gradlew test

# من اللابتوب
./test-on-server.sh
```

### تنظيف المشروع
```bash
# على السيرفر
cd ~/projects/voice-changer-pro-android
./gradlew clean
```

## 📱 اختبار التطبيق

### على المحاكي
```bash
# تشغيل المحاكي
~/Android/Sdk/emulator/emulator -avd Pixel_7_API_34 &

# تثبيت APK
adb install app/build/outputs/apk/debug/app-debug.apk
```

### على الجهاز الحقيقي
1. **تفعيل USB Debugging** على الجهاز
2. **ربط الجهاز** باللابتوب
3. **تثبيت APK**: `adb install app-debug.apk`

## 🔍 استكشاف الأخطاء

### مشاكل الاتصال
```bash
# اختبار الاتصال
ssh android-dev-server "echo 'Connection OK'"

# فحص حالة السيرفر
ssh android-dev-server "systemctl status android-studio"
```

### مشاكل البناء
```bash
# فحص Java
ssh android-dev-server "java -version"

# فحص Android SDK
ssh android-dev-server "echo \$ANDROID_HOME"

# تنظيف وإعادة البناء
ssh android-dev-server "cd ~/projects/voice-changer-pro-android && ./gradlew clean assembleDebug"
```

### مشاكل المزامنة
```bash
# مزامنة يدوية
./simple-sync.sh

# فحص الملفات على السيرفر
ssh android-dev-server "ls -la ~/projects/voice-changer-pro-android/"
```

## 📊 مراقبة الأداء

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
ssh android-dev-server "ps aux | grep -E '(java|gradle|android)'"
```

## 🔐 الأمان

### حماية المفتاح
```bash
chmod 600 my-dev-key.pem
```

### تحديث المفتاح
```bash
# نسخ مفتاح جديد
scp new-key.pem android-dev-server:~/.ssh/
```

## 📈 تحسين الأداء

### إعدادات Gradle
```properties
# في gradle.properties
org.gradle.jvmargs=-Xmx4096m
org.gradle.parallel=true
org.gradle.caching=true
```

### إعدادات Android Studio
- **Memory**: 4GB+
- **Build Process**: Parallel builds
- **Indexing**: Background indexing

## 🆘 الدعم

### السجلات
```bash
# سجلات Android Studio
ssh android-dev-server "journalctl -u android-studio"

# سجلات البناء
ssh android-dev-server "cd ~/projects/voice-changer-pro-android && ./gradlew --info"
```

### إعادة التشغيل
```bash
# إعادة تشغيل خدمة Android Studio
ssh android-dev-server "sudo systemctl restart android-studio"

# إعادة تشغيل السيرفر
ssh android-dev-server "sudo reboot"
```

## 📞 المساعدة

للحصول على المساعدة:
1. **تحقق من السجلات** أعلاه
2. **راجع هذا الدليل** مرة أخرى
3. **اتصل بفريق التطوير**

---

**ملاحظة**: هذا الإعداد يسمح لك بالتطوير على السيرفر باستخدام Android Studio على اللابتوب، مع الحفاظ على جميع الملفات متزامنة.
