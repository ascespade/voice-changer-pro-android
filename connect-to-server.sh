#!/bin/bash

# Connect to server for development
echo "🔗 Connecting to server for development..."

echo "📋 Available commands on server:"
echo "  - cd ~/projects/voice-changer-pro-android"
echo "  - ./gradlew assembleDebug"
echo "  - ./gradlew test"
echo "  - ~/android-studio/bin/studio.sh"
echo ""

ssh android-dev-server
