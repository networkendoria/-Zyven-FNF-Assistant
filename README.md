# Zyven FNF Assistant

Android prototype for Friday Night Funkin'.

## Version 0.2.0
- Movable floating Z bubble.
- MediaProjection screen capture; frames are processed locally and immediately discarded.
- Experimental four-lane color detector with configurable lane fractions and hit line in AutoPlayEngine.kt.
- Optional AccessibilityService gesture dispatch; user must enable it explicitly in Android Settings.
- Automatic detection is OFF by default.

## Limitations
This is an experimental detector, not a validated FNF bot. Generic lane positions and hit line may not match every FNF Android build, aspect ratio, mod, or HUD layout. False taps and missed notes are possible. Play Protect may warn because gesture automation requires Accessibility access; do not disable Play Protect to bypass a warning.

No captured frames are saved or uploaded.
