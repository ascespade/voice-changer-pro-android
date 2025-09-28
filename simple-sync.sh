#!/bin/bash

# Simple sync using scp
echo "📤 Syncing project to server..."

# Create projects directory on server if it doesn't exist
ssh android-dev-server "mkdir -p ~/projects/voice-changer-pro-android"

# Sync essential files only
echo "📁 Syncing source files..."
scp -r app/src android-dev-server:~/projects/voice-changer-pro-android/app/
scp -r app/build.gradle android-dev-server:~/projects/voice-changer-pro-android/app/
scp -r app/proguard-rules.pro android-dev-server:~/projects/voice-changer-pro-android/app/

echo "📁 Syncing project files..."
scp build.gradle android-dev-server:~/projects/voice-changer-pro-android/
scp settings.gradle android-dev-server:~/projects/voice-changer-pro-android/
scp gradle.properties android-dev-server:~/projects/voice-changer-pro-android/
scp gradlew android-dev-server:~/projects/voice-changer-pro-android/
scp -r gradle android-dev-server:~/projects/voice-changer-pro-android/

echo "✅ Project synced to server!"
