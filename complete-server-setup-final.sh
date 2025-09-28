#!/bin/bash

# Complete server setup for Android development with Emulator
echo "🚀 Completing final server setup for Android development..."

# Install missing packages
echo "📦 Installing missing packages..."
ssh android-dev-server "sudo apt update && sudo apt install -y unzip wget curl build-essential libc6-dev lib32stdc++6 lib32z1 lib32z1-dev libc6-dev-i386 lib32gcc-s1 libncurses5-dev libncurses5-dev:i386 libstdc++6:i386 zlib1g-dev zlib1g-dev:i386 libx11-dev libx11-dev:i386 libxext-dev libxext-dev:i386 libxrender-dev libxrender-dev:i386 libxtst-dev libxtst-dev:i386 libxi-dev libxi-dev:i386 libxrandr-dev libxrandr-dev:i386 libxss-dev libxss-dev:i386 libasound2t64 libasound2-dev libasound2-dev:i386 libnss3-dev libnss3-dev:i386 libatk-bridge2.0-dev libdrm2 libxcomposite1 libxdamage1 libxrandr2 libgbm1 libxss1 libasound2t64 libatspi2.0-0 libgtk-3-0 libgdk-pixbuf2.0-0 libx11-xcb1 libxcb-dri3-0 xvfb"

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
ssh android-dev-server "export ANDROID_HOME=~/Android/Sdk && export ANDROID_SDK_ROOT=~/Android/Sdk && ~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager 'platform-tools' 'platforms;android-34' 'platforms;android-33' 'platforms;android-32' 'platforms;android-31' 'platforms;android-30' 'platforms;android-29' 'platforms;android-28' 'platforms;android-27' 'platforms;android-26' 'platforms;android-25' 'platforms;android-24' 'build-tools;34.0.0' 'build-tools;33.0.2' 'build-tools;32.0.0' 'build-tools;31.0.0' 'build-tools;30.0.3' 'build-tools;29.0.3' 'build-tools;28.0.3' 'build-tools;27.0.3' 'build-tools;26.0.3' 'build-tools;25.0.3' 'build-tools;24.0.3' 'ndk;25.2.9519653' 'cmake;3.22.1' 'emulator' 'system-images;android-34;google_apis;x86_64' 'system-images;android-33;google_apis;x86_64' 'system-images;android-32;google_apis;x86_64' 'system-images;android-31;google_apis;x86_64' 'system-images;android-30;google_apis;x86_64' 'system-images;android-29;google_apis;x86_64' 'system-images;android-28;google_apis;x86_64' 'system-images;android-27;google_apis;x86_64' 'system-images;android-26;google_apis;x86_64' 'system-images;android-25;google_apis;x86_64' 'system-images;android-24;google_apis;x86_64'"

# Create AVD
echo "📱 Creating Android Virtual Device..."
ssh android-dev-server "export ANDROID_HOME=~/Android/Sdk && export ANDROID_SDK_ROOT=~/Android/Sdk && echo 'no' | ~/Android/Sdk/cmdline-tools/latest/bin/avdmanager create avd -n Pixel_7_API_34 -k 'system-images;android-34;google_apis;x86_64' -d 'pixel_7'"

# Create emulator startup script
echo "🤖 Creating emulator startup script..."
ssh android-dev-server "cat > ~/start-emulator.sh << 'EOF'
#!/bin/bash
export ANDROID_HOME=~/Android/Sdk
export ANDROID_SDK_ROOT=~/Android/Sdk
export DISPLAY=:99

# Start Xvfb for headless display
Xvfb :99 -screen 0 1920x1080x24 &
XVFB_PID=\$!

# Wait for Xvfb to start
sleep 5

# Start emulator
~/Android/Sdk/emulator/emulator -avd Pixel_7_API_34 -no-audio -no-window -gpu swiftshader_indirect &
EMULATOR_PID=\$!

echo \"Emulator started with PID: \$EMULATOR_PID\"
echo \"Xvfb started with PID: \$XVFB_PID\"

# Keep script running
wait \$EMULATOR_PID
EOF"

ssh android-dev-server "chmod +x ~/start-emulator.sh"

# Create emulator control script
echo "🎮 Creating emulator control script..."
ssh android-dev-server "cat > ~/emulator-control.sh << 'EOF'
#!/bin/bash

case \$1 in
    start)
        echo \"Starting emulator...\"
        ~/start-emulator.sh &
        echo \"Emulator started in background\"
        ;;
    stop)
        echo \"Stopping emulator...\"
        pkill -f emulator
        pkill -f Xvfb
        echo \"Emulator stopped\"
        ;;
    status)
        if pgrep -f emulator > /dev/null; then
            echo \"Emulator is running\"
        else
            echo \"Emulator is not running\"
        fi
        ;;
    list)
        echo \"Available AVDs:\"
        ~/Android/Sdk/cmdline-tools/latest/bin/avdmanager list avd
        ;;
    *)
        echo \"Usage: \$0 {start|stop|status|list}\"
        ;;
esac
EOF"

ssh android-dev-server "chmod +x ~/emulator-control.sh"

# Create ADB control script
echo "🔧 Creating ADB control script..."
ssh android-dev-server "cat > ~/adb-control.sh << 'EOF'
#!/bin/bash

export ANDROID_HOME=~/Android/Sdk
export PATH=\$ANDROID_HOME/platform-tools:\$PATH

case \$1 in
    devices)
        adb devices
        ;;
    install)
        if [ -z \"\$2\" ]; then
            echo \"Usage: \$0 install <apk_file>\"
            exit 1
        fi
        adb install \"\$2\"
        ;;
    uninstall)
        if [ -z \"\$2\" ]; then
            echo \"Usage: \$0 uninstall <package_name>\"
            exit 1
        fi
        adb uninstall \"\$2\"
        ;;
    shell)
        adb shell
        ;;
    logcat)
        adb logcat
        ;;
    reboot)
        adb reboot
        ;;
    *)
        echo \"Usage: \$0 {devices|install|uninstall|shell|logcat|reboot}\"
        ;;
esac
EOF"

ssh android-dev-server "chmod +x ~/adb-control.sh"

# Create build and test script
echo "🔨 Creating build and test script..."
ssh android-dev-server "cat > ~/build-and-test.sh << 'EOF'
#!/bin/bash

cd ~/projects/voice-changer-pro-android

# Set environment variables
export ANDROID_HOME=~/Android/Sdk
export ANDROID_SDK_ROOT=~/Android/Sdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

case \$1 in
    build)
        echo \"Building project...\"
        ./gradlew clean assembleDebug
        echo \"Build completed!\"
        ;;
    test)
        echo \"Running tests...\"
        ./gradlew test
        echo \"Tests completed!\"
        ;;
    install)
        echo \"Installing APK...\"
        ./gradlew installDebug
        echo \"APK installed!\"
        ;;
    run)
        echo \"Building and installing...\"
        ./gradlew clean assembleDebug installDebug
        echo \"App built and installed!\"
        ;;
    *)
        echo \"Usage: \$0 {build|test|install|run}\"
        ;;
esac
EOF"

ssh android-dev-server "chmod +x ~/build-and-test.sh"

# Create systemd service for emulator
echo "⚙️ Creating systemd service for emulator..."
ssh android-dev-server "sudo tee /etc/systemd/system/android-emulator.service > /dev/null << 'EOF'
[Unit]
Description=Android Emulator Service
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu
Environment=DISPLAY=:99
Environment=ANDROID_HOME=/home/ubuntu/Android/Sdk
Environment=ANDROID_SDK_ROOT=/home/ubuntu/Android/Sdk
Environment=JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ExecStart=/home/ubuntu/start-emulator.sh
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF"

# Reload systemd and enable service
ssh android-dev-server "sudo systemctl daemon-reload"
ssh android-dev-server "sudo systemctl enable android-emulator.service"

# Create desktop entry for emulator
echo "🖥️ Creating desktop entry for emulator..."
ssh android-dev-server "cat > ~/.local/share/applications/android-emulator.desktop << 'EOF'
[Desktop Entry]
Version=1.0
Type=Application
Name=Android Emulator
Comment=Android Virtual Device Manager
Exec=~/emulator-control.sh start
Icon=~/Android/Sdk/emulator/lib/images/emulator_icon.png
Terminal=false
Categories=Development;Emulator;
EOF"

# Create comprehensive status script
echo "📊 Creating status script..."
ssh android-dev-server "cat > ~/android-status.sh << 'EOF'
#!/bin/bash

echo \"=== Android Development Environment Status ===\"
echo \"\"

echo \"Java Version:\"
java -version
echo \"\"

echo \"Android SDK Location:\"
echo \"ANDROID_HOME: \$ANDROID_HOME\"
echo \"ANDROID_SDK_ROOT: \$ANDROID_SDK_ROOT\"
echo \"\"

echo \"Available AVDs:\"
~/Android/Sdk/cmdline-tools/latest/bin/avdmanager list avd
echo \"\"

echo \"Emulator Status:\"
if pgrep -f emulator > /dev/null; then
    echo \"✅ Emulator is running\"
else
    echo \"❌ Emulator is not running\"
fi
echo \"\"

echo \"ADB Devices:\"
~/Android/Sdk/platform-tools/adb devices
echo \"\"

echo \"Project Status:\"
if [ -d \"~/projects/voice-changer-pro-android\" ]; then
    echo \"✅ Project directory exists\"
    echo \"Project size: \$(du -sh ~/projects/voice-changer-pro-android | cut -f1)\"
else
    echo \"❌ Project directory not found\"
fi
echo \"\"

echo \"Available Commands:\"
echo \"  ~/emulator-control.sh {start|stop|status|list}\"
echo \"  ~/adb-control.sh {devices|install|uninstall|shell|logcat|reboot}\"
echo \"  ~/build-and-test.sh {build|test|install|run}\"
echo \"  ~/android-status.sh\"
EOF"

ssh android-dev-server "chmod +x ~/android-status.sh"

echo "✅ Server setup completed!"
echo ""
echo "📋 Available commands on server:"
echo "  - ~/emulator-control.sh start    # Start emulator"
echo "  - ~/emulator-control.sh stop     # Stop emulator"
echo "  - ~/emulator-control.sh status   # Check emulator status"
echo "  - ~/adb-control.sh devices       # List connected devices"
echo "  - ~/adb-control.sh install <apk> # Install APK"
echo "  - ~/build-and-test.sh build      # Build project"
echo "  - ~/build-and-test.sh run        # Build and install"
echo "  - ~/android-status.sh            # Check overall status"
echo ""
echo "🔗 To connect: ssh android-dev-server"
echo "🎮 To start emulator: ssh android-dev-server '~/emulator-control.sh start'"
echo "📱 To check status: ssh android-dev-server '~/android-status.sh'"
