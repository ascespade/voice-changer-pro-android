#!/bin/bash
echo "🛑 Stopping emulator on server..."

# Stop emulator on server
ssh android-dev-server "~/emulator-control.sh stop"

echo "✅ Emulator stopped!"
