package ted.parchment.reader.data.model

/**
 * Represents a single entry in a book's Table of Contents.
 *
 * Maps a human-readable chapter title to its zero-based page index within the PDF.
 * Supports bilingual titles (e.g., English + Urdu) separated by newlines.
 *
 * @param title     Chapter title, may contain newlines for multi-script display
 * @param pageIndex Zero-based page index where this chapter begins
 */
data class TocItem(
    val title: String,
    val pageIndex: Int
)
