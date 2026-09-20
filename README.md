# Elnet TV

Demo Android TV app for Elnet Network.

## Demo stream

The initial build uses a public test HLS stream:
`https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8`

Replace `demoUrl` in `app/src/main/java/com/elnet/tv/MainActivity.kt` with the official Elnet stream before production use.

## Build

GitHub Actions builds `app-debug.apk` and uploads it as an artifact when changes are pushed to `feature/elnet-tv-demo`.
