#!/bin/bash

# Create helpful scripts for laptop development
echo "📱 Creating laptop development scripts..."

# Sync project script
cat > sync-project.sh << 'EOF'
#!/bin/bash
echo "📤 Syncing project to server..."

# Create projects directory on server if it doesn't exist
ssh android-dev-server "mkdir -p ~/projects/voice-changer-pro-android"

# Sync essential files
echo "📁 Syncing source files..."
scp -r app/src android-dev-server:~/projects/voice-changer-pro-android/app/
scp app/build.gradle android-dev-server:~/projects/voice-changer-pro-android/app/
scp app/proguard-rules.pro android-dev-server:~/projects/voice-changer-pro-android/app/

echo "📁 Syncing project files..."
scp build.gradle android-dev-server:~/projects/voice-changer-pro-android/
scp settings.gradle android-dev-server:~/projects/voice-changer-pro-android/
scp gradle.properties android-dev-server:~/projects/voice-changer-pro-android/
scp gradlew android-dev-server:~/projects/voice-changer-pro-android/
scp -r gradle android-dev-server:~/projects/voice-changer-pro-android/

echo "✅ Project synced to server!"
EOF

# Build project script
cat > build-project.sh << 'EOF'
#!/bin/bash
echo "🔨 Building project on server..."

# Sync project first
./sync-project.sh

# Build on server
ssh android-dev-server "cd ~/projects/voice-changer-pro-android && ./gradlew clean assembleDebug"

# Download APK
echo "📥 Downloading APK from server..."
scp android-dev-server:~/projects/voice-changer-pro-android/app/build/outputs/apk/debug/app-debug.apk ./

echo "✅ Build completed! APK downloaded to current directory."
echo "📱 APK location: $(pwd)/app-debug.apk"
EOF

# Test project script
cat > test-project.sh << 'EOF'
#!/bin/bash
echo "🧪 Running tests on server..."

# Sync project first
./sync-project.sh

# Run tests on server
ssh android-dev-server "cd ~/projects/voice-changer-pro-android && ./gradlew test"

echo "✅ Tests completed!"
EOF

# Start emulator script
cat > start-emulator.sh << 'EOF'
#!/bin/bash
echo "🎮 Starting emulator on server..."

# Start emulator on server
ssh android-dev-server "~/emulator-control.sh start"

echo "✅ Emulator started on server!"
echo "📱 To view emulator, use X11 forwarding or VNC"
EOF

# Stop emulator script
cat > stop-emulator.sh << 'EOF'
#!/bin/bash
echo "🛑 Stopping emulator on server..."

# Stop emulator on server
ssh android-dev-server "~/emulator-control.sh stop"

echo "✅ Emulator stopped!"
EOF

# Install app script
cat > install-app.sh << 'EOF'
#!/bin/bash
echo "📱 Installing app on emulator..."

# Build and install on server
ssh android-dev-server "cd ~/projects/voice-changer-pro-android && ./gradlew clean assembleDebug installDebug"

echo "✅ App installed on emulator!"
EOF

# Check status script
cat > check-status.sh << 'EOF'
#!/bin/bash
echo "📊 Checking server status..."

# Check server status
ssh android-dev-server "~/android-status.sh"

echo ""
echo "🔗 To connect to server: ssh android-dev-server"
echo "🎮 To start emulator: ./start-emulator.sh"
echo "🔨 To build project: ./build-project.sh"
echo "📱 To install app: ./install-app.sh"
EOF

# Connect to server script
cat > connect-server.sh << 'EOF'
#!/bin/bash
echo "🔗 Connecting to server..."

echo "📋 Available commands on server:"
echo "  - ~/emulator-control.sh {start|stop|status|list}"
echo "  - ~/adb-control.sh {devices|install|uninstall|shell|logcat|reboot}"
echo "  - ~/build-and-test.sh {build|test|install|run}"
echo "  - ~/android-status.sh"
echo ""

ssh android-dev-server
EOF

# Quick development script
cat > quick-dev.sh << 'EOF'
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
EOF

# Make all scripts executable
chmod +x *.sh

echo "✅ Laptop development scripts created!"
echo ""
echo "📋 Available scripts:"
echo "  - ./sync-project.sh      # Sync project to server"
echo "  - ./build-project.sh     # Build project on server"
echo "  - ./test-project.sh      # Run tests on server"
echo "  - ./start-emulator.sh    # Start emulator on server"
echo "  - ./stop-emulator.sh     # Stop emulator on server"
echo "  - ./install-app.sh       # Install app on emulator"
echo "  - ./check-status.sh      # Check server status"
echo "  - ./connect-server.sh    # Connect to server"
echo "  - ./quick-dev.sh         # Quick development workflow"
echo ""
echo "🎯 Quick start:"
echo "  1. ./check-status.sh"
echo "  2. ./start-emulator.sh"
echo "  3. ./quick-dev.sh"
