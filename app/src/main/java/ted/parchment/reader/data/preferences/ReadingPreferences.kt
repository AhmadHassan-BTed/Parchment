package ted.parchment.reader.data.preferences

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight persistence layer for reading state using SharedPreferences.
 *
 * Stores per-book scroll positions, bookmarks, and global preferences like
 * night mode — without requiring Room or any database dependency.
 *
 * Thread-safety: All writes use [SharedPreferences.Editor.apply] (async, non-blocking).
 * Reads are synchronous but fast since SharedPreferences caches values in memory.
 */
class ReadingPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Scroll Position ─────────────────────────────────────────────────────

    /**
     * Persists the reader's scroll position for the given book.
     * @param fileName Unique identifier for the book (asset filename)
     * @param index    Zero-based item index from LazyListState
     */
    fun saveScrollPosition(fileName: String, index: Int) {
        prefs.edit().putInt("${KEY_POSITION_PREFIX}$fileName", index).apply()
    }

    /**
     * Retrieves the last saved scroll position for the given book.
     * @return Zero-based item index, defaults to 0 (beginning)
     */
    fun getScrollPosition(fileName: String): Int =
        prefs.getInt("${KEY_POSITION_PREFIX}$fileName", 0)

    // ── Night Mode ──────────────────────────────────────────────────────────

    fun saveNightMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NIGHT_MODE, enabled).apply()
    }

    fun getNightMode(): Boolean =
        prefs.getBoolean(KEY_NIGHT_MODE, false)

    // ── Bookmarks ───────────────────────────────────────────────────────────

    /**
     * Retrieves the set of bookmarked page indices for the given book.
     * Stored as a comma-separated string for simplicity.
     */
    fun getBookmarks(fileName: String): Set<Int> {
        val raw = prefs.getString("${KEY_BOOKMARK_PREFIX}$fileName", "") ?: ""
        return if (raw.isEmpty()) emptySet()
        else raw.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    }

    /**
     * Persists the complete set of bookmarked page indices for the given book.
     */
    fun saveBookmarks(fileName: String, bookmarks: Set<Int>) {
        prefs.edit()
            .putString("${KEY_BOOKMARK_PREFIX}$fileName", bookmarks.joinToString(","))
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "reading_prefs"
        private const val KEY_POSITION_PREFIX = "pos_"
        private const val KEY_BOOKMARK_PREFIX = "bm_"
        private const val KEY_NIGHT_MODE = "night_mode"
    }
}
