package ted.parchment.reader.data.repository

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [BookRepository].
 */
class BookRepositoryTest {

    @Test
    fun `getDefaultBook returns a non-null BookConfig`() {
        val book = BookRepository.getDefaultBook()

        assertNotNull(book)
        assertTrue(book.displayName.isNotBlank())
        assertTrue(book.assetFileName.isNotBlank())
    }

    @Test
    fun `getDefaultBook has a non-empty table of contents`() {
        val book = BookRepository.getDefaultBook()

        assertTrue(book.tableOfContents.isNotEmpty())
    }

    @Test
    fun `getAllBooks returns at least one book`() {
        val books = BookRepository.getAllBooks()

        assertTrue(books.isNotEmpty())
    }

    @Test
    fun `getAllBooks contains the default book`() {
        val defaultBook = BookRepository.getDefaultBook()
        val allBooks = BookRepository.getAllBooks()

        assertTrue(allBooks.contains(defaultBook))
    }

    @Test
    fun `TOC page indices are non-negative`() {
        val book = BookRepository.getDefaultBook()

        book.tableOfContents.forEach { item ->
            assertTrue(
                "Page index for '${item.title}' should be >= 0, was ${item.pageIndex}",
                item.pageIndex >= 0
            )
        }
    }

    @Test
    fun `TOC entries have non-blank titles`() {
        val book = BookRepository.getDefaultBook()

        book.tableOfContents.forEach { item ->
            assertTrue(
                "TOC item at page ${item.pageIndex} has blank title",
                item.title.isNotBlank()
            )
        }
    }

    @Test
    fun `TOC entries are in non-decreasing page order`() {
        val book = BookRepository.getDefaultBook()
        val indices = book.tableOfContents.map { it.pageIndex }

        for (i in 1 until indices.size) {
            assertTrue(
                "TOC is not sorted: page ${indices[i - 1]} > ${indices[i]}",
                indices[i - 1] <= indices[i]
            )
        }
    }
}
