# Voice Changer Pro - تطبيق تغيير الصوت المتقدم

تطبيق Android متقدم لتغيير الصوت في الوقت الفعلي مع دعم الصوت العربي السعودي.

## 🎯 المميزات الرئيسية

- **تغيير الصوت في الوقت الفعلي**: معالجة صوت فورية مع تأخير منخفض
- **دعم الصوت العربي السعودي**: نماذج صوتية مخصصة للصوت العربي
- **واجهة مستخدم عربية**: تصميم عصري ومتجاوب
- **معالجة صوت متقدمة**: خوارزميات متطورة لتحسين جودة الصوت
- **تكامل ElevenLabs API**: دعم كامل لخدمات الذكاء الاصطناعي
- **خدمة صوتية على مستوى النظام**: يعمل مع جميع التطبيقات

## 📋 المتطلبات

- **Android 8.0** (API level 26) أو أحدث
- **Java Development Kit (JDK) 8** أو أحدث
- **Android Studio** 2023.1 أو أحدث
- **مفتاح API من ElevenLabs**

## 🚀 التثبيت والتشغيل

### 1. إعداد البيئة
```bash
# استنساخ المشروع
git clone [repository-url]
cd Voice-Changer-APK-for-Real-Time-Saudi-Girl-Simulation

# فتح المشروع في Android Studio
# File -> Open -> اختر مجلد المشروع
```

### 2. إعداد مفتاح API
1. احصل على مفتاح API من [ElevenLabs](https://elevenlabs.io)
2. افتح التطبيق
3. أدخل المفتاح في حقل "ElevenLabs API Configuration"

### 3. بناء التطبيق
```bash
# بناء نسخة التطوير
./gradlew assembleDebug

# بناء نسخة الإنتاج
./gradlew assembleRelease
```

## 📱 الاستخدام

1. **فتح التطبيق** ومراجعة الأذونات المطلوبة
2. **إدخال مفتاح ElevenLabs API** في القسم المخصص
3. **اختيار نموذج الصوت** من القائمة المنسدلة
4. **اختيار وضع المعالجة** (Real-time, High-quality, etc.)
5. **الضغط على "Start Voice Changer"**
6. **منح الأذونات المطلوبة** عند الطلب

## 🔐 الأذونات المطلوبة

| الإذن | الغرض |
|--------|--------|
| `RECORD_AUDIO` | تسجيل الصوت من الميكروفون |
| `INTERNET` | الاتصال بخدمات ElevenLabs API |
| `SYSTEM_ALERT_WINDOW` | عرض النوافذ فوق التطبيقات الأخرى |
| `BIND_ACCESSIBILITY_SERVICE` | الخدمة المساعدة لالتقاط الصوت |
| `FOREGROUND_SERVICE` | تشغيل الخدمة في الخلفية |
| `WAKE_LOCK` | منع الجهاز من النوم أثناء المعالجة |

## 🏗️ هيكل المشروع

```
app/
├── src/main/
│   ├── java/com/voicechanger/app/
│   │   ├── MainActivity.java              # النشاط الرئيسي
│   │   ├── AudioProcessor.java            # معالج الصوت
│   │   ├── ElevenLabsApiClient.java       # عميل API
│   │   ├── SystemWideAudioService.java    # خدمة الصوت الشاملة
│   │   ├── VoiceProcessingEngine.java     # محرك معالجة الصوت
│   │   └── MediaProjectionService.java    # خدمة الإسقاط
│   ├── res/
│   │   ├── layout/                        # تخطيطات الواجهة
│   │   ├── values/                        # القيم والموارد
│   │   ├── drawable/                      # الرسوم والأيقونات
│   │   └── xml/                          # ملفات XML الإضافية
│   └── AndroidManifest.xml               # بيان التطبيق
├── build.gradle                          # إعدادات البناء
└── proguard-rules.pro                    # قواعد ProGuard
```

## 🔧 التطوير

### إضافة نماذج صوتية جديدة
```java
// في MainActivity.java
List<String> voiceModels = new ArrayList<>();
voiceModels.add("saudi_girl_warm");
voiceModels.add("your_new_voice_model");
```

### تخصيص معالجة الصوت
```java
// في VoiceProcessingEngine.java
public void setCustomProcessingMode(VoiceProcessingMode mode) {
    // إضافة منطق المعالجة المخصص
}
```

## 🐛 استكشاف الأخطاء

### مشاكل شائعة وحلولها

1. **خطأ في الأذونات**
   - تأكد من منح جميع الأذونات المطلوبة
   - أعد تشغيل التطبيق بعد منح الأذونات

2. **مشكلة في الاتصال بـ API**
   - تحقق من صحة مفتاح ElevenLabs API
   - تأكد من الاتصال بالإنترنت

3. **تأخير في معالجة الصوت**
   - قلل من جودة المعالجة في الإعدادات
   - تأكد من قوة إشارة الإنترنت

## 📄 الترخيص

هذا المشروع مفتوح المصدر ومتاح للاستخدام التعليمي والتطويري.

## 🤝 المساهمة

نرحب بالمساهمات! يرجى:
1. عمل Fork للمشروع
2. إنشاء فرع للميزة الجديدة
3. إرسال Pull Request

## 📞 الدعم

للحصول على الدعم أو الإبلاغ عن مشاكل:
- فتح Issue في GitHub
- مراجعة الوثائق
- التواصل مع فريق التطوير

---

**ملاحظة**: هذا التطبيق مخصص للاستخدام التعليمي والتطويري. يرجى احترام قوانين الخصوصية وحقوق الملكية الفكرية.
