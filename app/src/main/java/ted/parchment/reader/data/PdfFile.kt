package ted.parchment.reader.data

/**
 * Data class representing a PDF File.
 * @param name Display name
 * @param fileName Asset file name
 */
data class PdfFile(
    val name: String,
    val path: String,
    val lastAccessed: Long
)
