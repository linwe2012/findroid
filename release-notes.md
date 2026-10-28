Adds configurable playback control button size, control background dimming, and controller auto-hide time for the shared MPV/Media3 UI. Retains the centered and compact-bottom layouts, playback speed selection, and Jellyfin media-stream compatibility fixes.

The Release build and Kotlin formatting check passed. Emulator verification confirmed the shared controls selection with ExoPlayer. Runtime checks for control auto-hide and glyph rendering remain pending; no MPV runtime pass is claimed.

The optimized Release asset keeps the existing fork app identity (`dev.jdtech.jellyfin.debug`, **Findroid Debug**) and existing fork signing certificate. It is not an official Findroid release.
