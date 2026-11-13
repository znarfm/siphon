# Siphon

[![CI](https://github.com/znarfm/siphon/actions/workflows/ci.yml/badge.svg)](https://github.com/znarfm/siphon/actions/workflows/ci.yml)
[![Release](https://github.com/znarfm/siphon/actions/workflows/build.yml/badge.svg)](https://github.com/znarfm/siphon/actions/workflows/build.yml)
[![GitHub Release](https://img.shields.io/github/v/release/znarfm/siphon?color=blue&logo=github)](https://github.com/znarfm/siphon/releases)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-29%20(Android%2010)-brightgreen)](https://developer.android.com/about/versions/10)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-35%20(Android%2015)-brightgreen)](https://developer.android.com/about/versions/15)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

**Siphon** is a minimalist, privacy-focused, open-source Android media sniffer and chunked download manager.

## Why Siphon?

- **Zero Bloat or Tracking**: No ads, analytics SDKs, or proprietary trackers.
- **Zero FFmpeg Overhead**: HLS decryption, segment assembly, and MP4 remuxing run on native Android `MediaExtractor` and `MediaMuxer` APIs.
- **Scoped Storage Only**: Saves directly via `MediaStore.Downloads` or Storage Access Framework (SAF)—no `MANAGE_EXTERNAL_STORAGE` required.
- **Crash-Resilient**: Fsync-ordered chunk checkpointing preserves download progress across abrupt stops or reboots.

## Features

- **Multi-Worker Chunked Downloads**: Adaptive chunking with parallel HTTP workers, per-download speed throttling, and automatic single-stream fallback when byte ranges are unsupported.
- **Zero-FFmpeg HLS Extraction**: Concurrent segment downloads, in-flight AES-128 decryption, sliding-window assembly, and single-pass MP4 remuxing.
- **Passive Media Sniffer**: Detects streams in the built-in browser, deduplicates candidates, captures session headers, and supports rebinding expired CDN links with 4KB overlap verification.
- **Built-in Ad & Tracker Blocker**: Native Rust `adblock` engine via JNI with fast host/filter blocking and a one-tap Shields toggle.
- **Torrent Support**: Integrated `libtorrent4j` engine for `.torrent` files and magnet links.
- **Background Service & Controls**: Android 14/15 `dataSync` foreground service with Pause/Resume/Cancel notifications and automatic network-recovery resume.
- **Material You UI**: Jetpack Compose Material 3 Expressive interface with segmented chunk progress bars, browser tabs, bookmarks/history, and queue settings.

## Installation

### Requirements
- **OS**: Android 10+ (API 29 minimum, targeting API 35)
- **Architecture**: `arm64-v8a` (release APKs; `x86_64` also included in debug builds for emulators)

### Steps
1. Download `app-release.apk` from [Releases](https://github.com/znarfm/siphon/releases).
2. Open the APK on your device and allow installation from unknown sources if prompted.
3. Grant **Notification Permission** on Android 13+ for background progress and controls.

## Development

See [DEVELOPMENT.md](DEVELOPMENT.md) for prerequisites, JNI and Gradle build commands, signing/CI parity, and architecture notes.

## License

Licensed under the **GNU General Public License v3.0** (GPL-3.0). See [LICENSE](https://www.gnu.org/licenses/gpl-3.0).
