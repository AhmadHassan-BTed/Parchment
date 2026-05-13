package ted.parchment.reader.data.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [TocItem] data class.
 */
class TocItemTest {

    @Test
    fun `TocItem stores title and pageIndex correctly`() {
        val item = TocItem(title = "Chapter 1", pageIndex = 42)

        assertEquals("Chapter 1", item.title)
        assertEquals(42, item.pageIndex)
    }

    @Test
    fun `TocItem supports bilingual titles with newlines`() {
        val item = TocItem(title = "Durood-e-Ibrahimi\nدرودِ ابراہیمی", pageIndex = 283)

        assertTrue(item.title.contains("\n"))
        assertEquals(2, item.title.split("\n").size)
    }

    @Test
    fun `TocItem equality is based on all fields`() {
        val a = TocItem("Title", 10)
        val b = TocItem("Title", 10)
        val c = TocItem("Title", 20)

        assertEquals(a, b)
        assertNotEquals(a, c)
    }

    @Test
    fun `TocItem copy modifies specified fields`() {
        val original = TocItem("Title", 10)
        val modified = original.copy(pageIndex = 50)

        assertEquals("Title", modified.title)
        assertEquals(50, modified.pageIndex)
    }
}
