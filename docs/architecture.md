# Architecture

## Overview

Parchment follows a **layered architecture** with strict dependency rules. The data layer has zero knowledge of the UI, and the UI layer composes small, focused modules rather than monolithic screens.

```
┌─────────────────────────────────────────────────────┐
│                   MainActivity                       │
│                 (Entry Point)                        │
└─────────────────────┬───────────────────────────────┘
                      │
          ┌───────────▼───────────┐
          │    Presentation (UI)   │
          │                        │
          │  ┌──────────────────┐  │
          │  │  PdfViewerScreen │  │  ← Orchestrator
          │  └───────┬──────────┘  │
          │          │             │
          │  ┌───────▼──────────┐  │
          │  │ viewer/ package  │  │  ← Extracted components
          │  │  • TocPanel      │  │
          │  │  • BookmarksPanel│  │
          │  │  • SettingsPanel │  │
          │  │  • VerticalZoom  │  │
          │  │  • PdfPageRender │  │
          │  │  • PageJumpDialog│  │
          │  └──────────────────┘  │
          │                        │
          │  ┌──────────────────┐  │
          │  │ components/      │  │  ← Reusable
          │  │  • PdfThumbnail  │  │
          │  └──────────────────┘  │
          │                        │
          │  ┌──────────────────┐  │
          │  │ theme/           │  │  ← Design system
          │  │  • DesignTokens  │  │
          │  │  • ColorScheme   │  │
          │  │  • Typography    │  │
          │  └──────────────────┘  │
          └───────────┬────────────┘
                      │
          ┌───────────▼───────────┐
          │     Data Layer         │
          │                        │
          │  ┌──────────────────┐  │
          │  │ model/           │  │  ← Domain models
          │  │  • TocItem       │  │
          │  │  • BookConfig    │  │
          │  └──────────────────┘  │
          │                        │
          │  ┌──────────────────┐  │
          │  │ repository/      │  │  ← Data sources
          │  │  • BookRepository│  │
          │  └──────────────────┘  │
          │                        │
          │  ┌──────────────────┐  │
          │  │ preferences/     │  │  ← Persistence
          │  │  • ReadingPrefs  │  │
          │  └──────────────────┘  │
          └───────────┬────────────┘
                      │
          ┌───────────▼───────────┐
          │   Utility Layer        │
          │  • PdfUtils            │
          └────────────────────────┘
```

## Dependency Rules

| Layer | Can Depend On | Cannot Depend On |
|-------|--------------|-----------------|
| `data/model/` | Nothing | Everything else |
| `data/repository/` | `data/model/` | UI, Utils |
| `data/preferences/` | Android Framework | UI, Models |
| `utils/` | Android Framework | UI, Data |
| `ui/theme/` | Compose | Data, Utils |
| `ui/components/` | `utils/`, `ui/theme/` | `data/`, `ui/viewer/` |
| `ui/viewer/` | `data/model/`, `ui/theme/` | `data/repository/`, `utils/` |
| `ui/PdfViewerScreen` | Everything in data/ & ui/ | — (Orchestrator) |

## Key Design Decisions

### Why no ViewModel?
The PDF viewer's state (page position, zoom, HUD visibility) is entirely screen-scoped. A ViewModel would add a dependency (`lifecycle-viewmodel-compose`) and indirection with no measurable benefit. If multi-screen navigation is added later, a ViewModel can be introduced for the shared state.

### Why SharedPreferences over DataStore/Room?
The persistence needs are minimal (scroll position, night mode flag, bookmark sets). SharedPreferences is fast, synchronous for reads, and zero-dependency. DataStore migration is planned for v2.0.

### Why synchronized PdfRenderer access?
Android's `PdfRenderer` only allows one page to be open at a time. The `synchronized(pdfRenderer)` block in `PdfPageRenderer` ensures thread-safe access when `LazyColumn` renders multiple pages concurrently.

### Why color inversion for night mode?
Rather than maintaining separate dark-themed PDF assets, the reader applies a real-time `ColorMatrix` inversion filter. This inverts white backgrounds to near-black while keeping text readable — a technique used by most PDF readers.

## Module Responsibilities

| Module | Responsibility | Lines |
|--------|---------------|-------|
| `PdfViewerScreen` | Orchestrates all viewer state and UI composition | ~350 |
| `PdfPageRenderer` | Renders a single PDF page as a Compose Image | ~100 |
| `TocPanel` | Table of Contents bottom sheet | ~110 |
| `BookmarksPanel` | Bookmarks bottom sheet with empty state | ~120 |
| `SettingsPanel` | Night mode toggle + gesture reference | ~110 |
| `VerticalZoomBar` | Custom Canvas-based zoom slider | ~200 |
| `PageJumpDialog` | Page number input dialog | ~70 |
| `PdfThumbnail` | First-page thumbnail renderer | ~100 |
| `ReadingPreferences` | SharedPreferences CRUD | ~75 |
| `BookRepository` | Static book/TOC data | ~70 |
