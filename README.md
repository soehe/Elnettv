# Elnet TV

Android TV IPTV player built with Kotlin, AndroidX, and Media3 ExoPlayer.

## Features

- M3U/M3U8/TXT playlist loading from URL or local file
- Last successful playlist cached locally for offline fallback
- Channel search by name or group
- HLS playback with Media3 ExoPlayer
- Android TV / remote-friendly landscape interface
- No analytics and no hard-coded production stream credentials

## Development playlist

Set the playlist URL from the in-app URL field before loading. A development URL is accepted only when its response is a valid M3U playlist. If loading fails, the app keeps the last cached playlist.

Use only streams and playlists you are authorized to access. Do not ship third-party URLs as production defaults.

## Build

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

GitHub Actions builds the debug APK on pushes to the development and production branches.
