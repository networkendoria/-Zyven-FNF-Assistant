# Zyven FNF Assistant

## Version 0.3.1 — experimental visual note detection

Added a first-pass real-time screen detector:
- Reads frames only after the Android screen-capture consent flow.
- Scans the play area for saturated colored clusters.
- Reports experimental candidates across four horizontal screen regions.
- Shows live candidate counts in the floating control panel.

Limitations:
- This is a color-cluster heuristic, not a trained FNF arrow recognizer.
- Background effects, UI colors, skins, and HUD layouts can cause false positives or missed notes.
- The four regions are provisional and require calibration against real gameplay screenshots for each profile.
- No automatic taps or AccessibilityService are included.

Profiles: official Android/V-Slice, Psych Engine 0.7.3, and V-Slice-based mods. Profile selection is saved locally; profile-specific detector calibration is not yet implemented.
