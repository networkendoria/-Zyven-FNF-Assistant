# Zyven FNF Assistant

Android prototype for personal use with Friday Night Funkin'.

## Version 0.2.1
- Movable floating Z bubble.
- User-approved MediaProjection screen capture; frames are processed locally and discarded.
- Experimental four-lane color sampling.
- Optional AccessibilityService gesture dispatch. The user must explicitly enable this service in Android Settings.
- Automatic tapping is OFF by default.

## Build outputs
GitHub Actions always creates a debug APK for development. A signed release APK is created only after configuring a persistent signing key in the repository Actions secrets:

- `ANDROID_KEYSTORE_BASE64`: base64-encoded JKS/PKCS12 keystore
- `ANDROID_STORE_PASSWORD`: keystore password
- `ANDROID_KEY_ALIAS`: key alias
- `ANDROID_KEY_PASSWORD`: key password

Keep the keystore and passwords private and backed up. Never generate a new signing key for each release; Android will reject updates signed with a different key.

## Play Protect and safety
A release signature does not guarantee that Google Play Protect will approve an app. Play Protect may warn about newly distributed apps or apps that use sensitive capabilities such as Accessibility. Do not disable Play Protect or bypass a warning. If Play Protect reports a harmful-app detection, stop installation and review the exact warning and app behavior.

Screen frames are not saved or uploaded by this app.

## Limitations
The detector is experimental and has not been validated across FNF Android versions, mods, aspect ratios, or HUD layouts. Missed notes and false taps are possible. Lane positions and hit line may need calibration.
