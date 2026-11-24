# Zyven FNF Assistant

Personal Android prototype for the original Friday Night Funkin' Android app.

## Version 0.1.0
- Movable purple floating bubble and control panel.
- User-approved MediaProjection capture service.
- Accessibility gesture service with four manual test-tap buttons.
- GitHub Actions debug APK build.

## Important limitation
This is not an automatic FNF player yet. Version 0.1.0 verifies the overlay, capture and accessibility plumbing. It does not recognize notes or automatically press lanes. The four buttons send fixed-position test taps near the bottom of the display; use them only to verify permissions, not during a song.

Note recognition must be calibrated against screenshots from the exact FNF Android build and tested across screen sizes before automatic input can be enabled. Automatic gameplay input is intentionally disabled in this build.

## Build
GitHub Actions builds the debug APK on every push to main. Open Actions, select the latest successful Android APK run, then download the Zyven-FNF-Assistant-debug artifact.

## Privacy
Captured frames are immediately discarded in this prototype. No screenshots are saved or uploaded.
