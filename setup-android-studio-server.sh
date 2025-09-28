#!/bin/bash

# Voice Changer Pro - Android Studio Server Setup Script
# This script sets up Android Studio on the server for remote development

set -e

echo "🚀 Setting up Android Studio on server for remote development..."

# Update system packages
echo "📦 Updating system packages..."
sudo apt update && sudo apt upgrade -y

# Install required packages
echo "🔧 Installing required packages..."
sudo apt install -y \
    openjdk-17-jdk \
    openjdk-17-jre \
    wget \
    unzip \
    curl \
    git \
    build-essential \
    libc6-dev \
    lib32stdc++6 \
    lib32z1 \
    lib32z1-dev \
    libc6-dev-i386 \
    lib32gcc1 \
    lib32ncurses5 \
    lib32stdc++6 \
    libc6-i386 \
    libncurses5-dev \
    libncurses5-dev:i386 \
    libstdc++6:i386 \
    zlib1g-dev \
    zlib1g-dev:i386 \
    libx11-dev \
    libx11-dev:i386 \
    libxext-dev \
    libxext-dev:i386 \
    libxrender-dev \
    libxrender-dev:i386 \
    libxtst-dev \
    libxtst-dev:i386 \
    libxi-dev \
    libxi-dev:i386 \
    libxrandr-dev \
    libxrandr-dev:i386 \
    libxss-dev \
    libxss-dev:i386 \
    libgconf-2-4 \
    libxss1 \
    libasound2-dev \
    libasound2-dev:i386 \
    libnss3-dev \
    libnss3-dev:i386 \
    libatk-bridge2.0-dev \
    libdrm2 \
    libxcomposite1 \
    libxdamage1 \
    libxrandr2 \
    libgbm1 \
    libxss1 \
    libasound2 \
    libatspi2.0-0 \
    libgtk-3-0 \
    libgdk-pixbuf2.0-0 \
    libx11-xcb1 \
    libxcb-dri3-0 \
    libdrm2 \
    libxcomposite1 \
    libxdamage1 \
    libxrandr2 \
    libgbm1 \
    libxss1 \
    libasound2 \
    libatspi2.0-0 \
    libgtk-3-0 \
    libgdk-pixbuf2.0-0 \
    libx11-xcb1 \
    libxcb-dri3-0

# Set JAVA_HOME
echo "☕ Setting up Java environment..."
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc

# Create Android SDK directory
echo "📱 Setting up Android SDK..."
mkdir -p ~/Android/Sdk
export ANDROID_HOME=~/Android/Sdk
export ANDROID_SDK_ROOT=~/Android/Sdk
echo 'export ANDROID_HOME=~/Android/Sdk' >> ~/.bashrc
echo 'export ANDROID_SDK_ROOT=~/Android/Sdk' >> ~/.bashrc
echo 'export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH' >> ~/.bashrc

# Download and install Android command line tools
echo "⬇️ Downloading Android command line tools..."
cd ~/Android/Sdk
wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q commandlinetools-linux-11076708_latest.zip
mkdir -p cmdline-tools/latest
mv cmdline-tools/* cmdline-tools/latest/ 2>/dev/null || true
rm commandlinetools-linux-11076708_latest.zip

# Accept Android SDK licenses
echo "📋 Accepting Android SDK licenses..."
yes | ~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager --licenses

# Install required SDK components
echo "🔧 Installing Android SDK components..."
~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager \
    "platform-tools" \
    "platforms;android-34" \
    "platforms;android-33" \
    "platforms;android-32" \
    "platforms;android-31" \
    "platforms;android-30" \
    "platforms;android-29" \
    "platforms;android-28" \
    "platforms;android-27" \
    "platforms;android-26" \
    "platforms;android-25" \
    "platforms;android-24" \
    "build-tools;34.0.0" \
    "build-tools;33.0.2" \
    "build-tools;32.0.0" \
    "build-tools;31.0.0" \
    "build-tools;30.0.3" \
    "build-tools;29.0.3" \
    "build-tools;28.0.3" \
    "build-tools;27.0.3" \
    "build-tools;26.0.3" \
    "build-tools;25.0.3" \
    "build-tools;24.0.3" \
    "ndk;25.2.9519653" \
    "cmake;3.22.1" \
    "emulator" \
    "system-images;android-34;google_apis;x86_64" \
    "system-images;android-33;google_apis;x86_64"

# Download and install Android Studio
echo "⬇️ Downloading Android Studio..."
cd ~
wget -q https://redirector.gvt1.com/edgedl/android/studio/ide-zips/2023.3.1.18/android-studio-2023.3.1.18-linux.tar.gz
tar -xzf android-studio-2023.3.1.18-linux.tar.gz
rm android-studio-2023.3.1.18-linux.tar.gz

# Create Android Studio desktop entry
echo "🖥️ Creating Android Studio desktop entry..."
mkdir -p ~/.local/share/applications
cat > ~/.local/share/applications/android-studio.desktop << EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=Android Studio
Comment=Android Studio IDE
Exec=~/android-studio/bin/studio.sh
Icon=~/android-studio/bin/studio.png
Terminal=false
Categories=Development;IDE;
EOF

# Set up Gradle
echo "🔨 Setting up Gradle..."
mkdir -p ~/.gradle
echo "org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8" > ~/.gradle/gradle.properties
echo "org.gradle.parallel=true" >> ~/.gradle/gradle.properties
echo "org.gradle.caching=true" >> ~/.gradle/gradle.properties
echo "android.useAndroidX=true" >> ~/.gradle/gradle.properties
echo "android.enableJetifier=true" >> ~/.gradle/gradle.properties

# Create project directory
echo "📁 Setting up project directory..."
mkdir -p ~/projects
cd ~/projects

# Clone or copy the project
if [ ! -d "voice-changer-pro-android" ]; then
    echo "📂 Project directory not found. Please copy your project to ~/projects/voice-changer-pro-android"
fi

# Set up SSH for remote development
echo "🔐 Setting up SSH for remote development..."
mkdir -p ~/.ssh
chmod 700 ~/.ssh

# Create a script to start Android Studio in headless mode
echo "🤖 Creating Android Studio headless startup script..."
cat > ~/start-android-studio-headless.sh << 'EOF'
#!/bin/bash

# Start Android Studio in headless mode for remote development
export DISPLAY=:99
export ANDROID_HOME=~/Android/Sdk
export ANDROID_SDK_ROOT=~/Android/Sdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# Start Xvfb for headless display
Xvfb :99 -screen 0 1920x1080x24 &
XVFB_PID=$!

# Wait for Xvfb to start
sleep 5

# Start Android Studio
~/android-studio/bin/studio.sh &

# Keep the script running
wait $XVFB_PID
EOF

chmod +x ~/start-android-studio-headless.sh

# Create a script to build the project
echo "🔨 Creating build script..."
cat > ~/build-android-project.sh << 'EOF'
#!/bin/bash

# Build Android project script
cd ~/projects/voice-changer-pro-android

# Set environment variables
export ANDROID_HOME=~/Android/Sdk
export ANDROID_SDK_ROOT=~/Android/Sdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# Clean and build
./gradlew clean
./gradlew assembleDebug

echo "✅ Build completed! APK location: app/build/outputs/apk/debug/app-debug.apk"
EOF

chmod +x ~/build-android-project.sh

# Create a script to run tests
echo "🧪 Creating test script..."
cat > ~/test-android-project.sh << 'EOF'
#!/bin/bash

# Test Android project script
cd ~/projects/voice-changer-pro-android

# Set environment variables
export ANDROID_HOME=~/Android/Sdk
export ANDROID_SDK_ROOT=~/Android/Sdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# Run tests
./gradlew test
./gradlew connectedAndroidTest

echo "✅ Tests completed!"
EOF

chmod +x ~/test-android-project.sh

# Install Xvfb for headless display
echo "🖥️ Installing Xvfb for headless display..."
sudo apt install -y xvfb

# Create systemd service for Android Studio
echo "⚙️ Creating systemd service for Android Studio..."
sudo tee /etc/systemd/system/android-studio.service > /dev/null << EOF
[Unit]
Description=Android Studio Headless Service
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu
Environment=DISPLAY=:99
Environment=ANDROID_HOME=/home/ubuntu/Android/Sdk
Environment=ANDROID_SDK_ROOT=/home/ubuntu/Android/Sdk
Environment=JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ExecStart=/home/ubuntu/start-android-studio-headless.sh
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Reload systemd and enable service
sudo systemctl daemon-reload
sudo systemctl enable android-studio.service

echo "✅ Android Studio server setup completed!"
echo ""
echo "📋 Next steps:"
echo "1. Copy your project to ~/projects/voice-changer-pro-android"
echo "2. Start the service: sudo systemctl start android-studio.service"
echo "3. Check status: sudo systemctl status android-studio.service"
echo "4. Build project: ~/build-android-project.sh"
echo "5. Test project: ~/test-android-project.sh"
echo ""
echo "🔗 For remote development, you'll need to:"
echo "1. Set up SSH tunneling for Android Studio"
echo "2. Configure remote development in your local Android Studio"
echo "3. Use X11 forwarding for GUI access"
