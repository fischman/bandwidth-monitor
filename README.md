# Bandwidth Monitor

Trivial app to display current upload/download rates in the notification bar.

# Implementation notes

Android doesn't allow arbitrary text in the notification bar itself
(only in notification titles/bodies, visible only when the
notifications shade is expanded), so this app fakes it by rendering
its text into a bitmap and using that as the notification's icon.

Android further restricts notifications' icons to being square, so the
text is perforce tiny.

# Alternatives considered

There are other apps in the play store that do similar things but all
gave me scuzzy vibes, either because they collect PII, share PII with
third-parties, hide some features (or ad-suppression) behind paywalls,
etc.

# Building

- `./.headless/install-android-sdk.sh` once per host/VM/container to
  install build requirements
- `./.headless/build-and-deploy.sh` to build and deploy to an
  `adb`-connected device or emulator. Append `--release` for a minimal
  APK, ~23KB rather than the debug APK which weighs in ~806KB, or 35x bigger.

# Demo

<img src="https://github.com/user-attachments/assets/75be7287-729e-4bd2-bb62-627cd84fe0f5" width=540>
