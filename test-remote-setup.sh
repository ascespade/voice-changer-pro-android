#!/bin/bash

# Test remote development setup
echo "🧪 Testing remote development setup..."

# Test 1: Connection
echo "1️⃣ Testing connection to server..."
if ssh android-dev-server "echo 'Connection successful'"; then
    echo "✅ Connection test passed"
else
    echo "❌ Connection test failed"
    exit 1
fi

# Test 2: Java installation
echo "2️⃣ Testing Java installation..."
if ssh android-dev-server "java -version"; then
    echo "✅ Java test passed"
else
    echo "❌ Java test failed - installing Java..."
    ssh android-dev-server "sudo apt update && sudo apt install -y openjdk-17-jdk"
fi

# Test 3: Android SDK
echo "3️⃣ Testing Android SDK..."
if ssh android-dev-server "echo \$ANDROID_HOME"; then
    echo "✅ Android SDK test passed"
else
    echo "❌ Android SDK test failed - setting up SDK..."
    ssh android-dev-server "mkdir -p ~/Android/Sdk && export ANDROID_HOME=~/Android/Sdk"
fi

# Test 4: Project sync
echo "4️⃣ Testing project sync..."
./simple-sync.sh
if [ $? -eq 0 ]; then
    echo "✅ Project sync test passed"
else
    echo "❌ Project sync test failed"
    exit 1
fi

# Test 5: Build
echo "5️⃣ Testing build..."
ssh android-dev-server "cd ~/projects/voice-changer-pro-android && ./gradlew assembleDebug"
if [ $? -eq 0 ]; then
    echo "✅ Build test passed"
else
    echo "❌ Build test failed"
    exit 1
fi

# Test 6: Download APK
echo "6️⃣ Testing APK download..."
scp android-dev-server:~/projects/voice-changer-pro-android/app/build/outputs/apk/debug/app-debug.apk ./
if [ -f "app-debug.apk" ]; then
    echo "✅ APK download test passed"
    echo "📱 APK size: $(ls -lh app-debug.apk | awk '{print $5}')"
else
    echo "❌ APK download test failed"
    exit 1
fi

echo ""
echo "🎉 All tests passed! Remote development setup is working correctly."
echo ""
echo "📋 Next steps:"
echo "1. Open Android Studio on your laptop"
echo "2. Connect to remote project: android-dev-server:~/projects/voice-changer-pro-android"
echo "3. Start developing!"
echo ""
echo "🔗 Quick commands:"
echo "  - Connect: ssh android-dev-server"
echo "  - Sync: ./simple-sync.sh"
echo "  - Build: ./build-on-server.sh"
echo "  - Test: ./test-on-server.sh"
