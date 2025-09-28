#!/bin/bash

# Fix server setup for Android development
echo "🔧 Fixing server setup for Android development..."

# Install basic packages
echo "📦 Installing basic packages..."
ssh android-dev-server "sudo apt update && sudo apt install -y unzip wget curl build-essential libc6-dev lib32stdc++6 lib32z1 lib32z1-dev libc6-dev-i386 lib32gcc-s1 libncurses5-dev zlib1g-dev libx11-dev libxext-dev libxrender-dev libxtst-dev libxi-dev libxrandr-dev libxss-dev libasound2t64 libasound2-dev libnss3-dev libatk-bridge2.0-dev libdrm2 libxcomposite1 libxdamage1 libxrandr2 libgbm1 libxss1 libatspi2.0-0 libgtk-3-0 libgdk-pixbuf2.0-0 libx11-xcb1 libxcb-dri3-0 xvfb"

# Set environment variables
echo "🔧 Setting environment variables..."
ssh android-dev-server "echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc"
ssh android-dev-server "echo 'export PATH=\$JAVA_HOME/bin:\$PATH' >> ~/.bashrc"
ssh android-dev-server "echo 'export ANDROID_HOME=~/Android/Sdk' >> ~/.bashrc"
ssh android-dev-server "echo 'export ANDROID_SDK_ROOT=~/Android/Sdk' >> ~/.bashrc"
ssh android-dev-server "echo 'export PATH=\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$ANDROID_HOME/emulator:\$PATH' >> ~/.bashrc"

# Download and install Android command line tools
echo "⬇️ Downloading Android command line tools..."
ssh android-dev-server "cd ~/Android/Sdk && wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
ssh android-dev-server "cd ~/Android/Sdk && unzip -q commandlinetools-linux-11076708_latest.zip"
ssh android-dev-server "cd ~/Android/Sdk && mkdir -p cmdline-tools/latest && mv cmdline-tools/* cmdline-tools/latest/ 2>/dev/null || true"
ssh android-dev-server "cd ~/Android/Sdk && rm commandlinetools-linux-11076708_latest.zip"

# Accept licenses and install SDK components
echo "📋 Installing Android SDK components..."
ssh android-dev-server "export ANDROID_HOME=~/Android/Sdk && export ANDROID_SDK_ROOT=~/Android/Sdk && yes | ~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager --licenses"
ssh android-dev-server "export ANDROID_HOME=~/Android/Sdk && export ANDROID_SDK_ROOT=~/Android/Sdk && ~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager 'platform-tools' 'platforms;android-34' 'build-tools;34.0.0' 'emulator' 'system-images;android-34;google_apis;x86_64'"

# Create AVD
echo "📱 Creating Android Virtual Device..."
ssh android-dev-server "export ANDROID_HOME=~/Android/Sdk && export ANDROID_SDK_ROOT=~/Android/Sdk && echo 'no' | ~/Android/Sdk/cmdline-tools/latest/bin/avdmanager create avd -n Pixel_7_API_34 -k 'system-images;android-34;google_apis;x86_64' -d 'pixel_7'"

# Create project directory
echo "📁 Creating project directory..."
ssh android-dev-server "mkdir -p ~/projects/voice-changer-pro-android"

# Sync project
echo "📤 Syncing project to server..."
./simple-sync.sh

echo "✅ Server setup completed!"
echo "🔗 To connect: ssh android-dev-server"
echo "📊 To check status: ssh android-dev-server '~/android-status.sh'"
