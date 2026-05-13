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
        displayName = "Durood Collection",
        assetFileName = "Full_Book.pdf",
        tableOfContents = listOf(
            TocItem("Durood-e-Ibrahimi\nدرودِ ابراہیمی", pageIndex = 283),
            TocItem("Durood-e-Jilani\nدرودِ جیلانی", pageIndex = 283),
            TocItem("Durood-e-Taj\nدرودِ تاج", pageIndex = 284),
            TocItem("Durood-e-Imam Shafi'i\nدرودِ امام شافعی", pageIndex = 284),
            TocItem("Durood-e-Mohi\nدرودِ ماہی", pageIndex = 284),
            TocItem("Durood-e-Jal Shukrat\nدرودِ جَلّ شُکرَت", pageIndex = 285),
            TocItem("Salat-o-Salam\nصلوٰۃ و سلام", pageIndex = 285),
            TocItem("Durood-e-Khidr\nدرودِ خضری", pageIndex = 285),
            TocItem("Durood-e-Habib Rasool\nدرودِ حبیبِ رسول", pageIndex = 286),
            TocItem("Durood-e-Tahir\nدرودِ طاہر", pageIndex = 286),
            TocItem("Durood-e-Azmat\nدرودِ عظمت", pageIndex = 286),
            TocItem("Durood-e-Ghausia\nدرودِ غوثیہ", pageIndex = 287),
            TocItem("Durood-e-Tunaji\nدرودِ تنجی", pageIndex = 287),
            TocItem("Durood-e-Shifa Quloob\nدرودِ شفاء القلوب", pageIndex = 288),
            TocItem("Durood-e-Qurani\nدرودِ قرآنی", pageIndex = 289),
            TocItem("Durood-e-Khaas\nدرودِ خاص", pageIndex = 290),
            TocItem("Durood-e-Sa'adat\nدرودِ سعادت", pageIndex = 290),
            TocItem("Durood-e-Aali Qadr\nدرودِ عالی قدر", pageIndex = 291),
            TocItem("Durood-e-Didar\nدرودِ دیدار", pageIndex = 292),
            TocItem("Durood-e-Habib\nدرودِ حبیب", pageIndex = 292),
            TocItem("Durood-e-Noor\nدرودِ نور", pageIndex = 293),
            TocItem("Aik Mukammal Durood Shareef\nایک مکمل درود شریف", pageIndex = 293),
            TocItem("Durood-e-Uloom wa Asrar\nدرودِ علوم و اسرار", pageIndex = 296),
            TocItem("Durood-e-Ruhi ya Bazurgi\nدرودِ روحی یا بزرگی", pageIndex = 297),
            TocItem("Durood-e-Tanjeena\nدرودِ تنجینا", pageIndex = 299),
            TocItem("Durood-e-Taj (Variant)\nدرودِ تاج", pageIndex = 300),
            TocItem("Durood-e-Aali\nدرودِ عالی", pageIndex = 303),
            TocItem("Asma-e-Mubarak\nاسماءِ مبارک", pageIndex = 306),
            TocItem("Huzoor ke Naam-e-Mubarak\nحضورﷺ کے نامِ مبارک", pageIndex = 307),
            TocItem("Iraad-o-Dua Nafl\nاوراد و دعا نفل", pageIndex = 309)
        )
    )
}
