# Contributing Guide — In-Depth

This document supplements the top-level [CONTRIBUTING.md](../CONTRIBUTING.md) with deeper architectural context for new contributors.

## Codebase Map

```
app/src/main/java/ted/parchment/reader/
│
├── MainActivity.kt              # App entry point — wires theme + root screen
│
├── data/                         # Pure Kotlin, no Android UI dependencies
│   ├── model/
│   │   ├── TocItem.kt           # Chapter entry (title + page index)
│   │   └── BookConfig.kt        # Book metadata (name + asset + TOC)
│   ├── preferences/
│   │   └── ReadingPreferences.kt # SharedPreferences wrapper
│   └── repository/
│       └── BookRepository.kt     # Static book catalog
│
├── ui/                           # Jetpack Compose UI layer
│   ├── components/
│   │   └── PdfThumbnail.kt      # Reusable first-page thumbnail
│   ├── theme/
│   │   ├── Color.kt             # Material 3 palette (Parchment brand)
│   │   ├── DesignTokens.kt      # Reader-specific colors + timing constants
│   │   ├── Theme.kt             # ParchmentTheme composable
│   │   └── Type.kt              # Typography scale
│   ├── viewer/                   # Viewer-specific composables
│   │   ├── BookmarksPanel.kt
│   │   ├── PageJumpDialog.kt
│   │   ├── PdfPageRenderer.kt
│   │   ├── SettingsPanel.kt
│   │   ├── TocPanel.kt
│   │   └── VerticalZoomBar.kt
│   ├── HomeScreen.kt            # Book library grid (future use)
│   └── PdfViewerScreen.kt       # Main reading orchestrator
│
└── utils/
    └── PdfUtils.kt              # Asset → FileDescriptor helper
```

## How Things Connect

1. **User opens app** → `MainActivity` launches `PdfViewerScreen("Full_Book.pdf")`
2. **Viewer loads** → `BookRepository.getAllBooks()` finds the matching `BookConfig`
3. **PDF opens** → `PdfUtils.getPdfFileDescriptor()` copies asset to cache, returns `ParcelFileDescriptor`
4. **Pages render** → `LazyColumn` renders `PdfPageRenderer` for each visible page
5. **State persists** → `ReadingPreferences` saves scroll position, bookmarks, night mode

## Common Tasks

### Adding a new book
1. Place the PDF in `app/src/main/assets/`
2. Add a `BookConfig` entry in `BookRepository.kt`
3. That's it — the viewer and HomeScreen will pick it up

### Adding a new UI panel
1. Create a composable in `ui/viewer/` that takes only the data + callbacks it needs
2. Wire it into `PdfViewerScreen.kt` with a `ModalBottomSheet`
3. Add a trigger button to the HUD

### Modifying the design system
1. Color changes → `DesignTokens.kt` (reader) or `Color.kt` (Material shell)
2. Timing changes → `DesignTokens.kt` constants
3. Typography → `Type.kt`

## Testing Strategy

- **Unit tests** (`src/test/`) — Test data models, repository logic, preferences
- **Instrumented tests** (`src/androidTest/`) — Test Compose UI interactions
- Keep tests focused: one assertion per test, descriptive names
