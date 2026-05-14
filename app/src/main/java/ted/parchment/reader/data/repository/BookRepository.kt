package ted.parchment.reader.data.repository

import ted.parchment.reader.data.model.BookConfig
import ted.parchment.reader.data.model.TocItem

/**
 * Single source of truth for all book configurations available in the app.
 *
 * Centralizing book metadata here ensures:
 * - The UI layer never directly constructs domain data
 * - Adding or modifying books is a single-file change
 * - TOC data can later be loaded from remote sources or local JSON
 *
 * Architecture note: This is intentionally an `object` (singleton) since the
 * book catalog is static and compile-time constant. For dynamic book loading
 * (e.g., user-imported PDFs), inject a `BookRepository` interface instead.
 */
object BookRepository {

    /**
     * Returns the default book configuration for the bundled PDF asset.
     */
    fun getDefaultBook(): BookConfig = fullBook

    /**
     * Returns all available book configurations.
     * Currently contains only the bundled book; designed for future expansion.
     */
    fun getAllBooks(): List<BookConfig> = listOf(fullBook)

    // ── Book Definitions ────────────────────────────────────────────────────

    private val fullBook = BookConfig(
        displayName = "Parchment User Guide",
        assetFileName = "parchment_guide.pdf",
        tableOfContents = listOf(
            TocItem("Cover & Welcome", pageIndex = 0),
            TocItem("Features Overview", pageIndex = 1),
            TocItem("The Interface", pageIndex = 2),
            TocItem("Tools & Comfort", pageIndex = 3),
            TocItem("Your Library", pageIndex = 4),
            TocItem("The Promise (Privacy)", pageIndex = 5)
        )
    )
}
