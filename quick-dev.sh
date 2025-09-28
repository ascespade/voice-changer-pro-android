#!/bin/bash
echo "🚀 Quick development workflow..."

# Sync project
echo "1️⃣ Syncing project..."
./sync-project.sh

# Build project
echo "2️⃣ Building project..."
ssh android-dev-server "cd ~/projects/voice-changer-pro-android && ./gradlew clean assembleDebug"

# Install on emulator
echo "3️⃣ Installing on emulator..."
ssh android-dev-server "cd ~/projects/voice-changer-pro-android && ./gradlew installDebug"

echo "✅ Quick development cycle completed!"
echo "📱 App is now running on emulator"
