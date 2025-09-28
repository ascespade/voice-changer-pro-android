#!/bin/bash
echo "🔗 Connecting to server..."

echo "📋 Available commands on server:"
echo "  - ~/emulator-control.sh {start|stop|status|list}"
echo "  - ~/adb-control.sh {devices|install|uninstall|shell|logcat|reboot}"
echo "  - ~/build-and-test.sh {build|test|install|run}"
echo "  - ~/android-status.sh"
echo ""

ssh android-dev-server
