package ted.parchment.reader.data.model

/**
 * Pairs a PDF asset path with its pre-defined Table of Contents.
 *
 * Decoupling the path from the TOC means:
 * - The same PDF can be presented with different chapter segmentations
 * - TOC definitions live in a single repository, not scattered across screens
 * - The viewer composable is completely agnostic about what it's rendering
 *
 * @param displayName     Human-readable title shown in the UI (e.g. "Durood Collection")
 * @param assetFileName   Asset-relative filename (e.g. "Full_Book.pdf")
 * @param tableOfContents Ordered list of chapters; may be empty if the book has no TOC
 */
data class BookConfig(
    val displayName: String,
    val assetFileName: String,
    val tableOfContents: List<TocItem>
)
