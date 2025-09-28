#!/bin/bash

# Voice Changer Pro - Remote Android Studio Setup Script
# This script configures Android Studio on your laptop for remote development

set -e

echo "🚀 Setting up Android Studio for remote development..."

# Server details
SERVER_HOST="ec2-100-27-210-173.compute-1.amazonaws.com"
SERVER_USER="ubuntu"
KEY_FILE="my-dev-key.pem"

# Check if key file exists
if [ ! -f "$KEY_FILE" ]; then
    echo "❌ Error: Key file $KEY_FILE not found!"
    echo "Please make sure the key file is in the current directory."
    exit 1
fi

# Set proper permissions for key file
chmod 600 "$KEY_FILE"

echo "📋 Setting up SSH configuration..."

# Create SSH config for easy connection
mkdir -p ~/.ssh
cat >> ~/.ssh/config << EOF

# Android Studio Remote Development
Host android-dev-server
    HostName $SERVER_HOST
    User $SERVER_USER
    IdentityFile $(pwd)/$KEY_FILE
    StrictHostKeyChecking no
    UserKnownHostsFile /dev/null
    ServerAliveInterval 60
    ServerAliveCountMax 3
    LocalForward 8080 localhost:8080
    LocalForward 8081 localhost:8081
    LocalForward 8082 localhost:8082
    LocalForward 8083 localhost:8083
    LocalForward 8084 localhost:8084
    LocalForward 8085 localhost:8085
    LocalForward 8086 localhost:8086
    LocalForward 8087 localhost:8087
    LocalForward 8088 localhost:8088
    LocalForward 8089 localhost:8089
    LocalForward 8090 localhost:8090
    LocalForward 5005 localhost:5005
    LocalForward 5006 localhost:5006
    LocalForward 5007 localhost:5007
    LocalForward 5008 localhost:5008
    LocalForward 5009 localhost:5009
    LocalForward 5010 localhost:5010
    LocalForward 5900 localhost:5900
    LocalForward 5901 localhost:5901
    LocalForward 5902 localhost:5902
    LocalForward 5903 localhost:5903
    LocalForward 5904 localhost:5904
    LocalForward 5905 localhost:5905
    LocalForward 5906 localhost:5906
    LocalForward 5907 localhost:5907
    LocalForward 5908 localhost:5908
    LocalForward 5909 localhost:5909
    LocalForward 5910 localhost:5910
    ForwardX11 yes
    ForwardX11Trusted yes
    Compression yes
    ForwardAgent yes
EOF

echo "✅ SSH configuration created!"

# Test connection
echo "🔗 Testing connection to server..."
if ssh -o ConnectTimeout=10 android-dev-server "echo 'Connection successful!'"; then
    echo "✅ Connection to server successful!"
else
    echo "❌ Failed to connect to server. Please check your network and key file."
    exit 1
fi

echo "📁 Setting up project synchronization..."

# Create project sync script
cat > sync-project-to-server.sh << 'EOF'
#!/bin/bash

# Sync project to server
echo "📤 Syncing project to server..."

# Exclude unnecessary files
rsync -avz --delete \
    --exclude='.git/' \
    --exclude='build/' \
    --exclude='.gradle/' \
    --exclude='local.properties' \
    --exclude='*.iml' \
    --exclude='.idea/' \
    --exclude='app/build/' \
    --exclude='*.apk' \
    --exclude='*.aab' \
    --exclude='my-dev-key.pem' \
    ./ android-dev-server:~/projects/voice-changer-pro-android/

echo "✅ Project synced to server!"
EOF

chmod +x sync-project-to-server.sh

# Create build script
cat > build-on-server.sh << 'EOF'
#!/bin/bash

# Build project on server
echo "🔨 Building project on server..."

ssh android-dev-server "cd ~/projects/voice-changer-pro-android && ./gradlew clean assembleDebug"

echo "📥 Downloading APK from server..."
scp android-dev-server:~/projects/voice-changer-pro-android/app/build/outputs/apk/debug/app-debug.apk ./

echo "✅ Build completed! APK downloaded to current directory."
EOF

chmod +x build-on-server.sh

# Create test script
cat > test-on-server.sh << 'EOF'
#!/bin/bash

# Test project on server
echo "🧪 Running tests on server..."

ssh android-dev-server "cd ~/projects/voice-changer-pro-android && ./gradlew test"

echo "✅ Tests completed!"
EOF

chmod +x test-on-server.sh

# Create Android Studio remote development script
cat > start-android-studio-remote.sh << 'EOF'
#!/bin/bash

# Start Android Studio with remote development
echo "🚀 Starting Android Studio with remote development..."

# Check if Android Studio is installed
if command -v studio &> /dev/null; then
    ANDROID_STUDIO_CMD="studio"
elif command -v android-studio &> /dev/null; then
    ANDROID_STUDIO_CMD="android-studio"
elif [ -f "/Applications/Android Studio.app/Contents/MacOS/studio" ]; then
    ANDROID_STUDIO_CMD="/Applications/Android Studio.app/Contents/MacOS/studio"
elif [ -f "/opt/android-studio/bin/studio.sh" ]; then
    ANDROID_STUDIO_CMD="/opt/android-studio/bin/studio.sh"
else
    echo "❌ Android Studio not found! Please install Android Studio first."
    exit 1
fi

# Start SSH tunnel in background
echo "🔗 Starting SSH tunnel..."
ssh -f -N android-dev-server

# Wait a moment for tunnel to establish
sleep 3

# Start Android Studio
echo "🎯 Starting Android Studio..."
$ANDROID_STUDIO_CMD &

echo "✅ Android Studio started with remote development!"
echo "📋 You can now:"
echo "   - Open the project from the server"
echo "   - Use the remote build tools"
echo "   - Debug remotely"
EOF

chmod +x start-android-studio-remote.sh

# Create VS Code remote development script (alternative)
cat > setup-vscode-remote.sh << 'EOF'
#!/bin/bash

# Setup VS Code for remote development
echo "🔧 Setting up VS Code for remote development..."

# Install Remote-SSH extension if not already installed
if ! code --list-extensions | grep -q "ms-vscode-remote.remote-ssh"; then
    echo "📦 Installing Remote-SSH extension..."
    code --install-extension ms-vscode-remote.remote-ssh
fi

# Install Android extensions
if ! code --list-extensions | grep -q "vscjava.vscode-gradle"; then
    echo "📦 Installing Gradle extension..."
    code --install-extension vscodejava.vscode-gradle
fi

if ! code --list-extensions | grep -q "redhat.java"; then
    echo "📦 Installing Java extension..."
    code --install-extension redhat.java
fi

echo "✅ VS Code setup completed!"
echo "📋 To connect:"
echo "   1. Open VS Code"
echo "   2. Press Ctrl+Shift+P"
echo "   3. Type 'Remote-SSH: Connect to Host'"
echo "   4. Select 'android-dev-server'"
echo "   5. Open the project folder on the server"
EOF

chmod +x setup-vscode-remote.sh

# Create a comprehensive setup guide
cat > REMOTE_DEVELOPMENT_GUIDE.md << 'EOF'
# Voice Changer Pro - Remote Development Guide

## 🚀 Quick Start

### 1. Connect to Server
```bash
ssh android-dev-server
```

### 2. Sync Project
```bash
./sync-project-to-server.sh
```

### 3. Build Project
```bash
./build-on-server.sh
```

### 4. Test Project
```bash
./test-on-server.sh
```

## 📋 Available Scripts

- `sync-project-to-server.sh` - Sync your local changes to server
- `build-on-server.sh` - Build the project on server and download APK
- `test-on-server.sh` - Run tests on server
- `start-android-studio-remote.sh` - Start Android Studio with remote development
- `setup-vscode-remote.sh` - Setup VS Code for remote development

## 🔧 Android Studio Remote Development

### Method 1: SSH + X11 Forwarding
1. Connect to server: `ssh android-dev-server`
2. Start Android Studio: `~/android-studio/bin/studio.sh`
3. Open project: `~/projects/voice-changer-pro-android`

### Method 2: Local Android Studio + Remote Build
1. Open Android Studio locally
2. Open project from server path: `android-dev-server:~/projects/voice-changer-pro-android`
3. Configure remote build tools

## 🖥️ VS Code Remote Development

1. Install Remote-SSH extension
2. Connect to `android-dev-server`
3. Open project folder on server
4. Use integrated terminal for remote commands

## 🔨 Build Commands

### On Server
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test

# Clean build
./gradlew clean
```

### From Local Machine
```bash
# Build on server and download APK
./build-on-server.sh

# Sync changes to server
./sync-project-to-server.sh
```

## 🐛 Debugging

### Remote Debugging
1. Set breakpoints in Android Studio
2. Run app in debug mode
3. Use remote debugging features

### Logs
```bash
# View Android logs
adb logcat

# View Gradle logs
./gradlew --info
```

## 📱 Testing

### Emulator on Server
```bash
# Start emulator
~/Android/Sdk/emulator/emulator -avd Pixel_7_API_34 &

# Install APK
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Real Device
1. Connect device via USB
2. Enable USB debugging
3. Install APK: `adb install app-debug.apk`

## 🔧 Troubleshooting

### Connection Issues
- Check SSH key permissions: `chmod 600 my-dev-key.pem`
- Test connection: `ssh android-dev-server`
- Check server status: `ssh android-dev-server "systemctl status android-studio"`

### Build Issues
- Check Java version: `java -version`
- Check Android SDK: `echo $ANDROID_HOME`
- Clean build: `./gradlew clean`

### Performance Issues
- Increase server resources
- Use SSD storage
- Close unnecessary applications

## 📞 Support

For issues or questions:
1. Check server logs: `ssh android-dev-server "journalctl -u android-studio"`
2. Check build logs: `./gradlew --info`
3. Contact development team
EOF

echo "✅ Remote development setup completed!"
echo ""
echo "📋 Next steps:"
echo "1. Run: ./sync-project-to-server.sh"
echo "2. Run: ./build-on-server.sh"
echo "3. Run: ./start-android-studio-remote.sh"
echo ""
echo "📖 For detailed instructions, see: REMOTE_DEVELOPMENT_GUIDE.md"
echo ""
echo "🔗 To connect to server: ssh android-dev-server"
