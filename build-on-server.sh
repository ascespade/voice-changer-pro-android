#!/bin/bash

# Build project on server
echo "🔨 Building project on server..."

# Sync project first
./sync-to-server.sh

# Build on server
ssh android-dev-server "cd ~/projects/voice-changer-pro-android && ./gradlew clean assembleDebug"

# Download APK
echo "📥 Downloading APK from server..."
scp android-dev-server:~/projects/voice-changer-pro-android/app/build/outputs/apk/debug/app-debug.apk ./

echo "✅ Build completed! APK downloaded to current directory."
echo "📱 APK location: $(pwd)/app-debug.apk"
