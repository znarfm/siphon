# PLAN.md: Lightweight Android Media Downloader & Sniffer

## Milestone 0: Project Setup & Core Infrastructure (P0)
- [x] Initialize Android project (Min SDK 29, Target/Compile SDK 35, Kotlin 2.x, single `app` module per YAGNI; defer flavors to Milestone 8).
- [x] Configure `app/build.gradle.kts`:
  - [x] Add OkHttp 4.12+ and Kotlin Coroutines.
  - [x] Add Room DB dependencies with KSP.
  - [x] Add Jetpack Compose BOM and Material 3 dependencies.
  - [x] Restrict NDK ABIs to `arm64-v8a` (add `x86_64` for debug build variants only).
- [x] Define Room schema:
  - [x] `downloads` table (`id`, `url`, `displayName`, `destinationUri`, `mimeType`, `totalBytes`, `etag`, `lastModified`, `isResumable`, `type` [HTTP/HLS], `state`, `sourcePageUrl`, `headersJson`, `createdAt`).
  - [x] `download_chunks` table (primary key `(downloadId, trackIndex, chunkIndex)`, `startOffset`, `currentOffset`, `endOffset`, `state` [`PENDING`, `IN_FLIGHT`, `DONE`, `MERGED`]).
  - [x] Write `DownloadDao` with atomic transaction methods, boot recovery (`UPDATE download_chunks SET state = 'PENDING' WHERE state = 'IN_FLIGHT'`), and credential purge (`UPDATE downloads SET headersJson = NULL WHERE id = :id`).
- [x] Configure `AndroidManifest.xml` with permissions (`INTERNET`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `POST_NOTIFICATIONS`) and credential hygiene (`android:allowBackup="false"`, `android:dataExtractionRules` excluding Room DB).

---

## Milestone 1: Chunk-Queue HTTP Downloader Engine (P0)
*Goal: Rock-solid multi-threaded byte-range downloading with zero corruption on abrupt process death.*

- [x] Implement `OkHttpClient` factory:
  - [x] Instance 1: HTTP/1.1 enforced (`protocols = listOf(Protocol.HTTP_1_1)`), max 6–8 connections per host.
  - [x] Instance 2: HTTP/2 default for HLS/API probes sharing the same connection pool.
- [x] Implement `MetadataProber`:
  - [x] Issue `Range: bytes=0-0` probe (with `HEAD` fallback).
  - [x] Parse `Content-Range` / `Content-Length`, `Accept-Ranges`, `ETag` (strong only), and `Last-Modified`.
  - [x] Branch logic: if `Accept-Ranges: bytes` is missing, server returns HTTP 200, or `totalBytes <= 0`, set `isResumable = false` and create a single `ChunkEntity(trackIndex = 0, chunkIndex = 0, startOffset = 0, currentOffset = 0, endOffset = -1)`.
- [x] Implement `ChunkQueueEngine`:
  - [x] Adaptive chunk sizing: `clamp(totalBytes / 512, min = 4MB, max = 32MB)` (or `totalBytes / workerCount` for files < 24MB).
  - [x] Global concurrency: max 2 concurrent active downloads (configurable 1–4), default 6 workers per download (configurable 1–8) pulling from `ConcurrentLinkedQueue`.
  - [x] Execute positional disk writes: `channel.write(buffer, currentPos)` in a loop, updating per-chunk `AtomicLong` `currentOffset`.
  - [x] Validate HTTP 206 `Content-Range` header against expected `currentOffset` before reading body.
  - [x] Retry transient chunk errors with 3 exponential-backoff attempts (`1s`, `2s`, `4s` + `Retry-After` support) before transitioning download to `WAITING_FOR_NETWORK` or `FAILED`.
- [x] Implement Single Checkpointer Coroutine:
  - [x] Ticking interval: 2–4 seconds.
  - [x] Snapshot all workers' `AtomicLong` `currentOffset` values and in-memory `DONE` transitions *before* calling `channel.force(false)`.
  - [x] Flush disk: `channel.force(false)`.
  - [x] Commit pre-force snapshot to SQLite via `DownloadDao` *after* `force()` returns; execute final `force()` + commit when all chunks finish.
- [x] Implement Safe Resumption:
  - [x] If `isResumable == false`, truncate `FileChannel` to 0 and restart from byte 0.
  - [x] Otherwise query incomplete chunks (`state != DONE`), resuming each from `currentOffset` with `If-Range: <etag>` or `If-Unmodified-Since`.
  - [x] Reset task and truncate file if server returns HTTP 200 on resume.

---

## Milestone 2: Android Storage & MediaStore Integration (P0)
*Goal: Fast positional writes on API 29+ without requiring MANAGE_EXTERNAL_STORAGE.*

- [x] Implement `MediaStore` Storage Provider:
  - [x] Insert entry into `MediaStore.Downloads.EXTERNAL_CONTENT_URI` with `IS_PENDING = 1` and `DATE_EXPIRES` (+30 days), letting `MediaStore` auto-suffix duplicate `DISPLAY_NAME`s.
  - [x] Persist returned `content://` URI string in `downloads.destinationUri`.
  - [x] Obtain writeable descriptor via `contentResolver.openFileDescriptor(uri, "rw")` and extract `FileOutputStream(pfd.fileDescriptor).channel`.
  - [x] Flip `IS_PENDING = 0` upon completed download; call `contentResolver.delete(uri, null, null)` on cancel/reset to prevent orphan pending files.
- [x] Implement Free Space & Filesystem Pre-flight:
  - [x] Query `StatFs.availableBytes` before allocation.
  - [x] Detect filesystem type via `/proc/mounts` / `Os.fstatvfs`: fail fast if `totalBytes > 4GB` on FAT32, and warn/cap parallel chunk distance on non-sparse FAT/exFAT external SAF mounts.
- [x] Implement SAF Document Tree Fallback (for user-selected custom directories).

---

## Milestone 3: Foreground Service & App Lifecycle (P1)
*Goal: Stable background execution compliant with Android 14/15 restrictions.*

- [x] Implement `DownloadForegroundService` (`foregroundServiceType="dataSync"`):
  - [x] Foreground notification with throttled progress updates (`sample(250ms)`).
  - [x] Add pause, resume, and cancel `PendingIntent` actions.
- [x] Handle Android 15 Execution Limits:
  - [x] Override `onTimeout(int, int)` (API 35) and `onTimeout(int)` (API 34): cleanly trigger `channel.force(false)`, checkpoint DB state to `PAUSED`, and post tap-to-resume notification.
  - [x] Wrap download pipeline in `withContext(NonCancellable)` inside `finally` block to guarantee checkpoint on cancellation.

---

## Milestone 4: Fast In-Memory Ad-Blocker (P1)
*Goal: Drop trackers and ad networks without JVM object bloat or WebView lag.*

- [ ] Implement `FastHostFilter`:
  - [ ] Package pre-stripped lowercase domain list in assets (1 domain per line, comments/IPs stripped).
  - [ ] Lazily initialize pre-sized `HashSet<String>` on `Dispatchers.Default`.
  - [ ] Implement suffix-slicing matcher: test `sub.ads.domain.com` -> `ads.domain.com` -> `domain.com` ($O(k)$ domain depth).
- [ ] Integrate Blocker into WebView:
  - [ ] Block ad network subresources inside `shouldInterceptRequest` (return empty `WebResourceResponse`).
  - [ ] Block redirect chains and popups inside `shouldOverrideUrlLoading`.

---

## Milestone 5: WebView Media Interceptor & Sniffer (P1)
*Goal: Cleanly extract media without injected JavaScript bridges or segment probe storms.*

- [ ] Implement `MediaSnifferClient`:
  - [ ] Hook `shouldInterceptRequest` passively (always return `null` for non-ad requests so Chromium loads normally).
  - [ ] Dedup candidate URLs (normalized by stripping byte-range query params) in an LRU set.
  - [ ] Skip probing `.ts`/`.m4s` segment URLs when a parent `.m3u8`/`.mpd` manifest is already captured for the current page.
  - [ ] Capture `CookieManager.getInstance().getCookie(url)`, `Referer`, and `User-Agent`.
- [ ] Implement Async Probe & Manifest Fetch:
  - [ ] For `.m3u8` candidates, fetch playlist directly via background OkHttp client to extract variant streams/resolutions.
  - [ ] For progressive candidates (`.mp4`, `.webm`, `Sec-Fetch-Dest: video/audio`), fire background `Range: bytes=0-0` via OkHttp to confirm `Content-Type` and extract `totalBytes`.
- [ ] Implement Explicit "Update Link" Rebind Flow:
  - [ ] When a paused download fails with HTTP 403/410, tapping Resume opens `sourcePageUrl` in WebView in "Update Link" rebind mode.
  - [ ] Verify candidate replacement URL via `Content-Length == totalBytes` AND a 4KB `Range: bytes=0-4095` byte-comparison against local disk before updating `url`, `etag`, and `headersJson`.

---

## Milestone 6: Native HLS / .m3u8 Downloader & Remuxer (P2)
*Goal: Full stream extraction and crash-safe muxing with zero FFmpeg dependencies.*

- [ ] Implement HLS Playlist Parser:
  - [ ] Parse master playlists (`#EXT-X-STREAM-INF`, `#EXT-X-MEDIA:TYPE=AUDIO`) and cache selected media playlist(s) to disk.
  - [ ] Populate `download_chunks` keyed by `(downloadId, trackIndex, chunkIndex)` (`trackIndex = 0` for video/muxed, `1` for separate audio).
- [ ] Implement AES-128 Segment Decryption & Sliding-Window Assembly:
  - [ ] Fetch decryption keys using captured session headers and decrypt segments via `CipherInputStream` (`AES/CBC/PKCS5Padding`, sequence-number big-endian IV fallback) to `seg_<track>_<idx>.tmp`.
  - [ ] As contiguous segments complete, append directly into `track_<trackIndex>.tmp`, store cumulative byte length in `chunk.endOffset`, `channel.force(false)`, mark chunk `MERGED` in Room, and delete `seg_<track>_<idx>.tmp` immediately.
  - [ ] On crash resume, truncate `track_<trackIndex>.tmp` to the last `MERGED` chunk's `endOffset`.
- [ ] Implement Native Media Muxing:
  - [ ] Run a single `MediaExtractor` -> `MediaMuxer` pass per completed `track_<trackIndex>.tmp` file to interleave audio/video samples by `sampleTime` into the final `MediaStore` `.mp4` container (fallback to copying raw `.ts` if `MediaExtractor`/`MediaMuxer` rejects codec).
  - [ ] Purge `headersJson` and temporary track files upon completion.

---

## Milestone 7: Jetpack Compose UI & Controls (P2)
*Goal: Fast, modern user interface.*

- [ ] Design App Theme:
  - [ ] Material You dynamic color scheme + adaptive launcher icon.
- [ ] Screens & Components:
  - [ ] **Browser Tab**: Address bar, navigation controls, ad-block toggle, "Update Link" banner when rebinding a 403 task.
  - [ ] **Sniffer Sheet**: Bottom sheet listing detected streams, resolutions, sizes, and "Download" / "Rebind" triggers.
  - [ ] **Queue Screen**: Active/paused/completed downloads, segmented chunk progress bar, pause/resume/cancel/refresh-link actions, concurrency settings.

---

## Milestone 8: Optional Torrent Engine (P3 - Post-MVP)
*Goal: BitTorrent protocol support isolated from core build.*

- [ ] Split into `full` vs `slim` Gradle flavors.
- [ ] Wrap `libtorrent4j` `SessionManager` in a singleton service writing to `getExternalFilesDir()` with opt-in `MANAGE_EXTERNAL_STORAGE`.

---

## Verification & Test Plan
- [ ] **Sudden Process Death**: Run `adb shell am force-stop <package>` at 50% download progress. Resume and verify file integrity via `sha256sum`.
- [ ] **Reboot Integrity**: Download a 1GB test file, reboot device mid-transfer, resume, and confirm zero hash mismatches.
- [ ] **Bandwidth Saturation**: Compare single-stream download speed vs 6-chunk download on throttled hosts.
