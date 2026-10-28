Adds control contrast choices: None (preserve existing appearance), dark button backplates, a bottom gradient, or both. Moves the compact transport row 8dp closer to the seek bar while retaining 4dp clearance below its 48dp touch target. Retains the existing layouts, button size, background dimming, auto-hide, playback speed, and Jellyfin media-stream compatibility features.

The Release build and Kotlin formatting check passed. Signed Release emulator verification passed for ExoPlayer playback, persisted settings, None/Buttons/Both visibility, play/pause touch, and seek-bar touch. Gradient-only was not independently tested. MPV was not runtime-verified. Screen-tap reveal after controls hide remains unverified; the baseline comparison could not complete because the runtime agent reached its quota. No regression-fix or new-regression claim is made.

The Release asset keeps the existing fork app identity and signing certificate. This is not an official Findroid release.
