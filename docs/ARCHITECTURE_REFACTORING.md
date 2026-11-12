# Architecture & Package Refactoring Specification

Refactoring plan to decouple the single-module `:app` architecture, eliminate the 600-line `MainViewModel` god-object, and establish clean Jetpack Compose UDF patterns across Browser and Download Queue domains.

---

## 1. Target Package Structure

```
app/src/main/kotlin/com/znarfm/siphon/
├── SiphonApp.kt                 # Application subclass + AppContainer (manual DI, notification channels)
├── core/                        # Core engines, network clients, sniffer, and storage preflight
│   ├── adblock/
│   ├── engine/
│   ├── hls/
│   ├── network/
│   ├── service/
│   ├── sniffer/
│   └── storage/
├── data/
│   ├── db/                      # Room entities, DAOs, and SiphonDatabase
│   └── repository/
│       ├── DownloadRepository.kt # DB access, storage cleanup, DownloadForegroundService triggers
│       └── BrowserRepository.kt  # Bookmarks, history, search/browser preferences, adblock lists
└── ui/
    ├── MainActivity.kt          # Root Scaffold, top navigation pill, theme mode, rebind coordinator
    ├── browser/
    │   ├── BrowserScreen.kt
    │   ├── BrowserTab.kt
    │   └── BrowserViewModel.kt  # Tabs, WebView navigation, SnifferClient & SnifferSheet state
    ├── queue/
    │   ├── QueueScreen.kt
    │   └── QueueViewModel.kt    # Downloads StateFlow, chunk maps, pause/resume/delete actions
    ├── sniffer/
    │   └── SnifferSheet.kt      # Multi-select sniffer modal sheet UI
    └── theme/
        ├── Color.kt
        ├── Shape.kt
        └── Theme.kt
```

---

## 2. Settled Architectural Decisions

| Area | Decision | Technical Rationale |
|---|---|---|
| **Dependency Injection** | Manual `AppContainer` inside `SiphonApp` + `ViewModelProvider.Factory` | Zero dependencies, no Dagger/Hilt/Koin Gradle plugin or KSP overhead, fast builds. |
| **Sniffer Lifecycle** | Hosted inside `BrowserViewModel` | Media sniffing is inherently bound to `WebView` sessions, tab switching, and DOM events. Injects `DownloadRepository` to queue downloads directly. |
| **Cross-Screen Coordination** | `MainActivity` callback lambdas | Direct callback: `QueueScreen(onRebind = { ... })` -> `MainActivity` switches tab to `AppTab.BROWSER` and calls `browserViewModel.startRebindFlow(download)`. No event bus. |
| **Repository Scope** | Full-feature Repositories | `DownloadRepository` wraps Room DAO + `DownloadForegroundService` intents. `BrowserRepository` wraps Room bookmarks/history + adblock subscriptions + prefs. Keeps ViewModels pure. |
| **Application Lifecycle** | `SiphonApp : Application` | Early initialization of notification channels, singletons (`SiphonDatabase`, `FastHostFilter`), and `AppContainer`. Registered in `AndroidManifest.xml`. |

---

## 3. Detailed Component Responsibilities

### `DownloadRepository`
- **State Streams**:
  - `downloads: Flow<List<DownloadEntity>>`
  - `chunksMap: Flow<Map<Long, List<ChunkEntity>>>`
  - `concurrencySettings: Flow<ConcurrencySettings>` (maxDownloads, workers, bandwidthLimit, wifiOnly)
- **Actions**:
  - `startDownload(media, variantUrl, customFilename)`
  - `pauseDownload(id)`, `resumeDownload(id)`, `cancelDownload(id)`
  - `pauseAll()`, `resumeAll()`, `clearCompleted()`
  - `retryDownload(id)`
  - `renameDownload(id, newName)`
  - `deleteDownload(id, deleteFile)`
  - `rebindDownload(target, media, variantUrl)`
  - `updateConcurrency(maxDownloads, workers, bandwidthLimit, wifiOnly)`

### `BrowserRepository`
- **State Streams**:
  - `bookmarks: Flow<List<BookmarkEntity>>`
  - `history: Flow<List<HistoryEntity>>`
  - `isUrlBookmarked(url: Flow<String>): Flow<Boolean>`
  - `adblockRuleCount: StateFlow<Int>`
  - `browserPreferences: Flow<BrowserPreferences>` (desktopMode, webDarkMode, searchEngine)
- **Actions**:
  - `toggleBookmark(url, title)`, `deleteBookmark(id)`
  - `addHistory(url, title)`, `deleteHistory(id)`, `clearHistory()`
  - `refreshAdblockSubscription(customUrl)`
  - `setDesktopMode(enabled)`, `setWebDarkMode(enabled)`

### `BrowserViewModel`
- Manages `browserTabs`, `activeTabId`, `browserUrl`, `isSnifferSheetOpen`, and delegates to `MediaSnifferClient`.
- Injects `BrowserRepository` and `DownloadRepository`.

### `QueueViewModel`
- Manages `QueueFilter` (ALL, ACTIVE, PAUSED, COMPLETED), search query, sort mode, and multi-select queue items.
- Injects `DownloadRepository`.

### `MainActivity`
- Hosts root `Scaffold`, top navigation pill (`AppTab.BROWSER` vs `AppTab.QUEUE`), and app theme/seed picker.
- Dispatches cross-screen rebind lambda: `onRebind = { download -> selectTab(AppTab.BROWSER); browserViewModel.startRebindFlow(download) }`.

---

## 4. Migration Roadmap

### Phase 1: Application Container & Repositories
1. Register `SiphonApp` in `AndroidManifest.xml` (`android:name=".SiphonApp"`).
2. Implement `SiphonApp.kt` with `AppContainer` holding `database`, `downloadStorageProvider`, `fastHostFilter`, `downloadRepository`, and `browserRepository`.
3. Create `data/repository/DownloadRepository.kt` with Room DAO methods and `DownloadForegroundService` commands.
4. Create `data/repository/BrowserRepository.kt` with Room bookmarks/history, adblock rule reloading, and browser prefs.

### Phase 2: Feature ViewModels & UI Wiring
1. Implement `ui/browser/BrowserViewModel.kt`.
2. Implement `ui/queue/QueueViewModel.kt`.
3. Refactor `ui/MainActivity.kt` to instantiate both ViewModels via `SiphonApp.container.viewModelFactory`, wire UI screens, and connect the rebind callback lambda.
4. Delete legacy `ui/MainViewModel.kt`.

---

## 5. Line Count Audit & Decomposition Targets

```
Lines   File                                      Category
-------------------------------------------------------------------------
1433    ui/browser/BrowserScreen.kt               UI Monolith
1340    ui/queue/QueueScreen.kt                   UI Monolith
 818    core/sniffer/MediaSnifferClient.kt        Sniffer + DOM
 713    core/engine/ChunkQueueEngine.kt           Core Chunk Engine
 655    core/service/DownloadForegroundService.kt Foreground Service
 654    core/hls/HlsQueueEngine.kt                HLS Engine + Remuxer
 592    ui/MainViewModel.kt                       God ViewModel
 504    ui/sniffer/SnifferSheet.kt                UI Bottom Sheet
 405    ui/MainActivity.kt                        Root Scaffold + Tabs
 243    core/hls/HlsPlaylistParser.kt             HLS Parser
 216    core/storage/DownloadStorageProvider.kt   Storage & MediaStore
 193    core/network/MetadataProber.kt            Network Prober
 179    data/db/DownloadDao.kt                    Room DAO
 174    core/adblock/FastHostFilter.kt            Adblock Trie / Filter
 153    core/storage/FilesystemPreflight.kt       StatFs & FAT32 Preflight
  78    core/network/NetworkClients.kt            OkHttp Singletons
  75    ui/theme/Color.kt                         Color Schemes & Seeds
  68    core/network/TokenBucket.kt               Rate Limiter
  66    ui/theme/Theme.kt                         MaterialTheme Setup
  55    data/db/BrowserEntities.kt                Room Entities
  49    core/hls/HlsModels.kt                     HLS Data Models
  43    data/db/DownloadEntity.kt                 Room Entity
  35    data/db/ChunkEntity.kt                    Room Entity
  33    data/db/SiphonDatabase.kt                 Room DB Definition
  28    core/sniffer/SnifferModels.kt             Sniffer Data Models
  14    ui/theme/Shape.kt                         Compose Shapes
  10    ui/browser/BrowserTab.kt                  Tab Model
-------------------------------------------------------------------------
8826    TOTAL KOTLIN LINES
```

### Specific Subcomponent Extractions

1. **`ui/browser/BrowserScreen.kt` (1,433 lines -> ~500 lines)**:
   - Extract `ui/browser/BrowserTopBar.kt`: Address bar, progress bar, search suggest list.
   - Extract `ui/browser/BrowserTabGrid.kt`: Tab switcher sheet and grid cards.
   - Extract `ui/browser/BrowserMenuSheet.kt`: Bookmarks, history, adblock, and settings menus.

2. **`ui/queue/QueueScreen.kt` (1,340 lines -> ~400 lines)**:
   - Extract `ui/queue/DownloadItemCard.kt`: Download row card, action buttons, segment canvas strip.
   - Extract `ui/queue/TaskDetailsSheet.kt`: Stream metadata, file path, headers bottom sheet.
   - Extract `ui/queue/QueueSettingsDialog.kt`: Concurrency sliders and bandwidth limiter dialog.

3. **`core/hls/HlsQueueEngine.kt` (654 lines -> ~450 lines)**:
   - Extract `core/hls/HlsRemuxer.kt`: Isolate `remuxToMp4()` (`MediaExtractor` + `MediaMuxer` pipeline) into a standalone unit-testable class.

4. **`core/sniffer/MediaSnifferClient.kt` (818 lines -> ~500 lines)**:
   - Extract `core/sniffer/SnifferUrlClassifier.kt`: Media extensions, segment matching, language heuristics, and MIME sniffers.

