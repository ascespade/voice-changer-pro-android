#!/bin/bash

# Sync project to server
echo "📤 Syncing project to server..."

# Create projects directory on server if it doesn't exist
ssh android-dev-server "mkdir -p ~/projects"

# Create tar archive of project files (excluding unnecessary files)
tar --exclude='.git' \
    --exclude='build' \
    --exclude='.gradle' \
    --exclude='local.properties' \
    --exclude='*.iml' \
    --exclude='.idea' \
    --exclude='app/build' \
    --exclude='*.apk' \
    --exclude='*.aab' \
    --exclude='my-dev-key.pem' \
    --exclude='setup-*.sh' \
    --exclude='sync-*.sh' \
    --exclude='build-*.sh' \
    --exclude='test-*.sh' \
    --exclude='start-*.sh' \
    --exclude='REMOTE_DEVELOPMENT_GUIDE.md' \
    -czf project.tar.gz .

# Upload to server
scp project.tar.gz android-dev-server:~/projects/

# Extract on server
ssh android-dev-server "cd ~/projects && tar -xzf project.tar.gz -C voice-changer-pro-android/ && rm project.tar.gz"

# Clean up local tar
rm project.tar.gz

echo "✅ Project synced to server!"
