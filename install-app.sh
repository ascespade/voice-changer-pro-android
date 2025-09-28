#!/bin/bash
echo "📱 Installing app on emulator..."

# Build and install on server
ssh android-dev-server "cd ~/projects/voice-changer-pro-android && ./gradlew clean assembleDebug installDebug"

echo "✅ App installed on emulator!"
