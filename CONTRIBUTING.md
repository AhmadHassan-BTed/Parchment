# Contributing to Parchment

Thank you for your interest in contributing to Parchment! This guide will help you get started.

##  Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Architecture Overview](#architecture-overview)
- [Making Changes](#making-changes)
- [Commit Convention](#commit-convention)
- [Pull Request Process](#pull-request-process)
- [Code Style](#code-style)
- [Testing](#testing)

## Code of Conduct

This project adheres to the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## Getting Started

1. **Fork** the repository
2. **Clone** your fork locally
3. **Create a branch** for your change (`git checkout -b feat/my-feature`)
4. **Make your changes** following the guidelines below
5. **Test** your changes
6. **Push** and open a **Pull Request**

## Development Setup

### Prerequisites

- **Android Studio** Ladybug (2024.2) or newer
- **JDK 11+**
- **Android SDK 36** (compile SDK)
- **Min SDK 28** (Android 9.0)

### Building

```bash
# Clone the repository
git clone https://github.com/your-username/parchment.git
cd parchment

# Open in Android Studio, or build from CLI:
./gradlew assembleDebug

# Run tests
./gradlew test
```

## Architecture Overview

```
app/src/main/java/ted/parchment/reader/
├── data/                    # Data layer
│   ├── model/               # Domain models (TocItem, BookConfig)
│   ├── preferences/         # SharedPreferences wrapper
│   └── repository/          # Data sources (BookRepository)
├── ui/                      # Presentation layer
│   ├── components/          # Reusable composables (PdfThumbnail)
│   ├── theme/               # Material 3 theme + DesignTokens
│   └── viewer/              # PDF viewer components (extracted)
├── utils/                   # Utilities (PdfUtils)
└── MainActivity.kt          # Entry point
```

**Key principles:**
- **Separation of Concerns** — Data, UI, and utility layers are isolated
- **Single Responsibility** — Each file has one job
- **Composition over Inheritance** — UI is composed from small, focused composables

## Making Changes

### Adding a new feature
1. Models go in `data/model/`
2. Data sources go in `data/repository/`
3. Reusable UI components go in `ui/components/`
4. Feature-specific UI goes in `ui/viewer/` (or a new feature directory)
5. Never put data logic in composables

### Fixing a bug
1. Write a failing test first (if possible)
2. Fix the bug
3. Verify the test passes

## Commit Convention

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]
[optional footer]
```

**Types:** `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `ci`, `perf`

**Examples:**
```
feat(viewer): add double-tap zoom reset
fix(prefs): bookmark not persisting on config change
docs: update architecture diagram
refactor(theme): consolidate color tokens
```

## Pull Request Process

1. Ensure your code compiles without warnings
2. Update documentation if you changed public APIs
3. Fill out the PR template completely
4. Request review from at least one maintainer
5. Address review feedback promptly

### PR Requirements
- [ ] Code compiles cleanly (`./gradlew assembleDebug`)
- [ ] Tests pass (`./gradlew test`)
- [ ] No hardcoded secrets or API keys
- [ ] Follows existing code style and patterns
- [ ] KDoc added for public functions

## Code Style

- **Kotlin** — Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- **Max line length** — 120 characters
- **Imports** — No wildcard imports in production code (OK in Compose files for `.*` UI imports)
- **Naming** — `camelCase` for functions/variables, `PascalCase` for classes/composables
- **Documentation** — KDoc for all public functions and classes

## Testing

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

### Test structure
- Unit tests: `app/src/test/`
- Instrumented tests: `app/src/androidTest/`

---

Thank you for contributing! 
