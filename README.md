<div align="center">

# 📜 Parchment

**A refined, offline-first PDF reader for Android — purpose-built for scholarly and religious texts with bilingual support.**

[![CI](https://github.com/AhmadHassan-BTed/Parchment/actions/workflows/ci.yml/badge.svg)](https://github.com/AhmadHassan-BTed/Parchment/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-9.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4.svg)](https://developer.android.com/jetpack/compose)

</div>

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 📖 **Continuous Scrolling** | Smooth vertical page flow like a real book |
| 🔍 **Pinch-to-Zoom** | Up to 5× zoom with pan support for crisp text |
| 🌙 **Night Mode** | Smart colour inversion for comfortable dark reading |
| 📑 **Table of Contents** | Bilingual chapter navigation (English + Urdu/Arabic) |
| 🔖 **Bookmarks** | Save, browse, and jump to bookmarked pages |
| 💾 **Reading Position** | Automatically remembers where you left off |
| 📏 **Page Scrubber** | Slider + tap-to-jump for fast navigation |
| 🎯 **Gesture Controls** | Swipe for TOC/bookmarks, double-tap zoom, tap to toggle HUD |
| 📳 **Haptic Feedback** | Subtle vibrations on chapter changes and actions |
| 🔄 **Auto-hiding HUD** | Reading controls fade after 4 seconds |

## 🏗️ Architecture

Parchment follows a **layered architecture** with strict module boundaries:

```
app/src/main/java/ted/parchment/reader/
├── data/                        # Data layer (zero UI dependencies)
│   ├── model/                   #   Domain models: TocItem, BookConfig
│   ├── preferences/             #   SharedPreferences: ReadingPreferences
│   └── repository/              #   Data sources: BookRepository
├── ui/                          # Presentation layer
│   ├── components/              #   Reusable composables: PdfThumbnail
│   ├── theme/                   #   Design system: DesignTokens, Colors, Typography
│   ├── viewer/                  #   PDF viewer components (6 extracted modules)
│   ├── HomeScreen.kt            #   Book library grid
│   └── PdfViewerScreen.kt       #   Main reading orchestrator (~350 lines)
├── utils/                       # Utilities: PdfUtils
└── MainActivity.kt              # Entry point
```

> **Key insight:** The original `PdfViewerScreen.kt` was a 1,316-line monolith. It's now a 350-line orchestrator that composes 6 focused, independently testable components.

📄 **Full details:** [docs/architecture.md](docs/architecture.md)

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Kotlin 2.0 |
| **UI Framework** | Jetpack Compose with Material 3 |
| **PDF Engine** | Android native `PdfRenderer` (zero external dependencies) |
| **Persistence** | SharedPreferences |
| **Build System** | Gradle with Kotlin DSL + Version Catalogs |
| **Min SDK** | 28 (Android 9.0) |
| **Target SDK** | 36 |
| **CI/CD** | GitHub Actions |

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Ladybug (2024.2) or newer
- **JDK 11+**
- **Android SDK 36**

### Setup

```bash
# Clone the repository
git clone https://github.com/AhmadHassan-BTed/Parchment.git
cd Parchment

# Open in Android Studio and sync Gradle
# Or build from CLI:
./gradlew assembleDebug
```

### Running

1. Open the project in Android Studio
2. Select a device/emulator (API 28+)
3. Click **Run** ▶️

### Testing

```bash
# Unit tests
./gradlew test

# Android instrumented tests
./gradlew connectedAndroidTest
```

## 📦 Building for Release

```bash
# Build unsigned release APK
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/
```

For signed releases, configure your keystore in `~/.gradle/gradle.properties` (never commit keystores):

```properties
PARCHMENT_STORE_FILE=/path/to/keystore.jks
PARCHMENT_STORE_PASSWORD=****
PARCHMENT_KEY_ALIAS=****
PARCHMENT_KEY_PASSWORD=****
```

## 📐 Design System

Parchment uses a custom **warm paper aesthetic** distinct from Material 3 defaults:

| Token | Light | Dark |
|-------|-------|------|
| Background | `#F8F1E4` (warm cream) | `#1C1C1E` (charcoal) |
| Text | `#222222` | `#EDE6D6` |
| Accent | `#8B6914` (amber-gold) | `#C4974A` |
| Surface | `#F0E8D8` | `#2C2C2E` |

Dark mode deliberately avoids pure black (`#000000`) to reduce eye strain during extended reading.

## 🗺️ Roadmap

See [ROADMAP.md](ROADMAP.md) for planned features:

- **v1.1** — Text search, brightness control, landscape mode
- **v1.2** — Multi-book library, user-imported PDFs
- **v1.3** — Annotations and notes
- **v2.0** — Room/DataStore, Navigation, tablet layouts, localization

## 🤝 Contributing

Contributions are welcome! Please read:

1. [Contributing Guidelines](CONTRIBUTING.md)
2. [Code of Conduct](CODE_OF_CONDUCT.md)
3. [Architecture Docs](docs/architecture.md)

### Quick start

```bash
# Fork → Clone → Branch
git checkout -b feat/my-feature

# Make changes → Test → Commit
git commit -m "feat(viewer): add page search"

# Push → Open PR
git push origin feat/my-feature
```

## 🔒 Security

- **No network requests** — fully offline
- **No analytics/tracking** — zero telemetry
- **No user data leaves the device**
- See [SECURITY.md](SECURITY.md) for our security policy

## 📄 License

```
Copyright 2024 Parchment Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

See [LICENSE](LICENSE) for the full text.

## 🆘 Support

- 📋 [Open an issue](../../issues/new/choose)
- 💬 [GitHub Discussions](../../discussions)
- 📖 [FAQ & Support](SUPPORT.md)

---

<div align="center">

**Built with ❤️ using Kotlin and Jetpack Compose**

</div>
