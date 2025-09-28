#!/bin/bash
echo "🎮 Starting emulator on server..."

# Start emulator on server
ssh android-dev-server "~/emulator-control.sh start"

echo "✅ Emulator started on server!"
echo "📱 To view emulator, use X11 forwarding or VNC"
