# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Professional open-source repository structure
- Modular architecture with clean separation of concerns
- Extracted `TocPanel`, `BookmarksPanel`, `SettingsPanel`, `VerticalZoomBar`, `PdfPageRenderer`, `PageJumpDialog` from monolithic viewer
- `BookRepository` as single source of truth for book metadata
- `ReadingPreferences` as a standalone persistence layer
- `DesignTokens` and `ReaderColorScheme` design system
- `PdfThumbnail` as a reusable component
- GitHub Actions CI/CD pipeline
- Comprehensive documentation (README, Architecture, Contributing Guide)
- Issue and PR templates
- Dependabot configuration
- Security policy
- Code of Conduct

### Changed
- Refactored `PdfViewerScreen` from 1,316 lines to ~350 lines
- Replaced generic purple Material theme with warm Parchment brand colors
- `HomeScreen` now uses `BookConfig` instead of deleted `PdfFile`
- Improved `PdfUtils` with proper logging and cleaned imports
- Hardened `.gitignore` with production-grade rules

### Removed
- Dead code: `PdfFile.kt` (unused data class)
- Development artifacts: `python.py`, `freelance.png`, `compressed_output/`
- Security risks: committed JKS keystore, `local.properties`, build logs
- Release APK from version control
- Empty placeholder directories

### Security
- Removed committed signing keystore from repository
- Removed `local.properties` containing local SDK paths
- Added comprehensive `.gitignore` rules for secrets and credentials

## [1.0.0] — 2024-01-01

### Added
- Initial release
- PDF rendering with Android's native PdfRenderer
- Continuous vertical scrolling
- Pinch-to-zoom with pan support
- Night mode with color inversion
- Table of Contents with bilingual chapter names
- Bookmark system with per-book persistence
- Auto-hiding HUD with gesture controls
- Chapter change notifications with haptic feedback
- Page jump dialog
- Reading position persistence across sessions
- Edge-to-edge display
