#!/bin/bash

# LibreCare Android SDK Setup Script for Codespaces/Devcontainers
# This script is run by devcontainer.json postCreateCommand

set -e

ANDROID_HOME="/opt/android-sdk"
CMDLINE_TOOLS_VERSION="12.0"
BUILD_TOOLS_VERSION="35.0.0"
PLATFORM_VERSION="35"

# Check if already set up
if [ -d "$ANDROID_HOME/platforms/android-$PLATFORM_VERSION" ] && \
   [ -d "$ANDROID_HOME/build-tools/$BUILD_TOOLS_VERSION" ] && \
   [ -d "$ANDROID_HOME/platform-tools" ]; then
  echo "Android SDK already configured."
  exit 0
fi

echo "=== Setting up Android SDK ==="
echo "ANDROID_HOME: $ANDROID_HOME"
echo "Build Tools: $BUILD_TOOLS_VERSION"
echo "Platform: android-$PLATFORM_VERSION"

# Create directories
mkdir -p "$ANDROID_HOME/cmdline-tools"
cd "$ANDROID_HOME/cmdline-tools"

# Download command-line tools if not present
if [ ! -d "latest" ]; then
  echo "Downloading Android SDK Command-line Tools..."
  CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"

  if ! curl -sS -f -o cmdlinetools.zip "$CMDLINE_TOOLS_URL"; then
    echo "Failed to download Android SDK Command-line Tools"
    exit 1
  fi

  unzip -q cmdlinetools.zip
  rm -f cmdlinetools.zip

  # Rename to 'latest' (command-line tools expect this structure)
  if [ -d "cmdline-tools" ]; then
    mv cmdline-tools latest
  fi
fi

# Accept licenses
echo "Accepting Android licenses..."
yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null 2>&1 || true

# Install SDK components
echo "Installing Android Platform $PLATFORM_VERSION..."
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "platforms;android-$PLATFORM_VERSION"

echo "Installing Build Tools $BUILD_TOOLS_VERSION..."
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "build-tools;$BUILD_TOOLS_VERSION"

echo "Installing Platform Tools..."
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "platform-tools"

# Verify installation
echo "=== Verifying installation ==="
if [ -d "$ANDROID_HOME/platforms/android-$PLATFORM_VERSION" ]; then
  echo "✓ Platform android-$PLATFORM_VERSION installed"
else
  echo "✗ Platform android-$PLATFORM_VERSION NOT found"
  exit 1
fi

if [ -d "$ANDROID_HOME/build-tools/$BUILD_TOOLS_VERSION" ]; then
  echo "✓ Build Tools $BUILD_TOOLS_VERSION installed"
else
  echo "✗ Build Tools $BUILD_TOOLS_VERSION NOT found"
  exit 1
fi

if [ -d "$ANDROID_HOME/platform-tools" ]; then
  echo "✓ Platform Tools installed"
else
  echo "✗ Platform Tools NOT found"
  exit 1
fi

echo ""
echo "=== Android SDK setup complete ==="
echo "ANDROID_HOME: $ANDROID_HOME"
echo "Run 'java -version' to verify JDK 17"
echo "Run './gradlew tasks' to verify Gradle setup"

