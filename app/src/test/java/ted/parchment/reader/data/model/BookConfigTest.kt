package ted.parchment.reader.data.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [BookConfig] data class.
 */
class BookConfigTest {

    @Test
    fun `BookConfig stores all fields correctly`() {
        val toc = listOf(TocItem("Ch 1", 0), TocItem("Ch 2", 10))
        val config = BookConfig(
            displayName = "Test Book",
            assetFileName = "test.pdf",
            tableOfContents = toc
        )

        assertEquals("Test Book", config.displayName)
        assertEquals("test.pdf", config.assetFileName)
        assertEquals(2, config.tableOfContents.size)
        assertEquals("Ch 1", config.tableOfContents[0].title)
    }

    @Test
    fun `BookConfig supports empty TOC`() {
        val config = BookConfig(
            displayName = "No TOC Book",
            assetFileName = "empty.pdf",
            tableOfContents = emptyList()
        )

        assertTrue(config.tableOfContents.isEmpty())
    }

    @Test
    fun `BookConfig equality checks all fields`() {
        val toc = listOf(TocItem("Ch 1", 0))
        val a = BookConfig("Book", "file.pdf", toc)
        val b = BookConfig("Book", "file.pdf", toc)
        val c = BookConfig("Other", "file.pdf", toc)

        assertEquals(a, b)
        assertNotEquals(a, c)
    }
}
