#!/bin/bash

# Complete server setup for Android development
echo "🚀 Completing server setup for Android development..."

# Install Java
echo "☕ Installing Java..."
ssh android-dev-server "sudo apt update && sudo apt install -y openjdk-17-jdk"

# Set JAVA_HOME
echo "🔧 Setting JAVA_HOME..."
ssh android-dev-server "echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc"
ssh android-dev-server "echo 'export PATH=\$JAVA_HOME/bin:\$PATH' >> ~/.bashrc"

# Install Android SDK
echo "📱 Installing Android SDK..."
ssh android-dev-server "mkdir -p ~/Android/Sdk"
ssh android-dev-server "export ANDROID_HOME=~/Android/Sdk && export ANDROID_SDK_ROOT=~/Android/Sdk"

# Download Android command line tools
echo "⬇️ Downloading Android command line tools..."
ssh android-dev-server "cd ~/Android/Sdk && wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
ssh android-dev-server "cd ~/Android/Sdk && unzip -q commandlinetools-linux-11076708_latest.zip"
ssh android-dev-server "cd ~/Android/Sdk && mkdir -p cmdline-tools/latest && mv cmdline-tools/* cmdline-tools/latest/ 2>/dev/null || true"
ssh android-dev-server "cd ~/Android/Sdk && rm commandlinetools-linux-11076708_latest.zip"

# Accept licenses and install SDK components
echo "📋 Installing Android SDK components..."
ssh android-dev-server "export ANDROID_HOME=~/Android/Sdk && export ANDROID_SDK_ROOT=~/Android/Sdk && yes | ~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager --licenses"
ssh android-dev-server "export ANDROID_HOME=~/Android/Sdk && export ANDROID_SDK_ROOT=~/Android/Sdk && ~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager 'platform-tools' 'platforms;android-34' 'build-tools;34.0.0'"

# Set environment variables
echo "🔧 Setting environment variables..."
ssh android-dev-server "echo 'export ANDROID_HOME=~/Android/Sdk' >> ~/.bashrc"
ssh android-dev-server "echo 'export ANDROID_SDK_ROOT=~/Android/Sdk' >> ~/.bashrc"
ssh android-dev-server "echo 'export PATH=\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$PATH' >> ~/.bashrc"

echo "✅ Server setup completed!"
echo "🔗 To connect: ssh android-dev-server"
echo "🔨 To build: ssh android-dev-server 'cd ~/projects/voice-changer-pro-android && ./gradlew assembleDebug'"
