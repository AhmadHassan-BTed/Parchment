# Support

## 🆘 Getting Help

### Before asking for help

1. **Check the [README](README.md)** for setup and usage instructions
2. **Search [existing issues](../../issues)** — your question may already be answered
3. **Read the [FAQ](#faq)** below

### Where to get help

| Channel | Use For |
|---------|---------|
| [GitHub Issues](../../issues) | Bug reports, feature requests |
| [GitHub Discussions](../../discussions) | Questions, ideas, community chat |
| [Contributing Guide](CONTRIBUTING.md) | Development setup, code guidelines |

### Reporting bugs

Use the [bug report template](../../issues/new?template=bug_report.md) and include:
- Device model and Android version
- Steps to reproduce
- Expected vs. actual behavior
- Screenshots if applicable

## FAQ

### Q: Why does the app only show one book?
**A:** Parchment currently ships with a single bundled PDF. Multi-book library support is on the [roadmap](ROADMAP.md). The `HomeScreen` and `BookRepository` are already designed for multi-book expansion.

### Q: Can I load my own PDFs?
**A:** Not yet — user-imported PDFs are planned for v1.2. Currently, PDFs must be bundled as assets.

### Q: Why not use a PDF library like PDFium or MuPDF?
**A:** Parchment uses Android's built-in `PdfRenderer` to keep the APK size minimal and avoid native library dependencies. For most use cases, the native renderer provides excellent quality.

### Q: The PDF looks blurry when zoomed in.
**A:** Pages are rendered at 2× native resolution. If you need higher fidelity at extreme zoom levels, this can be improved by implementing dynamic re-rendering at the current zoom level.

### Q: How do I change the book's Table of Contents?
**A:** Edit the `tableOfContents` list in `BookRepository.kt`. Each `TocItem` maps a chapter title to a zero-based page index.
