# Voice Changer Pro - Installation & Setup Guide

## Quick Start Guide

### Step 1: Download and Install
1. Download the VoiceChangerApp.apk file
2. Enable "Install from Unknown Sources" in Android Settings > Security
3. Install the APK file
4. Launch the application

### Step 2: Grant Permissions
1. **Accessibility Service**: Go to Settings > Accessibility > Voice Changer Pro > Enable
2. **Audio Permissions**: Allow microphone and audio recording when prompted
3. **Media Projection**: Grant screen recording permission for system audio capture
4. **Background Activity**: Allow the app to run in background

### Step 3: Configure Voice Settings
1. Open the app and enter your preferred settings
2. Select "Saudi Girl Warm" voice profile (default)
3. Choose processing mode:
   - **Real-time**: Low latency, good for calls
   - **High Quality**: Better voice quality, slight delay
   - **Auto**: Switches automatically based on app

### Step 4: Start Voice Changing
1. Tap "Start Voice Changer"
2. The app will now work system-wide
3. Test with any communication app (WhatsApp, calls, etc.)

## Detailed Setup Instructions

### Android Version Requirements
- **Minimum**: Android 7.0 (API 24)
- **Recommended**: Android 10+ (API 29) for full system audio capture
- **Optimal**: Android 11+ for best performance

### Required Permissions Explained

#### Accessibility Service
This is the most important permission that enables system-wide voice changing:
1. Go to **Settings > Accessibility**
2. Find **Voice Changer Pro** in the list
3. Tap it and toggle **ON**
4. Confirm the warning dialog

#### Audio Permissions
- **Record Audio**: Captures your voice
- **Modify Audio Settings**: Routes processed audio
- **Capture Audio Output**: System-wide audio capture (Android 10+)

#### Background Permissions
- **Background App Refresh**: Keeps the app running
- **Battery Optimization**: Disable for this app
- **Autostart**: Enable if available on your device

### Troubleshooting Setup Issues

#### App Not Working System-Wide
1. Verify Accessibility Service is enabled
2. Restart the app after enabling accessibility
3. Check if MediaProjection permission was granted
4. Try toggling the accessibility service off and on

#### Poor Voice Quality
1. Switch to "High Quality" mode if using "Real-time"
2. Ensure stable internet connection for AI processing
3. Close other audio apps that might interfere
4. Restart the device if issues persist

#### App Stops Working
1. Disable battery optimization for the app
2. Enable autostart if available
3. Check if accessibility service got disabled
4. Ensure the app has background activity permission

### Compatibility Notes

#### Tested Communication Apps
✅ **Fully Compatible:**
- WhatsApp Voice/Video Calls
- Phone Calls (GSM/VoLTE)
- Snapchat Voice Messages
- Telegram Voice Calls
- Discord Voice Chat
- Zoom Meetings
- Google Meet
- Skype

✅ **Partially Compatible:**
- Instagram Voice Messages
- Facebook Messenger
- Viber
- WeChat

⚠️ **Limited Compatibility:**
- Some gaming voice chats
- Apps with custom audio drivers

#### Device Compatibility
✅ **Fully Supported:**
- Samsung Galaxy series (Android 10+)
- Google Pixel series
- OnePlus devices
- Xiaomi/MIUI (with accessibility enabled)
- Huawei/EMUI (non-HMS versions)

⚠️ **Limited Support:**
- Heavily customized Android ROMs
- Very old devices (< 3GB RAM)
- Devices with custom audio implementations

### Performance Optimization

#### For Best Performance:
1. **Close unnecessary apps** before using
2. **Use "Real-time" mode** for live calls
3. **Ensure good internet** for AI processing
4. **Keep device charged** (processing is CPU intensive)
5. **Use wired headphones** to prevent feedback

#### Battery Saving Tips:
1. Use "Real-time" mode instead of "High Quality"
2. Disable the app when not needed
3. Enable "Auto" mode for intelligent switching
4. Close other audio/media apps

### Advanced Configuration

#### Custom Voice Profiles
1. Open app settings
2. Go to "Voice Profiles"
3. Tap "Create Custom"
4. Adjust pitch, formants, and warmth
5. Save with a custom name

#### Processing Mode Details
- **Real-time**: Uses local algorithms, ~200ms latency
- **High Quality**: Uses AI services, ~500-2000ms latency  
- **Auto**: Switches based on detected app and network

#### Network Settings
- **AI Service Priority**: Choose preferred free AI service
- **Fallback Mode**: What to do when AI services fail
- **Quality vs Speed**: Balance processing quality and latency

### Security & Privacy

#### Data Usage
- Voice processing happens locally when possible
- AI services only receive audio chunks, no personal data
- No voice data is stored permanently
- All processing is real-time, no recordings saved

#### Permissions Explained
- **Accessibility**: Required for system-wide audio capture
- **Microphone**: Captures your voice for processing
- **Internet**: Connects to free AI services for high-quality processing
- **Background**: Keeps the app running continuously

### Getting Help

#### Common Questions
**Q: Does this work with all apps?**
A: Works with most communication apps. Some apps with custom audio may have limited compatibility.

**Q: Is my voice data stored anywhere?**
A: No, all processing is real-time. No voice data is stored or transmitted except temporarily to AI services.

**Q: Why does it need accessibility permissions?**
A: This is the only way to capture system-wide audio on Android without root access.

**Q: Can I use this for streaming?**
A: Yes, it works with most streaming apps and platforms.

#### Support Resources
- Check the troubleshooting section above
- Restart the app and device
- Verify all permissions are granted
- Ensure your device meets minimum requirements

### Legal Notice
This app is for entertainment and accessibility purposes. Users are responsible for complying with local laws regarding voice modification and recording. The app does not store or transmit personal voice data beyond temporary processing.

