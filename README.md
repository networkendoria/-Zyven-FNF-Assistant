# Zyven FNF Assistant

## Version 0.2.2 — install compatibility build

This build deliberately restores the same permission profile used by 0.1.1, which removed the AccessibilityService declaration after Play Protect blocked the earlier APK.

Included:
- Movable purple floating bubble.
- User-approved screen capture.
- No AccessibilityService and no automated taps.
- Screen-frame processing is disabled in this compatibility build.

This change addresses the known permission difference from 0.1.1. It cannot guarantee Play Protect approval; Play Protect decisions are made by Google and may depend on the exact warning, app reputation, and device state. Do not disable or bypass Play Protect.

GitHub Actions produces a debug APK. A release build requires a persistent signing key configured as repository Actions secrets. A release signature alone does not guarantee Play Protect approval.
