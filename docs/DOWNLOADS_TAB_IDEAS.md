# Downloads Tab Feature Ideas

High-leverage features and improvements for the Siphon Downloads / Queue tab (`QueueScreen.kt`).

---

### 1. Batch Queue Controls
Header quick actions:
- **Clear Completed**: Purge completed tasks from database without deleting media files from disk.
- **Pause All / Resume All**: Single-tap toggle across all active/queued tasks.
- **Bulk Delete / Cancel**: Multi-select mode with checkboxes to cancel or remove multiple tasks at once.

### 2. Search & Filter Bar
Collapsible search toolbar under filter chips:
- **Text Search**: Real-time filtering by target filename or source domain.
- **Sort Options**:
  - Date Added (newest / oldest)
  - File Size (largest / smallest)
  - Progress %
  - Name (A–Z)

### 3. File Sharing & URL Actions
Long-press menu or dedicated card actions:
- **Share File**: Trigger system `Intent.ACTION_SEND` using `FileProvider` to share directly to messaging, storage, or external players.
- **Copy Stream URL**: Copy raw `.m3u8` or progressive media URL to clipboard.
- **Copy Page URL**: Copy originating webpage URL to revisit source.

### 4. One-Tap Retry for Failed Tasks
Currently, failed tasks require re-sniffing the page:
- Add direct **Retry** button on `FAILED` and `CANCELLED` tasks.
- Resets chunk offsets, clears stale temp files, and moves download back into `QUEUED` state.

### 5. Media & Task Inspector Bottom Sheet
Tap card body to inspect details:
- Output file path on storage with direct "Show in Files" intent.
- HLS metadata: stream codecs, resolution, bandwidth, segment count.
- Captured network headers (`Referer`, `User-Agent`, `Cookie`).
- Preflight validation status and HTTP error codes.

### 6. Storage Disk Meter
Header banner displaying remaining device storage:
- Live display: `Available: 42.5 GB · Queue Est: 1.8 GB`.
- Low-storage warning banner when free space drops below threshold (e.g. <500 MB).

### 7. In-Place Rename
- Rename completed files directly from the card.
- Updates disk filename via `File.renameTo` / `MediaStore` and updates Room DB record without external file managers.

### 8. Inline Playback Preview
- Playback icon on completed tasks (and progressive downloads past initial buffer threshold).
- Native lightweight preview via `AndroidView` `VideoView` or Media3 player.

### 9. Segment-Driven Progress & Dynamic Estimate Handling
Prevents UI overflow and premature 100% pin when Variable Bitrate (VBR) downloads exceed manifest estimates:
- **Segment Ground Truth**: Base HLS progress bar on completed chunks (`mergedSegments / totalSegments`), not bytes.
- **Sliding Ceiling**: Dynamically project total size when bytes downloaded exceed initial estimate:
  `dynamicTotal = (downloadedBytes.toDouble() / mergedSegments * totalSegments).toLong()`
- **UI Label Format**: Display `1.2 GB · 420/500 segs (84%)` or `1.2 GB of ~1.5 GB` instead of hard mismatched numbers like `1.4 GB / 800 MB`.

### 10. Explicit "Merging / Processing" State UI
Eliminates the appearance of an app freeze after downloading all segments:
- Transition from `ACTIVE` to dedicated `MERGING` state during `remuxToMp4` and final MediaStore copy.
- Display distinct badge and status text: `"Remuxing MP4..."` and `"Saving to storage..."` with indeterminate progress.
