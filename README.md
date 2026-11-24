# Zyven FNF Assistant

## Version 0.3.2 — player-lane calibration

Detector calibration updated using the user's supplied 1600x720 gameplay recording:
- Player-side receptor columns are calibrated at normalized horizontal positions 0.2875, 0.4125, 0.6000, and 0.7250.
- Scans saturated note-colored components rather than dividing the entire display into four equal vertical strips.
- Filters components by approximate arrow-head / sustain-note geometry and reports candidates per player lane.
- Floating panel reports lane candidate counts.

Limitations:
- Experimental color/shape heuristic, not a trained arrow classifier.
- Character art, effects, and some HUD elements can still create false positives.
- Calibration is based on the supplied recording and may need adjustment for different resolutions, mods, and HUD layouts.
- No automated taps or AccessibilityService are included.
