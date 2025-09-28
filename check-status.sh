#!/bin/bash
echo "📊 Checking server status..."

# Check server status
ssh android-dev-server "~/android-status.sh"

echo ""
echo "🔗 To connect to server: ssh android-dev-server"
echo "🎮 To start emulator: ./start-emulator.sh"
echo "🔨 To build project: ./build-project.sh"
echo "📱 To install app: ./install-app.sh"
