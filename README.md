# Siphon

Minimalist, open-source Android media sniffer and chunked download manager (1DM alternative).

- Zero-FFmpeg HLS (`.m3u8`) capture and remuxing via built-in Android media APIs.
- Multi-part chunked HTTP downloader (adaptive 4–32MB ranges, positional disk I/O).
- Built-in lightweight suffix host ad-blocker without WebView overhead.
- Clean Scoped Storage / `MediaStore` integration (no broad storage permissions required).

---

## Users

### Requirements
- Android 10+ (API 29+)
- `arm64-v8a` device

### Install
1. Grab latest `app-release.apk` from GitHub Releases.
2. Open APK on device and confirm install prompt.
3. Grant notification permissions when prompted (required for foreground download progress).

---

## Developers

### Prerequisites
- JDK 17
- Android SDK (Compile/Target API 35, Build Tools 35.0.0)

### Setup
```bash
git clone https://github.com/your-repo/siphon.git
cd siphon
```

### Build

Debug APK:
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

Release APK:
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

### Quality Checks
```bash
./gradlew test
./gradlew lint
```
