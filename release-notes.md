Fixes media-stream parsing when Jellyfin omits `IsOriginal` from a `MediaStream` response. An authenticated, read-only response from Jellyfin 10.11.11 contained five media streams, all without `IsOriginal`. A sanitized copy of that response reproduces the original SDK decoding failure; the updated typed `getEpisodes` path decodes all five streams.

Four regression tests and the Kotlin formatting check passed. No runtime playback test is claimed.

This optimized release preserves the existing fork app identity (`dev.jdtech.jellyfin.debug`, **Findroid Debug**) and the same fork signing certificate as prior releases. It is not an official Findroid release.

