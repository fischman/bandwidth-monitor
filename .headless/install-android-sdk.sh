#!/usr/bin/env bash
#
# Install Android SDK command-line tools for headless builds.
# Run once per VM/container.

set -euo pipefail

sudo apt update
sudo apt install -y --no-install-recommends default-jdk-headless

ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
echo "Installing Android SDK to $ANDROID_HOME"

mkdir -p "$ANDROID_HOME"
cd "$ANDROID_HOME"

if [ ! -f cmdline-tools.zip ]; then
  curl -fSL -o cmdline-tools.zip \
    https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
fi

if [ ! -d cmdline-tools/latest/bin ]; then
  rm -rf cmdline-tools-tmp cmdline-tools
  unzip -qo cmdline-tools.zip
  mv cmdline-tools cmdline-tools-tmp
  mkdir -p cmdline-tools
  mv cmdline-tools-tmp cmdline-tools/latest
fi

yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses > /dev/null 2>&1 || true

"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
  "platform-tools" \
  "platforms;android-37.0" \
  "build-tools;37.0.0"

echo ""
echo "Done. Adding ANDROID_HOME and its PATH to ~/.bashrc"
echo "export ANDROID_HOME=$ANDROID_HOME" >> ~/.bashrc
echo "export PATH=\$ANDROID_HOME/platform-tools:\$ANDROID_HOME/cmdline-tools/latest/bin:\$PATH" >> ~/.bashrc
