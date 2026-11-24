# Zyven FNF Assistant

Personal Android prototype for the original Friday Night Funkin' Android app.

## Version 0.1.1
- Movable purple floating bubble and control panel.
- User-approved MediaProjection capture service.
- Accessibility gesture service intentionally omitted from this install-test build.
- GitHub Actions debug APK build.

## Important limitation
This build is for verifying installation, overlay permission, and screen-capture permission. It does not recognize notes or automatically press lanes. The Accessibility service was removed from this build because Play Protect blocked the previous sideloaded APK after detecting sensitive accessibility capabilities. A future build that needs gesture automation must be distributed through a trusted, verifiable route and tested before use.

Captured frames are immediately discarded. No screenshots are saved or uploaded.
