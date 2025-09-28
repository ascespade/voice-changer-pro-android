#!/bin/bash

# Test project on server
echo "🧪 Running tests on server..."

# Sync project first
./sync-to-server.sh

# Run tests on server
ssh android-dev-server "cd ~/projects/voice-changer-pro-android && ./gradlew test"

echo "✅ Tests completed!"
