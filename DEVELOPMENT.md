# Development Guide

## Prerequisites

- **JDK**: JDK 17+ (`app/build.gradle.kts` targets JVM 17; [`mise.toml`](mise.toml) provisions `temurin-21`, `android-sdk`, and `gradle`).
- **Android SDK & NDK**: Compile SDK 35, Build Tools 35.0.0, and NDK `27.2.12479018` (`ANDROID_NDK_HOME` set).
- **Rust & `cargo-ndk`**: Stable Rust toolchain with Android targets and `cargo-ndk`:
  ```bash
  rustup target add aarch64-linux-android x86_64-linux-android
  cargo install cargo-ndk
  ```

## Building JNI (`rust/adblock-jni`)

Build Android `.so` libraries into `app/src/main/jniLibs`:
```bash
./rust/adblock-jni/build-android.sh
```

Build the host shared library (required for JVM unit tests):
```bash
cargo build --release --manifest-path rust/adblock-jni/Cargo.toml
```

## Gradle Checks & Builds

```bash
# Lint, unit tests, and debug APK (arm64-v8a + x86_64)
./gradlew lintDebug testDebugUnitTest assembleDebug

# Release APK (R8 minified, resource-shrunk, arm64-v8a)
./gradlew assembleRelease
```

Outputs:
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`

## Signing & CI Parity

[`app/build.gradle.kts`](app/build.gradle.kts) resolves release signing in this order:
1. `keystore.properties` in the repo root (or `app/`).
2. Environment variables: `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
3. Fallback to debug signing with a warning if neither is configured.

### Local Setup
Run the interactive setup script to generate/link a keystore, write `keystore.properties` (`chmod 600`), and optionally upload GitHub Actions secrets via `gh`:
```bash
./scripts/setup-signing.sh
```

`keystore.properties` format (gitignored):
```properties
storeFile=release.keystore
storePassword=...
keyAlias=siphon
keyPassword=...
```

### CI Secrets (`.github/workflows/build.yml`)
Configure in **Settings → Secrets and variables → Actions**:

| Secret | Description |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded release keystore |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias (e.g. `siphon`) |
| `KEY_PASSWORD` | Key password |

CI decodes `KEYSTORE_BASE64` to `release.keystore` and passes `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` to `./gradlew assembleRelease`.

## Architecture Overview

Siphon uses a modular unidirectional data flow (UDF) architecture with manual DI via `AppContainer` in `SiphonApp`:

```
siphon/
├── app/src/main/kotlin/com/znarfm/siphon/
│   ├── SiphonApp.kt           # Application & AppContainer DI
│   ├── core/
│   │   ├── adblock/           # Native Rust adblock JNI wrapper & host filter
│   │   ├── engine/            # Multi-worker ChunkQueueEngine & checkpointer
│   │   ├── hls/               # Playlist parser, AES-128, MediaExtractor/MediaMuxer
│   │   ├── network/           # Dual-protocol OkHttp clients, MetadataProber, TokenBucket
│   │   ├── service/           # DownloadForegroundService (dataSync) & TileService
│   │   ├── sniffer/           # Passive WebView MediaSnifferClient & URL classifier
│   │   ├── storage/           # MediaStore/SAF provider & FilesystemPreflight
│   │   └── torrent/           # libtorrent4j session & torrent engine
│   ├── data/
│   │   ├── db/                # Room database, DAOs, entities
│   │   └── repository/        # DownloadRepository, BrowserRepository
│   └── ui/
│       ├── MainActivity.kt    # Root Compose scaffold & intent handling
│       ├── browser/           # WebView browser, tabs, address bar
│       ├── queue/             # Download queue & chunk progress UI
│       ├── settings/          # App & engine settings
│       ├── sniffer/           # SnifferSheet stream picker
│       └── theme/             # Material 3 Expressive theme
├── rust/adblock-jni/          # Rust cdylib wrapping `adblock` crate via `jni`
├── scripts/setup-signing.sh   # Local keystore & GitHub Secrets helper
└── .github/workflows/         # ci.yml (lint/test/debug), build.yml (signed release)
```
