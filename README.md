# Siphon

[![CI](https://github.com/znarfm/siphon/actions/workflows/ci.yml/badge.svg)](https://github.com/znarfm/siphon/actions/workflows/ci.yml)
[![Release](https://github.com/znarfm/siphon/actions/workflows/build.yml/badge.svg)](https://github.com/znarfm/siphon/actions/workflows/build.yml)
[![GitHub Release](https://img.shields.io/github/v/release/znarfm/siphon?color=blue&logo=github)](https://github.com/znarfm/siphon/releases)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-29%20(Android%2010)-brightgreen)](https://developer.android.com/about/versions/10)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-35%20(Android%2015)-brightgreen)](https://developer.android.com/about/versions/15)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

**Siphon** is a minimalist, privacy-focused, open-source Android media sniffer and chunked download manager designed as a modern, high-performance alternative to 1DM. Built with zero bloatware, zero third-party tracking, and zero FFmpeg dependencies, Siphon harnesses native Android media and storage APIs to deliver maximum transfer throughput with minimal resource utilization.

---

## Table of Contents
- [Why Siphon?](#why-siphon)
- [Key Features & Architecture](#key-features--architecture)
  - [Zero-FFmpeg HLS Stream Extraction & Remuxing](#1-zero-ffmpeg-hls-stream-extraction--remuxing)
  - [Multi-Worker Chunked HTTP Engine](#2-multi-worker-chunked-http-engine)
  - [Passive Media Sniffer & Resilient CDN Link Rebinding](#3-passive-media-sniffer--resilient-cdn-link-rebinding)
  - [In-Memory Suffix Host Ad-Blocker](#4-in-memory-suffix-host-ad-blocker)
  - [Scoped Storage & MediaStore (Zero Broad Permissions)](#5-scoped-storage--mediastore-zero-broad-permissions)
  - [Background Execution & Android 14/15 Lifecycle](#6-background-execution--android-1415-lifecycle)
  - [Material You (M3 Expressive) UI](#7-material-you-m3-expressive-ui)
- [User Installation](#user-installation)
- [Developer Guide](#developer-guide)
  - [Prerequisites](#prerequisites)
  - [Clone & Setup](#clone--setup)
  - [Quality Checks](#quality-checks)
  - [Build Commands](#build-commands)
- [Signing Key & Local / CI Parity Guide](#signing-key--local--ci-parity-guide)
  - [Architecture & Parity Strategy](#architecture--parity-strategy)
  - [Quick Automated Setup with `setup-signing.sh`](#quick-automated-setup-with-setup-signingsh)
  - [Configuration Reference](#configuration-reference)
- [Architecture & Directory Overview](#architecture--directory-overview)
- [Contributing & License](#contributing--license)

---

## Why Siphon?

Traditional Android download managers are often burdened with intrusive full-screen advertisements, invasive analytics SDKs, battery-draining background processes, and heavy multi-megabyte native binaries like FFmpeg.

Siphon re-architects mobile downloading from first principles:
- **Zero FFmpeg Overhead**: Stream decryption, sliding-window segment assembly, and container muxing are executed entirely through Android's hardware-accelerated platform APIs (`MediaExtractor` and `MediaMuxer`).
- **No Broad Storage Permissions**: Full compliance with Android Scoped Storage using `MediaStore.Downloads`—no risky `MANAGE_EXTERNAL_STORAGE` permission required.
- **Tear-Free Crash Resiliency**: Strict `Snapshot -> Fsync -> SQLite` checkpointer ordering guarantees zero byte corruption across abrupt task kills or device reboots.
- **Privacy by Default**: Includes an in-memory suffix-slicing ad blocker, stores session credentials in private app storage with backup exclusion, and purges headers immediately upon download completion.

---

## Key Features & Architecture

### 1. Zero-FFmpeg HLS Stream Extraction & Remuxing
- **Sliding-Window Segment Assembly**: HLS segments (`.ts` / `.m4s`) are pulled by concurrent workers into a sliding window keyed by `(downloadId, trackIndex, chunkIndex)`. As contiguous segments finish, they are appended directly to `track_<trackIndex>.tmp` and the temporary segment files are pruned immediately, preventing 3× disk amplification.
- **In-Flight AES-128 Decryption**: Decrypts encrypted segments on-the-fly via `CipherInputStream` (`AES/CBC/PKCS5Padding`) using session-authenticated key retrieval and RFC 8216 sequence-number big-endian IV fallback.
- **Byte-Range Manifest Parsing**: Supports `#EXT-X-BYTERANGE` and `#EXT-X-MAP` range partitions (Vimeo, Cloudflare Stream, YouTube HLS), preventing redundant full-file redownloads.
- **Single-Pass Container Remuxing**: Completed video and audio tracks are interleaved into standard `.mp4` containers via a unified `MediaExtractor` → `MediaMuxer` pass. Presentation timestamps (PTS) and decode timestamps (DTS) are preserved across non-monotonic B-frame sequences without audio desync, falling back to raw `.ts` only if codecs are rejected by the platform muxer.

### 2. Multi-Worker Chunked HTTP Engine
- **Adaptive Chunk Sizing**: Download payloads are dynamically divided into chunks scaled by `clamp(totalBytes / 512, min = 4MB, max = 32MB)` (or `totalBytes / workerCount` for smaller files < 24MB) and queued in a thread-safe `ConcurrentLinkedQueue`.
- **Thread-Safe Positional Disk I/O**: Multiple workers concurrently write into a single shared `FileChannel` using positional `channel.write(buffer, position)` loops, avoiding thread locking and file descriptor duplication.
- **Pre-Force Snapshot Checkpointer**: A dedicated checkpointer coroutine snapshots all workers' `AtomicLong` offsets and `DONE` states *before* calling `channel.force(false)`, and commits to Room (`download_chunks`) *after* fsync completes. This prevents phantom progress records during sudden power loss.
- **Strict Byte-Range Validation**: Verifies HTTP 206 `Content-Range` responses against expected offsets before reading socket streams, utilizing strong `ETag` (`If-Range`) and `Last-Modified` headers. Automatically falls back to single-stream downloading if byte ranges are unsupported.
- **Dual-Protocol OkHttp Routing**: Dedicated HTTP/1.1 client prevents connection multiplexing head-of-line bottlenecks across chunked worker pools, while a shared HTTP/2 client handles lightweight manifest and metadata probes.
- **Bandwidth Throttling**: Integrated `TokenBucket` rate limiter ensures smooth, configurable speed caps without token evaporation during high-frequency concurrent socket reads.

### 3. Passive Media Sniffer & Resilient CDN Link Rebinding
- **Zero-Latency Passive Interception**: Monitors network requests in WebView via `shouldInterceptRequest` strictly passively (returning `null` for non-ad resources so Chromium's native network stack runs unimpeded).
- **Candidate Deduplication & Segment Suppression**: Normalizes candidate URLs, strips transient query parameters, and automatically suppresses segment flood (`.ts`/`.m4s`) once a parent manifest (`.m3u8`/`.mpd`) is detected.
- **Credential & Session Capture**: Securely captures session cookies, `Referer`, and `User-Agent` headers required for authenticated streams.
- **Expired Link Rebinding (HTTP 403/410 Recovery)**: When temporary signed CDN links expire, tapping Resume reopens the original `sourcePageUrl` in WebView in "Update Link" rebind mode.
- **4KB Overlap Verification**: Before adopting a replacement URL, Siphon verifies `Content-Length == totalBytes` and performs a cryptographic byte-for-byte comparison of a 4KB `Range: bytes=0-4095` sample against existing local file data to guarantee stream identity.

### 4. In-Memory Suffix Host Ad-Blocker
- **Zero-IPC Matcher**: Operates on an in-memory, pre-sized `HashSet<String>` loaded lazily on `Dispatchers.Default` from a clean, newline-delimited host asset (`adblock_hosts.txt`).
- **Domain Suffix Slicing**: Evaluates host hierarchies (`a.b.ads.com` → `b.ads.com` → `ads.com`) in $O(k)$ domain depth without regular expression overhead.
- **Subresource & Redirect Blocking**: Instantly returns HTTP 403 for blocked subresources in `shouldInterceptRequest` and intercepts popups/redirect chains in `shouldOverrideUrlLoading`.

### 5. Scoped Storage & MediaStore (Zero Broad Permissions)
- **Zero Special Permissions**: Operates within modern Android Scoped Storage guidelines—no `MANAGE_EXTERNAL_STORAGE` permission required.
- **Atomic MediaStore Allocation**: Inserts downloads directly into `MediaStore.Downloads.EXTERNAL_CONTENT_URI` with `IS_PENDING = 1` and a `+30d` `DATE_EXPIRES` pre-allocation, automatically handling platform filename collision resolution.
- **Orphan File Prevention**: Cleans up pending allocations via `contentResolver.delete(uri, null, null)` upon task cancellation or reset.
- **Storage Pre-Flight Verification**: Validates available disk space with `StatFs.availableBytes` and inspects filesystem mount points (`/proc/mounts` and `Os.fstatvfs`) to enforce 4GB FAT32 file boundaries and non-sparse external allocation limits.
- **SAF Document Tree Support**: Full fallback to Storage Access Framework (SAF) folder picker for custom user directories.

### 6. Background Execution & Android 14/15 Lifecycle
- **Foreground Service (`dataSync`)**: Reliable background execution with Android 14/15 foreground service policies.
- **Android 15 Execution Limit Handling**: Gracefully handles `onTimeout(int, int)` (API 35) and `onTimeout(int)` (API 34) timeouts by calling `channel.force(false)`, checkpointing database state to `PAUSED`, and posting a tap-to-resume notification.
- **Throttled Notification UI**: Progress notifications are sampled at `250ms` intervals to avoid IPC notification starvation, featuring direct Pause, Resume, and Cancel actions.
- **Network Resilience**: Monitors network state via `ConnectivityManager.NetworkCallback` to auto-resume active downloads upon network recovery.

### 7. Material You (M3 Expressive) UI
- **Modern Jetpack Compose Layout**: 100% Compose UI featuring dynamic color theming, expressive typography, and edge-to-edge system bar integration.
- **Multi-Chunk Progress Visualization**: Segmented progress bars display per-worker chunk allocations, active download streams, buffered byte ranges, and completed segments in real time.
- **Integrated Browser Tab**: Built-in browser with address bar, search engine switching, tab grid, bookmark/history management, and a one-tap Shields toggle.
- **Sniffer Modal Bottom Sheet**: Clean stream selector with audio/video stream badges, resolution tags, container formats, and file size estimates.

---

## User Installation

### Requirements
- **OS**: Android 10+ (API 29 minimum, fully tested on API 35)
- **Architecture**: `arm64-v8a` (standard for modern Android devices; `x86_64` included in debug builds for emulators)

### Installation Steps
1. Navigate to the [Releases](https://github.com/znarfm/siphon/releases) page.
2. Download the latest `app-release.apk`.
3. Open the downloaded file on your device and follow the installation prompt (enable "Install unknown apps" for your browser/file manager if prompted).
4. Launch Siphon and grant **Notification Permission** when requested (required on Android 13+ for background download progress notifications and action controls).

---

## Developer Guide

### Prerequisites
- **JDK**: OpenJDK 17 or Eclipse Temurin 17
- **Android SDK**: Compile SDK 35, Build Tools 35.0.0, Platform Tools
- **Gradle**: Handled via included Gradle Wrapper (`./gradlew`)

### Clone & Setup
```bash
git clone https://github.com/znarfm/siphon.git
cd siphon
```

### Quality Checks
Run static analysis and unit tests:
```bash
# Run Android lint
./gradlew lint

# Run unit tests across engines, HLS parser, and sniffer
./gradlew test
```

### Build Commands
Compile debug or release variants:

```bash
# Assemble Debug APK (supports arm64-v8a and x86_64 emulators)
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Assemble Release APK (R8 minified, resource-shrunk, arm64-v8a)
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

---

## Signing Key & Local / CI Parity Guide

### Architecture & Parity Strategy
Siphon ensures identical signing key parity between local developer builds and GitHub Actions CI releases without exposing secret credentials to version control.

The release signing configuration in [`app/build.gradle.kts`](file:///home/antigravity/znarfm/siphon/app/build.gradle.kts) implements a dual-source lookup strategy:
1. **Local Machine**: Reads configuration from `keystore.properties` (or root `keystore.properties`).
2. **CI / Automation**: Reads configuration from environment variables (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
3. **Graceful Fallback**: If signing parameters are absent or incomplete, Gradle logs a warning and falls back to debug signing so local builds never break.

Both `keystore.properties` and keystore files (`*.keystore`, `*.jks`) are ignored by [`.gitignore`](file:///home/antigravity/znarfm/siphon/.gitignore).

### Quick Automated Setup with `setup-signing.sh`
The repository includes an interactive signing setup script:

```bash
./scripts/setup-signing.sh
```

This automated helper:
1. Generates a new 2048-bit RSA release keystore (`release.keystore`) or links an existing keystore file.
2. Creates `keystore.properties` with strict file permissions (`chmod 600`).
3. Encodes your keystore into a Base64 string for GitHub Actions.
4. Automatically uploads the secrets to your GitHub repository if the GitHub CLI (`gh`) is authenticated.

### Configuration Reference

#### Local Configuration: `keystore.properties`
Located in the repository root:
```properties
storeFile=release.keystore
storePassword=your_keystore_password
keyAlias=siphon
keyPassword=your_key_password
```

#### GitHub Actions Secrets
In your GitHub repository under **Settings → Secrets and variables → Actions**, configure:

| Secret Name | Description |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded contents of your release keystore file |
| `KEYSTORE_PASSWORD` | Keystore master password |
| `KEY_ALIAS` | Key alias name (e.g., `siphon`) |
| `KEY_PASSWORD` | Key alias password |

In your CI workflow, decode `KEYSTORE_BASE64` before compiling:
```yaml
- name: Decode Keystore
  run: echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > release.keystore

- name: Assemble Release APK
  env:
    KEYSTORE_FILE: release.keystore
    KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
    KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
    KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
  run: ./gradlew assembleRelease
```

---

## Architecture & Directory Overview

Siphon adheres to a modular, unidirectional data flow (UDF) architecture with manual dependency injection via `AppContainer`:

```
siphon/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml          # Permissions, foreground dataSync service
│   │   │   ├── assets/
│   │   │   │   └── adblock_hosts.txt        # Pre-processed domain blocklist
│   │   │   ├── kotlin/com/znarfm/siphon/
│   │   │   │   ├── SiphonApp.kt             # Application class & AppContainer DI
│   │   │   │   ├── core/
│   │   │   │   │   ├── adblock/             # FastHostFilter suffix-slicing matcher
│   │   │   │   │   ├── engine/              # ChunkQueueEngine, DynamicSemaphore
│   │   │   │   │   ├── hls/                 # HlsPlaylistParser, HlsQueueEngine, HlsRemuxer
│   │   │   │   │   ├── network/             # NetworkClients, MetadataProber, TokenBucket
│   │   │   │   │   ├── service/             # DownloadForegroundService, TileService
│   │   │   │   │   ├── sniffer/             # MediaSnifferClient, SnifferUrlClassifier
│   │   │   │   │   └── storage/             # DownloadStorageProvider, FilesystemPreflight
│   │   │   │   ├── data/
│   │   │   │   │   ├── db/                  # Room Database, DownloadDao, Entities
│   │   │   │   │   └── repository/          # DownloadRepository, BrowserRepository
│   │   │   │   └── ui/
│   │   │   │       ├── MainActivity.kt      # Root scaffold & cross-screen coordination
│   │   │   │       ├── browser/             # WebView browser screen, tabs, top bar
│   │   │   │       ├── queue/               # Download queue, chunk progress, settings
│   │   │   │       ├── sniffer/             # SnifferSheet modal stream picker
│   │   │   │       └── theme/               # Material 3 Expressive theme, typography, color
│   │   │   └── res/                         # Vector drawables, adaptive launcher icons, XMLs
│   │   └── test/                            # Unit tests for core engines, parser, filters
│   └── build.gradle.kts                     # App module configuration (API 29-35, Compose, Room)
├── gradle/                                  # Gradle wrapper and version catalog (libs.versions.toml)
├── scripts/
│   └── setup-signing.sh                     # Keystore & GitHub Secrets setup script
├── .github/workflows/
│   ├── ci.yml                               # CI workflow: lint, unit test, assemble debug
│   └── build.yml                            # Build & Release workflow: tags/dispatch, signed APK, GitHub Release
├── handoff.md                               # Architectural decisions & implementation rationale
└── README.md                                # Project documentation
```

---

## Contributing & License

Contributions are welcome! Please submit issues, pull requests, or feature requests via GitHub.

This project is licensed under the **GNU General Public License v3.0** (GPL-3.0). See [LICENSE](https://www.gnu.org/licenses/gpl-3.0) for complete license details.
