# Zyven FNF Assistant

## Version 0.3.0 — game profile foundation

Added selectable and locally saved game profiles:
- Official Friday Night Funkin' Android release from Google Play (V-Slice).
- Psych Engine 0.7.3.
- V-Slice-based mods.

The profile selector is an initial compatibility layer. It does not yet mean note detection or automatic input is supported for every profile.

Still included:
- Movable purple floating bubble.
- User-approved screen capture.
- No AccessibilityService and no automated taps in this build.

Next implementation stage: profile-specific lane/receptor detection, then Smart Input and Auto Play. These features must be tested against actual gameplay captures for each profile; support is not claimed before that testing.

Do not disable or bypass Android/Google security protections. Re-enabling automated input may require Android Accessibility permission and can trigger additional security review.
