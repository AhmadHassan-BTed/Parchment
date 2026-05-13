package ted.parchment.reader.ui.theme

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [DesignTokens] and [ReaderColorScheme].
 */
class DesignTokensTest {

    @Test
    fun `light mode colors are distinct from dark mode`() {
        assertNotEquals(DesignTokens.LightBackground, DesignTokens.DarkBackground)
        assertNotEquals(DesignTokens.LightText, DesignTokens.DarkText)
        assertNotEquals(DesignTokens.LightAccent, DesignTokens.DarkAccent)
    }

    @Test
    fun `ReaderColorScheme resolves light mode correctly`() {
        val scheme = ReaderColorScheme.resolve(isNightMode = false)

        assertEquals(DesignTokens.LightBackground, scheme.background)
        assertEquals(DesignTokens.LightText, scheme.text)
        assertEquals(DesignTokens.LightAccent, scheme.accent)
        assertEquals(DesignTokens.LightSurface, scheme.surface)
        assertEquals(DesignTokens.LightHud, scheme.hud)
    }

    @Test
    fun `ReaderColorScheme resolves dark mode correctly`() {
        val scheme = ReaderColorScheme.resolve(isNightMode = true)

        assertEquals(DesignTokens.DarkBackground, scheme.background)
        assertEquals(DesignTokens.DarkText, scheme.text)
        assertEquals(DesignTokens.DarkAccent, scheme.accent)
        assertEquals(DesignTokens.DarkSurface, scheme.surface)
        assertEquals(DesignTokens.DarkHud, scheme.hud)
    }

    @Test
    fun `timing constants are positive`() {
        assertTrue(DesignTokens.HUD_AUTO_HIDE_MS > 0)
        assertTrue(DesignTokens.CHAPTER_OVERLAY_MS > 0)
        assertTrue(DesignTokens.SWIPE_THRESHOLD > 0f)
    }
}
