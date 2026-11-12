# Download Engine & Stream Merging Audit

Technical audit of `ChunkQueueEngine`, `HlsQueueEngine`, `HlsPlaylistParser`, and `DownloadStorageProvider`.

---

## Critical Defects

### 1. Premature Multi-Track EOS in `remuxToMp4`
- **File**: `app/src/main/kotlin/com/znarfm/siphon/core/hls/HlsQueueEngine.kt` (`remuxToMp4`)
- **Issue**:
  ```kotlin
  val sampleSize = extractor.readSampleData(buffer, 0)
  if (sampleSize < 0) break
  ```
  Audio and video track durations rarely match exactly. If audio finishes 50ms before video, `sampleSize < 0` breaks out of the entire muxing loop, dropping all subsequent video samples and corrupting duration.
- **Remedy**: Mark finished track as EOS and advance; break only when *all* extractors reach EOS (`sampleTime == -1L`).

### 2. B-Frame PTS Distortion & Stream Desync
- **File**: `app/src/main/kotlin/com/znarfm/siphon/core/hls/HlsQueueEngine.kt` (`remuxToMp4`)
- **Issue**:
  ```kotlin
  val rawPts = extractor.sampleTime
  val lastPts = lastPtsMap[muxerTrack] ?: -1L
  val adjustedPts = if (rawPts <= lastPts) lastPts + 1L else rawPts
  ```
  `MediaExtractor` reads frames in **decode order (DTS)**. With B-frames, presentation timestamps (PTS) naturally drop below preceding P-frames. Enforcing monotonic PTS (`lastPts + 1L`) corrupts presentation timestamps for all B-frames, causing stutter and A/V drift.
- **Remedy**: Pass raw PTS directly to `MediaMuxer` (Android API 30+ supports non-monotonic PTS in MP4 containers). Normalize initial track offset without distorting individual frame deltas.

### 3. Missing `#EXT-X-BYTERANGE` Support in HLS Parser
- **File**: `app/src/main/kotlin/com/znarfm/siphon/core/hls/HlsPlaylistParser.kt`
- **Issue**: Manifests that partition a single media file via `#EXT-X-BYTERANGE:length[@offset]` (Vimeo, YouTube HLS, Cloudflare Stream) are parsed without byte-range metadata.
- **Impact**: Engine issues `GET segment.url` without `Range` headers, downloading the entire remote file once for each segment (e.g. 500 segments = 500x full file downloads).
- **Remedy**: Parse `EXT-X-BYTERANGE` (and `#EXT-X-MAP` byte-range attributes) and store `startOffset` / `length` in `ParsedHlsSegment` to send `Range: bytes=start-end`.

### 4. Token Evaporation in `TokenBucket` Rate Limiter
- **File**: `app/src/main/kotlin/com/znarfm/siphon/core/network/TokenBucket.kt`
- **Issue**:
  ```kotlin
  val newTokens = (elapsedNanos * rate) / 1_000_000_000L
  ```
  Under frequent concurrent socket reads (<1ms intervals), integer division yields `0` when `elapsedNanos * rate < 1e9`. `lastRefillTimeNanos` updates to `now` regardless, discarding fractional time. Under heavy concurrency, the bucket starves and throughput drops to near zero.
- **Remedy**: Retain remainder nanos or advance `lastRefillTimeNanos` only by consumed elapsed time `(newTokens * 1e9) / rate`.

### 5. Post-Download Merge Freeze (Silent Remux & MediaStore Transfer)
- **File**: `app/src/main/kotlin/com/znarfm/siphon/core/hls/HlsQueueEngine.kt` (lines 161–190)
- **Issue**:
  - Once all segments finish downloading, the engine runs `remuxToMp4` followed by copying the multi-gigabyte file into the destination MediaStore URI via a 128 KB buffer.
  - On a 1.5 GB file, this disk-heavy processing takes 2–5 minutes on flash storage.
  - `onProgress` is **never called** during remuxing or destination copying. Network speed drops to `0 KB/s`.
  - `DownloadState` lacks a `MERGING` or `PROCESSING` state; the task remains `ACTIVE` with a full progress bar, giving the appearance of a hard freeze or lockup.
- **Remedy**:
  - Add `DownloadState.MERGING` (or `PROCESSING`) to database and UI with indeterminate progress / phase labels ("Remuxing MP4...", "Saving to disk...").
  - Report progress during the final MediaStore byte transfer.
  - Bypass `remuxToMp4` when only a single track exists that is already valid MP4/TS.

---

## UI/UX & Estimate Handling

### 6. Dynamic HLS Target Expansion & VBR Bitrate Drift
- **Issue**:
  - Master playlist `#EXT-X-STREAM-INF:BANDWIDTH` is only an average/nominal declaration. Variable Bitrate (VBR) encodes frequently exceed this value by 50%–100% (e.g., estimated 800 MB -> actual 1.5 GB).
  - When `totalBytes` is statically saved as 800 MB, the UI shows broken states like `1.4 GB / 800 MB` and the progress bar pins at 100% while segments continue downloading.
- **Remedy**:
  1. **Segment-Driven Progress**: For HLS, progress must be determined by segment completion:
     `progress = completedSegments.toFloat() / totalSegments`
     Ground truth is segments, not estimated byte sizes.
  2. **Sliding Ceiling (Dynamic Estimate)**:
     If `downloadedBytes >= estimatedTotalBytes`, dynamically project total size:
     `dynamicTotal = (downloadedBytes.toDouble() / completedSegments * totalSegments).toLong()`
  3. **Visual Distinction**:
     Display `~` prefix on estimated sizes (`1.2 GB of ~1.5 GB`) or render segment count alongside byte volume (`1.2 GB · 420/500 segs (84%)`).

---

## Performance & Resource Optimizations

### 7. Disk Space Amplification During HLS Remux
- **File**: `app/src/main/kotlin/com/znarfm/siphon/core/hls/HlsQueueEngine.kt`
- **Issue**: Retaining `seg_*.tmp` + `track_*.tmp` + `remux_output.mp4` alongside final `MediaStore` file requires 3x–4x stream size in cache (`/data/user/0/.../cache`), causing `ENOSPC` errors on 1GB+ files.
- **Remedy**:
  - Delete `track_*.tmp` immediately after `remuxToMp4` succeeds before copying to final storage.
  - Bypass `remuxToMp4` if download contains a single track that is already MP4.

### 8. Static 1MB Sample Buffer in Remuxer
- **File**: `app/src/main/kotlin/com/znarfm/siphon/core/hls/HlsQueueEngine.kt`
- **Issue**: `ByteBuffer.allocate(1024 * 1024)` fails with `BufferOverflowException` on high-bitrate 1080p/4K keyframes exceeding 1MB.
- **Remedy**: Query `format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4 * 1024 * 1024)`.

### 9. Explicit Default Audio Track Selection
- **File**: `app/src/main/kotlin/com/znarfm/siphon/core/hls/HlsQueueEngine.kt` (`parseAndPopulateTracks`)
- **Issue**: Always takes `firstOrNull` audio track matching `groupId`. If master playlist defines alternate dubs or commentary, default track may be skipped.
- **Remedy**: Prioritize `audioTracks.firstOrNull { it.groupId == groupId && it.isDefault } ?: firstOrNull()`.
