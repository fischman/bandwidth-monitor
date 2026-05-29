#!/usr/bin/env bash
#
# Build APK and optionally deploy to a connected device.
#
# Flags:
#   --build-only - Don't deploy.
#   --releast - assembleRelease instead of assembleDebug.

set -euo pipefail

cd "$(dirname "$0")/.."

PACKAGE="org.fischman.bandwidthmonitor"

BUILD_ONLY=false
RELEASE=false
for arg in "$@"; do
  case "$arg" in
      --build-only) BUILD_ONLY=true ;;
      --release) RELEASE=true ;;
      *) echo "Unknown flag $arg" >&2 ; exit 1 ;;
  esac
done

export ANDROID_HOME=${ANDROID_HOME:-$HOME/android-sdk}
APK="build/outputs/apk/debug/BandwidthMonitor-debug.apk"
CMD="assembleDebug"

if $RELEASE; then
    APK="${APK//debug/release}"
    CMD="${CMD//Debug/Release}"
fi

./gradlew ktlintFormat ktlintCheck "$CMD"

\ls -lh "$APK" |awk '{print $5, $9}'

if $BUILD_ONLY; then
  exit 0
fi

adb devices | grep -q 'device$' || { echo "ERROR: No ADB device connected." >&2; exit 1; }

if ! adb install "$APK"; then
    echo "failed; uninstalling first..."
    adb uninstall "$PACKAGE" 2>/dev/null || true
    adb install "$APK"
fi

adb shell am start -n "${PACKAGE}/${PACKAGE}.MainActivity"
